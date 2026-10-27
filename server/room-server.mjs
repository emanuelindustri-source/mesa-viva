import { createServer } from 'node:http';
import { WebSocketServer } from 'ws';
import { randomUUID } from 'node:crypto';

const PORT = Number(process.env.PORT ?? 9001);
const ALLOWED = (process.env.ALLOWED_ORIGINS ?? '').split(',').map((s) => s.trim()).filter(Boolean);
const MAX_ROOMS = Number(process.env.MAX_ROOMS ?? 500);
const MAX_GUESTS = 8;
const MAX_MESSAGE = 64 * 1024;

/** @type {Map<string, { host: import('ws').WebSocket, guests: Map<string, import('ws').WebSocket>, createdAt: number }>} */
const rooms = new Map();

const httpServer = createServer((req, res) => {
  if (req.method === 'GET' && (req.url === '/' || req.url === '/health')) {
    res.writeHead(200, { 'Content-Type': 'text/plain; charset=utf-8', 'Cache-Control': 'no-store' });
    res.end('Mesa Viva · relay de salas activo\n');
    return;
  }
  res.writeHead(404, { 'Content-Type': 'text/plain; charset=utf-8' });
  res.end('Mesa Viva · sala no encontrada\n');
});

const server = new WebSocketServer({
  noServer: true,
  maxPayload: MAX_MESSAGE,
  verifyClient: (info) => {
    if (ALLOWED.length === 0) return true;
    const origin = info.origin ?? '';
    return ALLOWED.includes(origin);
  },
});

httpServer.on('upgrade', (request, socket, head) => {
  server.handleUpgrade(request, socket, head, (ws) => {
    server.emit('connection', ws, request);
  });
});

function normalize(code) {
  return String(code ?? '').toUpperCase().replace(/[^A-Z0-9]/g, '').slice(0, 6);
}

function send(ws, payload) {
  if (!ws || ws.readyState !== 1) return;
  try {
    ws.send(JSON.stringify(payload));
  } catch {
    // La conexión pudo cerrarse durante el envío.
  }
}

server.on('connection', (ws) => {
  ws.isAlive = true;
  ws.rate = 0;
  ws.rateWindow = Date.now();
  ws.roomCode = null;
  ws.role = null;
  ws.clientId = null;

  ws.on('pong', () => {
    ws.isAlive = true;
  });

  ws.on('message', (data) => {
    const now = Date.now();
    if (now - ws.rateWindow > 1000) {
      ws.rateWindow = now;
      ws.rate = 0;
    }
    ws.rate += 1;
    if (ws.rate > 80) {
      send(ws, { t: 'error', key: 'sala.tooFast' });
      ws.close(1008, 'rate limit');
      return;
    }
    if (data.length > MAX_MESSAGE) {
      ws.close(1009, 'too large');
      return;
    }
    let msg;
    try {
      msg = JSON.parse(data.toString());
    } catch {
      return;
    }
    if (!msg || typeof msg.t !== 'string') return;

    if (msg.t === 'host') {
      const code = normalize(msg.code);
      if (code.length < 4) {
        send(ws, { t: 'error', key: 'sala.invalidCode' });
        return;
      }
      if (rooms.has(code)) {
        send(ws, { t: 'error', key: 'sala.codeTaken' });
        return;
      }
      if (rooms.size >= MAX_ROOMS) {
        send(ws, { t: 'error', key: 'sala.serverBusy' });
        return;
      }
      const hostId = `host-${randomUUID().slice(0, 8)}`;
      ws.roomCode = code;
      ws.role = 'host';
      ws.clientId = hostId;
      rooms.set(code, { host: ws, guests: new Map(), createdAt: Date.now() });
      send(ws, { t: 'hosted', code, hostId });
      console.log(`[room] host ${code} (${hostId})`);
      return;
    }

    if (msg.t === 'join') {
      const code = normalize(msg.code);
      const room = rooms.get(code);
      if (!room) {
        send(ws, { t: 'error', key: 'sala.invalid' });
        return;
      }
      if (room.guests.size >= MAX_GUESTS) {
        send(ws, { t: 'error', key: 'sala.full' });
        return;
      }
      const guestId = `guest-${randomUUID().slice(0, 8)}`;
      ws.roomCode = code;
      ws.role = 'guest';
      ws.clientId = guestId;
      room.guests.set(guestId, ws);
      send(ws, { t: 'joined', code, guestId });
      send(room.host, {
        t: 'guest',
        guestId,
        name: String(msg.name ?? 'Invitado').slice(0, 24),
        avatar: String(msg.avatar ?? 'tucan').slice(0, 24),
      });
      console.log(`[room] guest ${code} (${guestId})`);
      return;
    }

    if (msg.t === 'send') {
      const room = ws.roomCode ? rooms.get(ws.roomCode) : null;
      if (!room) return;
      if (ws.role === 'host') {
        const guest = room.guests.get(String(msg.to ?? ''));
        if (guest) send(guest, { t: 'message', from: ws.clientId, payload: msg.payload });
      } else {
        send(room.host, { t: 'message', from: ws.clientId, payload: msg.payload });
      }
      return;
    }

    if (msg.t === 'ping') {
      send(ws, { t: 'pong' });
    }
  });

  ws.on('close', () => {
    const code = ws.roomCode;
    if (!code) return;
    const room = rooms.get(code);
    if (!room) return;
    if (ws.role === 'host') {
      for (const guest of room.guests.values()) {
        send(guest, { t: 'closed' });
        try {
          guest.close(1000, 'host closed');
        } catch {
          // Ignorar.
        }
      }
      rooms.delete(code);
      console.log(`[room] host closed ${code}`);
    } else {
      room.guests.delete(ws.clientId);
      send(room.host, { t: 'guestLeft', guestId: ws.clientId });
      console.log(`[room] guest left ${code} (${ws.clientId})`);
    }
  });

  ws.on('error', () => {
    // El cierre se encarga de la limpieza.
  });
});

const heartbeat = setInterval(() => {
  for (const ws of server.clients) {
    if (!ws.isAlive) {
      ws.terminate();
      continue;
    }
    ws.isAlive = false;
    try {
      ws.ping();
    } catch {
      // Ignorar.
    }
  }
  for (const [code, room] of rooms) {
    if (Date.now() - room.createdAt > 12 * 60 * 60 * 1000) {
      try {
        room.host.close(1000, 'expired');
        for (const guest of room.guests.values()) guest.close(1000, 'expired');
      } catch {
        // Ignorar.
      }
      rooms.delete(code);
    }
  }
}, 30000);

httpServer.listen(PORT, () => {
  console.log(`Mesa Viva · relay de salas en http://localhost:${PORT} y WebSocket en ws://localhost:${PORT}`);
  console.log(ALLOWED.length ? `Orígenes permitidos: ${ALLOWED.join(', ')}` : 'Orígenes: todos (desarrollo)');
});

function shutdown() {
  clearInterval(heartbeat);
  for (const room of rooms.values()) {
    try {
      room.host.close(1000, 'server shutdown');
      for (const guest of room.guests.values()) guest.close(1000, 'server shutdown');
    } catch {
      // Ignorar.
    }
  }
  server.close(() => httpServer.close(() => process.exit(0)));
  setTimeout(() => process.exit(0), 3000).unref();
}

process.on('SIGTERM', shutdown);
process.on('SIGINT', shutdown);

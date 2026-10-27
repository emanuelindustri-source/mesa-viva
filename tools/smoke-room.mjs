import WebSocket from 'ws';

const url = process.env.ROOM_URL ?? 'ws://localhost:9001';
const code = `T${Date.now().toString(36).slice(-5).toUpperCase()}`;
const host = new WebSocket(url);
const guest = new WebSocket(url);
let hosted = false;
let joined = false;
let welcome = false;

const timer = setTimeout(() => {
  console.error('timeout', { hosted, joined, welcome });
  process.exit(1);
}, 8000);

function finish() {
  if (!(hosted && joined && welcome)) return;
  clearTimeout(timer);
  console.log(JSON.stringify({ ok: true, code, hosted, joined, welcome }));
  host.close();
  guest.close();
  setTimeout(() => process.exit(0), 100);
}

host.on('open', () => host.send(JSON.stringify({ t: 'host', code })));
host.on('message', (raw) => {
  const msg = JSON.parse(raw.toString());
  if (msg.t === 'hosted') {
    hosted = true;
    guest.send(JSON.stringify({ t: 'join', code, name: 'Invitado', avatar: 'tucan' }));
  }
  if (msg.t === 'guest') {
    joined = true;
    host.send(JSON.stringify({
      t: 'send',
      to: msg.guestId,
      payload: { t: 'welcome', room: { code, hostId: 'host', players: [], playlist: [], scores: {}, round: 0, chat: [] }, youId: msg.guestId },
    }));
  }
  finish();
});
guest.on('message', (raw) => {
  const msg = JSON.parse(raw.toString());
  if (msg.t === 'joined') joined = true;
  if (msg.t === 'message' && msg.payload?.t === 'welcome') welcome = true;
  finish();
});
host.on('error', (error) => {
  console.error('host error', error.message);
  process.exit(1);
});
guest.on('error', (error) => {
  console.error('guest error', error.message);
  process.exit(1);
});

import WebSocket from 'ws';
import assert from 'node:assert/strict';

const URL = 'ws://127.0.0.1:9001';
const queue = new Map();

function connect() {
  return new Promise((resolve, reject) => {
    const ws = new WebSocket(URL);
    const messages = [];
    queue.set(ws, messages);
    ws.on('message', (data) => messages.push(JSON.parse(data.toString())));
    ws.once('open', () => resolve(ws));
    ws.once('error', reject);
  });
}

async function next(ws, predicate, label) {
  const messages = queue.get(ws);
  const started = Date.now();
  while (Date.now() - started < 5000) {
    const index = messages.findIndex(predicate);
    if (index >= 0) return messages.splice(index, 1)[0];
    await new Promise((resolve) => setTimeout(resolve, 25));
  }
  throw new Error(`timeout esperando ${label}`);
}

const host = await connect();
host.send(JSON.stringify({ t: 'host', code: 'RJ01' }));
const hosted = await next(host, (m) => m.t === 'hosted', 'hosted');

const guest1 = await connect();
guest1.send(JSON.stringify({ t: 'join', code: 'RJ01', name: 'Guest', avatar: 'tucan' }));
const joined = await next(guest1, (m) => m.t === 'joined', 'joined');
await next(host, (m) => m.t === 'guest' && m.guestId === joined.guestId, 'guest');

guest1.close();
const left = await next(host, (m) => m.t === 'guestLeft' && m.guestId === joined.guestId, 'guestLeft');
assert.equal(left.permanent, false);

const guest2 = await connect();
guest2.send(JSON.stringify({ t: 'rejoin', code: 'RJ01', guestId: joined.guestId }));
const rejoined = await next(guest2, (m) => m.t === 'joined', 'rejoined');
assert.equal(rejoined.guestId, joined.guestId);
await next(host, (m) => m.t === 'guestRejoined' && m.guestId === joined.guestId, 'guestRejoined');

host.send(JSON.stringify({ t: 'send', to: joined.guestId, payload: { t: 'room', room: { code: 'RJ01' } } }));
const message = await next(guest2, (m) => m.t === 'message' && m.payload?.t === 'room', 'message');
assert.equal(message.from, hosted.hostId);

guest2.send(JSON.stringify({ t: 'leave' }));
const permanent = await next(host, (m) => m.t === 'guestLeft' && m.guestId === joined.guestId, 'permanent leave');
assert.equal(permanent.permanent, true);

host.close();
guest2.close();
console.log('rejoin OK');

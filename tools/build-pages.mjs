import { cpSync, existsSync, mkdirSync, rmSync, writeFileSync } from 'node:fs';
import { resolve } from 'node:path';

const root = resolve(import.meta.dirname, '..');
const game = resolve(root, 'dist');
const landing = resolve(root, 'landing');
const site = resolve(root, 'dist-site');

if (!existsSync(game)) {
  console.error('No existe dist/. Ejecuta primero npm run build.');
  process.exit(1);
}
if (!existsSync(landing)) {
  console.error('No existe landing/.');
  process.exit(1);
}

rmSync(site, { recursive: true, force: true });
mkdirSync(site, { recursive: true });
cpSync(landing, site, { recursive: true });
cpSync(game, resolve(site, 'play'), { recursive: true });
writeFileSync(resolve(site, '.nojekyll'), '');
console.log(`Sitio Pages construido en ${site}`);

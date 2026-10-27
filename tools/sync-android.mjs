import { cpSync, existsSync, mkdirSync, rmSync } from 'node:fs';
import { resolve } from 'node:path';

const root = resolve(import.meta.dirname, '..');
const source = resolve(root, 'dist');
const target = resolve(root, 'android', 'app', 'src', 'main', 'assets', 'www');

if (!existsSync(source)) {
  console.error('No existe dist/. Ejecuta primero npm run build.');
  process.exit(1);
}

rmSync(target, { recursive: true, force: true });
mkdirSync(target, { recursive: true });
cpSync(source, target, { recursive: true });
console.log(`Web sincronizada en ${target}`);

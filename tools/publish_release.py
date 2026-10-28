"""Publica una release de Mesa Viva y sube el APK.

Uso:
  $env:GITHUB_TOKEN = "..."
  python tools/publish_release.py --tag v0.1.0 --apk artifacts/mesa-viva-0.1.0-debug.apk
"""

from __future__ import annotations

import argparse
import hashlib
import json
import os
import sys
import urllib.error
import urllib.request
from pathlib import Path

OWNER = "emanuelindustri-source"
REPO = "mesa-viva"
API = f"https://api.github.com/repos/{OWNER}/{REPO}"


def request(method: str, url: str, token: str, data: bytes | None = None, content_type: str = "application/json"):
    headers = {
        "Authorization": f"token {token}",
        "User-Agent": "MesaVivaRelease",
        "Accept": "application/vnd.github+json",
        "Content-Type": content_type,
    }
    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    with urllib.request.urlopen(req, timeout=90) as response:
        body = response.read()
        return json.loads(body.decode("utf-8")) if body else None


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--tag", default="v0.1.0")
    parser.add_argument("--apk", default="artifacts/mesa-viva-0.1.0-debug.apk")
    args = parser.parse_args()
    token = os.environ.get("GITHUB_TOKEN", "").strip()
    if not token:
        print("Falta GITHUB_TOKEN", file=sys.stderr)
        return 2
    apk = Path(args.apk)
    if not apk.exists():
        print(f"No existe {apk}", file=sys.stderr)
        return 2
    digest = hashlib.sha256(apk.read_bytes()).hexdigest().upper()
    asset_name = f"mesa-viva-{args.tag.lstrip('v')}-debug.apk"

    release = None
    try:
        release = request("GET", f"{API}/releases/tags/{args.tag}", token)
        print("release existente", release["html_url"])
    except urllib.error.HTTPError as error:
        if error.code != 404:
            raise

    if release is None:
        body = f"""## Mesa Viva {args.tag}

La primera mesa completa: 8 juegos, salas en línea, 11 idiomas, temas de festividad y APK Android.

### Incluye
- Caída venezolana, Furia de Colores, Dominó, Dados Mentirosos, Ajedrez, Damas, Serpientes y Escaleras, Mancala.
- Bots con 3 niveles, modo pasar el teléfono y salas con código/QR, chat y rondas.
- Perfil de 10 niveles, XP, rachas y recompensas.
- Temas de Halloween criollo, Navidad, Reyes, Carnaval, San Valentín, San Patricio, Semana Santa, Cruz de Mayo, Corpus e independencia por país.
- Crónica de partida con últimos 5 movimientos, historial completo y PDF.
- Música generativa, partículas, vibración y sensores.
- Web/PWA instalable y APK Android nativo.

### APK
- `{asset_name}` · instalable por sideload (firma de depuración).
- SHA-256: `{digest}`.
- Para salas en línea: despliega el relay de `server/room-server.mjs` (ver `docs/DEPLOY.md`) y configura su URL en **Ajustes → Servidor de salas**.
- Los modos locales funcionan sin internet.

### Limitaciones conocidas
- La sala LAN sin servidor por doble QR queda para v0.2; los modos offline locales ya funcionan.
- El catálogo muestra 30 juegos; 8 son jugables en esta versión.
- Las reglas tradicionales usan una variante documentada por Mesa Viva.

Hecho con cariño para Venezuela y el mundo.
"""
        payload = {
            "tag_name": args.tag,
            "target_commitish": "main",
            "name": f"Mesa Viva {args.tag} · La primera mesa",
            "body": body,
            "draft": False,
            "prerelease": False,
            "make_latest": "true",
        }
        try:
            release = request("POST", f"{API}/releases", token, json.dumps(payload).encode("utf-8"))
            print("release creada", release["html_url"])
        except urllib.error.HTTPError as error:
            if error.code not in (409, 422):
                raise
            release = request("GET", f"{API}/releases/tags/{args.tag}", token)
            print("release recuperada", release["html_url"])

    assets = request("GET", f"{API}/releases/{release['id']}/assets", token) or []
    existing = next((a for a in assets if a.get("name") == asset_name), None)
    if existing:
        print("asset existente", existing["browser_download_url"])
        return 0

    upload_url = release["upload_url"].split("{")[0] + f"?name={asset_name}"
    data = apk.read_bytes()
    upload = request("POST", upload_url, token, data, "application/vnd.android.package-archive")
    print(json.dumps({
        "release": release["html_url"],
        "tag": release["tag_name"],
        "asset": upload["browser_download_url"],
        "size": upload["size"],
        "sha256": digest,
    }, ensure_ascii=False, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

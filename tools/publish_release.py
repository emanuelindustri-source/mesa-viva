"""Publica una release de Mesa Viva y sube sus artefactos.

Uso:
  $env:GITHUB_TOKEN = "..."
  python tools/publish_release.py `
    --tag v2.7.0 `
    --apk artifacts/mesa-viva-2.7.0-release.apk `
    --aab artifacts/mesa-viva-2.7.0-release.aab `
    --debug artifacts/mesa-viva-2.7.0-debug.apk `
    --notes artifacts/release-body-v270.md

El token necesita permiso `contents: write` sobre el repositorio. No se guarda
en el proyecto: se lee de la variable de entorno GITHUB_TOKEN.
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
    with urllib.request.urlopen(req, timeout=180) as response:
        body = response.read()
        return json.loads(body.decode("utf-8")) if body else None


def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest().upper()


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--tag", required=True)
    parser.add_argument("--apk", required=True)
    parser.add_argument("--aab", default="")
    parser.add_argument("--debug", default="")
    parser.add_argument("--notes", default="")
    parser.add_argument("--name", default="")
    args = parser.parse_args()

    token = os.environ.get("GITHUB_TOKEN", "").strip()
    if not token:
        print("Falta GITHUB_TOKEN", file=sys.stderr)
        return 2

    files = [Path(value) for value in (args.apk, args.aab, args.debug) if value]
    for path in files:
        if not path.exists():
            print(f"No existe {path}", file=sys.stderr)
            return 2

    body = ""
    if args.notes:
        notes = Path(args.notes)
        if notes.exists():
            body = notes.read_text(encoding="utf-8")
    if not body:
        body = f"## Mesa Viva {args.tag}\n\nRelease de Mesa Viva. Consulta `versions/{args.tag.lstrip('v')}.md` para la evidencia completa.\n"

    release = None
    try:
        release = request("GET", f"{API}/releases/tags/{args.tag}", token)
        print("release existente", release["html_url"])
    except urllib.error.HTTPError as error:
        if error.code != 404:
            raise

    if release is None:
        payload = {
            "tag_name": args.tag,
            "target_commitish": "main",
            "name": args.name or f"Mesa Viva {args.tag}",
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
    existing = {asset.get("name") for asset in assets}
    uploaded = []
    for path in files:
        name = path.name
        if name in existing:
            print("asset existente", name)
            continue
        upload_url = release["upload_url"].split("{")[0] + f"?name={name}"
        data = path.read_bytes()
        content_type = "application/vnd.android.package-archive" if path.suffix == ".apk" else "application/octet-stream"
        uploaded.append(request("POST", upload_url, token, data, content_type))
        print("subido", name)

    summary = {
        "release": release["html_url"],
        "tag": release["tag_name"],
        "assets": [
            {
                "name": path.name,
                "size": path.stat().st_size,
                "sha256": sha256(path),
            }
            for path in files
        ],
    }
    print(json.dumps(summary, ensure_ascii=False, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

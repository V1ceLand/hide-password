#!/usr/bin/env python3
"""Публикует Hide Password на Modrinth: создаёт проект (если его нет) и заливает
по версии на каждую сборку из build/release/.

Токен (Personal Access Token с правами CREATE_PROJECT, CREATE_VERSION, PROJECT_WRITE):
переменная MODRINTH_TOKEN или файл C:\\Users\\<user>\\tools\\modrinth\\.token (вне репозитория).

    python tools/publish_modrinth.py            # показать план
    python tools/publish_modrinth.py --publish  # создать проект/версии и отправить на модерацию
"""
from __future__ import annotations

import json
import os
import re
import sys
import urllib.error
import urllib.request
import uuid
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
API = "https://api.modrinth.com/v2"
SLUG = "hide-password-by-kozyr"
TITLE = "Hide Password (by KOZYR)"
DISCORD = "https://discord.gg/6g6VqENtvU"
GITHUB = "https://github.com/V1ceLand/hide-password"
USER_AGENT = "V1ceLand/hide-password publisher (kozyr)"

SUMMARY = "Hides your password behind asterisks in chat (/login, /register) and server login dialogs. Eye button to reveal."

BODY = f"""# Hide Password (by KOZYR)

Client-side mod that hides your password behind asterisks while you type it — in chat
(`/login`, `/register`, `/changepassword`, …) and in server login dialogs (AuthMe «Login» screen).

*Скрывает пароль звёздочками при вводе в чат (`/login`, `/register`, …) и в окнах входа на сервер.
Кнопка-глазок рядом с полем показывает пароль.*

## Features
- `/login 12345` is shown as `/login *****` — the real text is sent to the server unchanged.
- Password fields in server login dialogs are masked too (Minecraft 1.21.6+).
- Square eye button toggles visibility: next to the password field in dialogs, bottom-right in chat
  (only while a password command is typed, and it moves aside if another mod has a button there).
- Password commands are not saved to chat history (↑/↓, `command_history.txt`) or chat drafts.
- The narrator never reads the password aloud.
- Always starts hidden after a restart — nothing is saved to disk.

Recognised commands: `login`, `l`, `log`, `register`, `reg`, `changepassword`, `changepass`,
`changepw`, `cp`, `unregister`, `unreg`, `auth`, `email`.

## Requirements
- Fabric Loader. **Fabric API is not required.**
- Client only — works on any server, nothing is needed server-side.

## Help / Помощь
Bugs and questions — Discord **by KOZYR**, section 🔐 HIDE PASSWORD: {DISCORD}

Source code: {GITHUB}
"""


def props(path: Path) -> dict[str, str]:
    result = {}
    for line in path.read_text(encoding="utf-8").splitlines():
        if "=" in line and not line.lstrip().startswith("#"):
            key, value = line.split("=", 1)
            result[key.strip()] = value.strip()
    return result


def vkey(version: str):
    return [int(p) for p in version.split(".")]


def token() -> str:
    value = os.environ.get("MODRINTH_TOKEN", "").strip()
    token_file = Path.home() / "tools" / "modrinth" / ".token"
    if not value and token_file.exists():
        value = token_file.read_text(encoding="utf-8").strip()
    if not value:
        sys.exit(f"Нет токена Modrinth: задайте MODRINTH_TOKEN или положите его в {token_file}")
    return value


def request(method: str, path: str, body: bytes | None = None, content_type: str | None = None, auth: bool = True):
    headers = {"User-Agent": USER_AGENT}
    if auth:
        headers["Authorization"] = token()
    if content_type:
        headers["Content-Type"] = content_type
    req = urllib.request.Request(API + path, data=body, method=method, headers=headers)
    try:
        with urllib.request.urlopen(req) as response:
            raw = response.read()
            return json.loads(raw) if raw else None
    except urllib.error.HTTPError as error:
        if error.code == 404 and method == "GET":
            return None
        raise SystemExit(f"{method} {path} -> {error.code}: {error.read().decode(errors='replace')}")


def multipart(data: dict, files: dict[str, Path]) -> tuple[bytes, str]:
    boundary = uuid.uuid4().hex
    parts = [f'--{boundary}\r\nContent-Disposition: form-data; name="data"\r\n'
             f'Content-Type: application/json\r\n\r\n{json.dumps(data)}\r\n'.encode()]
    for name, path in files.items():
        kind = "image/png" if path.suffix == ".png" else "application/java-archive"
        parts.append(f'--{boundary}\r\nContent-Disposition: form-data; name="{name}"; filename="{path.name}"\r\n'
                     f'Content-Type: {kind}\r\n\r\n'.encode() + path.read_bytes() + b"\r\n")
    parts.append(f"--{boundary}--\r\n".encode())
    return b"".join(parts), f"multipart/form-data; boundary={boundary}"


def game_versions_for(spec: str, releases: list[str]) -> list[str]:
    """Переводит диапазон из fabric.mod.json (>=a <=b, >=a <b, ~x, x) в список версий Modrinth."""
    spec = spec.strip()
    if spec.startswith("~"):
        base = spec[1:]
        return [v for v in releases if v == base or v.startswith(base + ".")]
    low = re.search(r">=\s*([\d.]+)", spec)
    high = re.search(r"<=\s*([\d.]+)", spec)
    below = re.search(r"(?<![<>=])<\s*([\d.]+)", spec)
    if not low:
        return [spec] if spec in releases else []
    result = []
    for v in releases:
        if vkey(v) < vkey(low.group(1)):
            continue
        if high and vkey(v) > vkey(high.group(1)):
            continue
        if below and vkey(v) >= vkey(below.group(1)):
            continue
        result.append(v)
    return result


def changelog(version: str) -> str:
    text = (ROOT / "CHANGELOG.md").read_text(encoding="utf-8")
    match = re.search(rf"^## {re.escape(version)}\n(.*?)(?=^## |\Z)", text, re.S | re.M)
    return match.group(1).strip() if match else ""


def main() -> None:
    publish = "--publish" in sys.argv
    mod_version = props(ROOT / "gradle.properties")["mod_version"]
    releases = [g["version"] for g in request("GET", "/tag/game_version", auth=False) if g["version_type"] == "release"]

    plan = []
    for module in sorted((p for p in (ROOT / "versions").iterdir() if p.is_dir()), key=lambda p: vkey(p.name)):
        mc = props(module / "gradle.properties")
        jar = ROOT / "build/release" / f"hide-password-by-kozyr-{mod_version}+mc{mc['minecraft_version']}.jar"
        versions = sorted(game_versions_for(mc["minecraft_range"], releases), key=vkey)
        plan.append((mc["minecraft_version"], jar, versions))
        print(f"{mod_version}+mc{mc['minecraft_version']:8} {'OK ' if jar.exists() else 'NO JAR'} {', '.join(versions)}")
    if not publish:
        return

    project = request("GET", f"/project/{SLUG}")
    if project is None:
        data = {
            "slug": SLUG, "title": TITLE, "description": SUMMARY, "body": BODY,
            "categories": ["utility"], "additional_categories": ["social"],
            "client_side": "required", "server_side": "unsupported",
            "project_type": "mod", "license_id": "MIT",
            "source_url": GITHUB, "issues_url": GITHUB + "/issues", "discord_url": DISCORD,
            "initial_versions": [], "is_draft": True,
        }
        body, ctype = multipart(data, {"icon": ROOT / "common/src/main/resources/assets/hidepassword/icon.png"})
        project = request("POST", "/project", body, ctype)
        # При создании стороны не сохраняются (остаются unknown) — выставляем отдельным запросом.
        request("PATCH", f"/project/{project['id']}",
                json.dumps({"client_side": "required", "server_side": "unsupported"}).encode(), "application/json")
        print("проект создан:", project["id"])
    existing = {v["version_number"] for v in request("GET", f"/project/{project['id']}/version") or []}

    for mc_version, jar, versions in plan:
        number = f"{mod_version}+mc{mc_version}"
        if number in existing:
            print("уже есть:", number)
            continue
        data = {
            "project_id": project["id"], "name": f"{mod_version} for Minecraft {versions[0]}–{versions[-1]}"
            if len(versions) > 1 else f"{mod_version} for Minecraft {versions[0]}",
            "version_number": number, "changelog": changelog(mod_version), "dependencies": [],
            "game_versions": versions, "version_type": "release", "loaders": ["fabric"],
            "featured": mc_version == plan[-1][0], "file_parts": ["file"], "primary_file": "file",
        }
        body, ctype = multipart(data, {"file": jar})
        request("POST", "/version", body, ctype)
        print("версия:", number)

    if project.get("status") in (None, "draft"):
        request("PATCH", f"/project/{project['id']}", json.dumps({"status": "processing"}).encode(), "application/json")
        print("отправлен на модерацию")
    print(f"https://modrinth.com/mod/{SLUG}")


if __name__ == "__main__":
    main()

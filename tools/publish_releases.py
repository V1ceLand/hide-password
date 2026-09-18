#!/usr/bin/env python3
"""Публикует GitHub-релизы Hide Password: по одному на каждую версию Minecraft.

Берёт jar из build/release/, диапазон версий — из versions/<mc>/gradle.properties.
Тег: v<mod>+mc<mc>. Уже существующие релизы пропускает.

    python tools/publish_releases.py            # показать план
    python tools/publish_releases.py --publish  # создать релизы (нужен gh)
"""
from __future__ import annotations

import re
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
REPO = "V1ceLand/hide-password"
DISCORD = "https://discord.gg/6g6VqENtvU"


def props(path: Path) -> dict[str, str]:
    result = {}
    for line in path.read_text(encoding="utf-8").splitlines():
        if "=" in line and not line.lstrip().startswith("#"):
            key, value = line.split("=", 1)
            result[key.strip()] = value.strip()
    return result


def human_range(spec: str) -> str:
    """'>=1.20 <=1.20.1' -> '1.20–1.20.1', '~26.2' -> '26.2.x', '>=26.1 <26.2' -> '26.1.x'."""
    spec = spec.strip()
    if spec.startswith("~"):
        return spec[1:] + ".x" if spec.count(".") == 1 else spec[1:]
    low = re.search(r">=\s*([\d.]+)", spec)
    high = re.search(r"<=\s*([\d.]+)", spec)
    below = re.search(r"<\s*([\d.]+)", spec)
    if low and high:
        return f"{low.group(1)}–{high.group(1)}"
    if low and below:
        return f"{low.group(1)}.x"
    return spec


def version_key(mc: str):
    return [int(part) for part in mc.split(".")]


def changelog_section(version: str) -> str:
    text = (ROOT / "CHANGELOG.md").read_text(encoding="utf-8")
    match = re.search(rf"^## {re.escape(version)}\n(.*?)(?=^## |\Z)", text, re.S | re.M)
    return match.group(1).strip() if match else ""


def main() -> None:
    mod_version = props(ROOT / "gradle.properties")["mod_version"]
    notes = changelog_section(mod_version)
    existing = subprocess.run(["gh", "release", "list", "-R", REPO, "--limit", "200", "--json", "tagName",
                               "--jq", ".[].tagName"], capture_output=True, text=True).stdout.split()

    modules = sorted((p for p in (ROOT / "versions").iterdir() if p.is_dir()), key=lambda p: version_key(p.name))
    for index, module in enumerate(modules):
        mc = props(module / "gradle.properties")
        jar = ROOT / "build/release" / f"hide-password-by-kozyr-{mod_version}+mc{mc['minecraft_version']}.jar"
        tag = f"v{mod_version}+mc{mc['minecraft_version']}"
        supported = human_range(mc["minecraft_range"])
        title = f"Hide Password (by KOZYR) {mod_version} — Minecraft {supported}"
        dialogs = version_key(mc["minecraft_version"]) >= [1, 21, 6]
        body = f"""**Minecraft {supported}** · Fabric Loader ≥ {mc['loader_min']} · Java {mc['java_version']}+ · client only, Fabric API not required

Download `{jar.name}` below and put it into `mods/`. Remove the old `password-mask-*.jar` if you had it.
{'' if dialogs else chr(10) + '> Server login dialogs appeared in Minecraft 1.21.6, so on this version the mod masks chat commands (`/login`, `/register`, …).' + chr(10)}
### Changes
{notes}

### Help / Помощь
Bugs and questions — Discord **by KOZYR**, section 🔐 HIDE PASSWORD: {DISCORD}
Баги и вопросы — Discord «by KOZYR», раздел 🔐 HIDE PASSWORD: {DISCORD}
"""
        latest = index == len(modules) - 1
        print(f"{tag:28} {supported:14} {'LATEST ' if latest else ''}{'exists' if tag in existing else ''}")
        if "--publish" in sys.argv and tag not in existing:
            if not jar.exists():
                sys.exit(f"нет {jar}")
            subprocess.run(["gh", "release", "create", tag, str(jar), "-R", REPO, "--target", "main",
                            "--title", title, "--notes", body, f"--latest={'true' if latest else 'false'}"],
                           check=True)


if __name__ == "__main__":
    main()

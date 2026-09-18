#!/usr/bin/env python3
"""Генератор иконок мода Password Mask.

Рисует пиксель-арт без сглаживания (так он остаётся чётким при любом
масштабе GUI Minecraft) и кладёт результат прямо в ресурсы мода:

    assets/passwordmask/textures/gui/sprites/icon/eye.png
    assets/passwordmask/textures/gui/sprites/icon/eye_slash.png
    assets/passwordmask/icon.png

Запуск:  python tools/generate_icons.py
"""

from __future__ import annotations

import math
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
SPRITES = ROOT / "src/client/resources/assets/passwordmask/textures/gui/sprites/icon"
MOD_ICON = ROOT / "src/client/resources/assets/passwordmask/icon.png"

SIZE = 16
WHITE = (255, 255, 255, 255)
PUPIL = (30, 30, 34, 255)
CLEAR = (0, 0, 0, 0)

# Глаз — «линза» (пересечение двух кругов) с круглым зрачком.
LENS_RADIUS = 7.7
LENS_OFFSET = 3.2
PUPIL_RADIUS = 2.4
CENTER = SIZE / 2


def _dist(x: float, y: float, cx: float, cy: float) -> float:
    return math.hypot(x - cx, y - cy)


def _in_lens(x: float, y: float) -> bool:
    return (_dist(x, y, CENTER, CENTER + LENS_OFFSET) <= LENS_RADIUS
            and _dist(x, y, CENTER, CENTER - LENS_OFFSET) <= LENS_RADIUS)


def _dist_to_diagonal(x: float, y: float) -> float:
    """Расстояние до диагонали из левого верхнего в правый нижний угол."""
    return abs(x - y) / math.sqrt(2.0)


def draw_eye(slashed: bool) -> Image.Image:
    image = Image.new("RGBA", (SIZE, SIZE), CLEAR)
    pixels = image.load()

    for py in range(SIZE):
        for px in range(SIZE):
            x, y = px + 0.5, py + 0.5
            if not _in_lens(x, y):
                continue
            if _dist(x, y, CENTER, CENTER) <= PUPIL_RADIUS:
                pixels[px, py] = PUPIL
            else:
                pixels[px, py] = WHITE

    # Блик в зрачке.
    pixels[7, 6] = WHITE

    if slashed:
        for py in range(SIZE):
            for px in range(SIZE):
                x, y = px + 0.5, py + 0.5
                gap = _dist_to_diagonal(x, y)
                if not (2.0 <= x <= 14.0 and 2.0 <= y <= 14.0):
                    continue
                if gap <= 2.1:
                    pixels[px, py] = CLEAR
                if gap <= 1.0:
                    pixels[px, py] = WHITE

    return image


def draw_mod_icon(size: int = 128) -> Image.Image:
    """Иконка мода: тот же глаз крупно на тёмном фоне плюс «пароль» звёздочками."""
    icon = Image.new("RGBA", (size, size), (26, 29, 34, 255))
    pixels = icon.load()

    scale = size / SIZE
    eye = draw_eye(slashed=False).resize((int(13 * scale), int(13 * scale)), Image.NEAREST)
    icon.alpha_composite(eye, ((size - eye.width) // 2, int(size * 0.18)))

    # Ряд звёздочек-«пароля» под глазом.
    star = max(2, size // 22)
    gap = star * 2
    total = star * 5 + gap * 4
    left = (size - total) // 2
    top = int(size * 0.72)
    for index in range(5):
        x0 = left + index * (star + gap)
        for dy in range(star):
            for dx in range(star):
                pixels[x0 + dx, top + dy] = (255, 255, 255, 255)

    # Рамка.
    for i in range(size):
        for j in (0, 1, size - 2, size - 1):
            pixels[i, j] = (52, 58, 66, 255)
            pixels[j, i] = (52, 58, 66, 255)

    return icon


def main() -> None:
    SPRITES.mkdir(parents=True, exist_ok=True)
    draw_eye(slashed=False).save(SPRITES / "eye.png")
    draw_eye(slashed=True).save(SPRITES / "eye_slash.png")
    MOD_ICON.parent.mkdir(parents=True, exist_ok=True)
    draw_mod_icon().save(MOD_ICON)
    print("готово:", SPRITES / "eye.png", SPRITES / "eye_slash.png", MOD_ICON, sep="\n  ")


if __name__ == "__main__":
    main()

from __future__ import annotations

from pathlib import Path
from PIL import Image, ImageDraw, ImageFilter

OUT = Path("public/icons")
OUT.mkdir(parents=True, exist_ok=True)
SIZE = 1024


def gradient(size: int, top: tuple[int, int, int], bottom: tuple[int, int, int]) -> Image.Image:
    img = Image.new("RGB", (size, size), top)
    px = img.load()
    for y in range(size):
        t = y / max(1, size - 1)
        r = int(top[0] + (bottom[0] - top[0]) * t)
        g = int(top[1] + (bottom[1] - top[1]) * t)
        b = int(top[2] + (bottom[2] - top[2]) * t)
        for x in range(size):
            px[x, y] = (r, g, b)
    return img


def card_layer(width: int, height: int, fill, border, radius: int = 38) -> Image.Image:
    layer = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    d.rounded_rectangle((2, 2, width - 3, height - 3), radius=radius, fill=fill, outline=border, width=8)
    d.rounded_rectangle((18, 18, width - 19, height - 19), radius=radius - 12, outline=(176, 122, 34, 70), width=4)
    return layer


def sun(size: int, color, ray_color, center_color) -> Image.Image:
    layer = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    c = size // 2
    for i in range(8):
        angle = i * 45
        import math
        a = math.radians(angle)
        x1 = c + math.cos(a) * size * 0.28
        y1 = c + math.sin(a) * size * 0.28
        x2 = c + math.cos(a) * size * 0.45
        y2 = c + math.sin(a) * size * 0.45
        d.line((x1, y1, x2, y2), fill=ray_color, width=max(4, size // 34))
    d.ellipse((c - size * 0.27, c - size * 0.27, c + size * 0.27, c + size * 0.27), fill=color)
    d.ellipse((c - size * 0.13, c - size * 0.13, c + size * 0.13, c + size * 0.13), fill=center_color)
    return layer


def compose(maskable: bool = False) -> Image.Image:
    bg = gradient(SIZE, (10, 40, 33), (4, 18, 14)).convert("RGBA")
    # halo
    glow = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    gd = ImageDraw.Draw(glow)
    gd.ellipse((120, 80, 920, 880), fill=(224, 166, 58, 26))
    glow = glow.filter(ImageFilter.GaussianBlur(70))
    bg.alpha_composite(glow)

    # mesa / mesa edge
    d = ImageDraw.Draw(bg)
    d.ellipse((100, 250, 900, 900), outline=(231, 198, 106, 30), width=4)

    # cartas
    red = card_layer(330, 460, (124, 31, 43, 255), (231, 198, 106, 180))
    red = red.rotate(13, expand=True, resample=Image.Resampling.BICUBIC)
    cream = card_layer(330, 460, (247, 236, 215, 255), (150, 110, 45, 160))
    cream = cream.rotate(-11, expand=True, resample=Image.Resampling.BICUBIC)
    if maskable:
        bg.alpha_composite(red, (430, 330))
        bg.alpha_composite(cream, (300, 300))
    else:
        bg.alpha_composite(red, (440, 310))
        bg.alpha_composite(cream, (300, 280))

    # pips on cream card
    pip_layer = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    pd = ImageDraw.Draw(pip_layer)
    cx, cy = 455, 470
    for ox, oy in ((-58, -80), (58, -80), (-58, 0), (58, 0), (-58, 80), (58, 80)):
        pd.ellipse((cx + ox - 16, cy + oy - 16, cx + ox + 16, cy + oy + 16), fill=(76, 110, 78, 255))
    bg.alpha_composite(pip_layer)

    # sol
    s = sun(340, (224, 166, 58, 255), (240, 198, 106, 255), (124, 31, 43, 255))
    bg.alpha_composite(s, (620, 80))

    # destello
    d = ImageDraw.Draw(bg)
    d.ellipse((760, 140, 800, 180), fill=(255, 244, 214, 230))
    return bg


def main() -> None:
    base = compose(False)
    maskable = compose(True)
    for size in (192, 512, 1024):
        base.resize((size, size), Image.Resampling.LANCZOS).save(OUT / f"icon-{size}.png", optimize=True)
        maskable.resize((size, size), Image.Resampling.LANCZOS).save(OUT / f"maskable-{size}.png", optimize=True)
    base.resize((180, 180), Image.Resampling.LANCZOS).save(OUT / "apple-touch-icon.png", optimize=True)
    base.resize((32, 32), Image.Resampling.LANCZOS).save(OUT / "favicon-32.png", optimize=True)
    print("icons written to", OUT)


if __name__ == "__main__":
    main()

#!/usr/bin/env python3
"""
Make black (or near-black) background transparent in icon_set4.png.
Requires: pip install Pillow
Usage: python make_needle_bg_transparent.py
"""
from pathlib import Path

try:
    from PIL import Image
except ImportError:
    print("Install Pillow first: pip install Pillow")
    raise

# Path to the image (same folder as script, or adjust)
SCRIPT_DIR = Path(__file__).resolve().parent
IMG_PATH = SCRIPT_DIR / "app" / "src" / "main" / "res" / "drawable-nodpi" / "icon_set4.png"
OUT_PATH = IMG_PATH  # overwrite; or set to IMG_PATH.with_name("icon_set4_transparent.png")

def main():
    if not IMG_PATH.exists():
        print(f"Not found: {IMG_PATH}")
        return
    img = Image.open(IMG_PATH).convert("RGBA")
    data = img.getdata()
    # Threshold: pixels with R,G,B all <= this become transparent (0 = strict black)
    threshold = 30  # allow slight variation from pure black
    new_data = []
    for item in data:
        r, g, b, a = item
        if r <= threshold and g <= threshold and b <= threshold:
            new_data.append((r, g, b, 0))  # transparent
        else:
            new_data.append(item)
    img.putdata(new_data)
    img.save(OUT_PATH, "PNG")
    print(f"Saved: {OUT_PATH} (black pixels set to transparent)")

if __name__ == "__main__":
    main()

# -*- coding: utf-8 -*-
"""把 vitd/ 目录下的 6 张图按生成时间顺序映射到 m25-m30，裁水印转 WebP"""
import json, os, glob, sys
from PIL import Image

BASE = os.path.dirname(os.path.abspath(__file__))
RAW = os.path.abspath(os.path.join(BASE, "..", "..", "generated-images", "vitd"))
OUT = os.path.abspath(os.path.join(BASE, "..", "app", "src", "main", "assets", "dishes"))

prompts = json.load(open(os.path.join(BASE, "image_prompts_vitd.json"), encoding="utf-8"))
files = sorted(glob.glob(os.path.join(RAW, "*.png")), key=lambda p: os.path.basename(p).split("T")[-1])
print("prompts", len(prompts), "files", len(files))
assert len(prompts) == len(files), "数量不一致，需人工核对"

for p, src in zip(prompts, files):
    im = Image.open(src).convert("RGB")
    w, h = im.size
    im = im.crop((0, 0, int(w * 0.995), int(h * 0.865)))
    tw, th = 720, 630
    iw, ih = im.size
    scale = max(tw / iw, th / ih)
    im = im.resize((int(iw * scale + 0.5), int(ih * scale + 0.5)), Image.LANCZOS)
    iw, ih = im.size
    left = (iw - tw) // 2
    top = int((ih - th) * 0.42)
    im = im.crop((left, top, left + tw, top + th))
    dst = os.path.join(OUT, p["id"] + ".webp")
    im.save(dst, "WEBP", quality=82, method=6)
    print("%-5s %-14s <- %s  (%d bytes)" % (p["id"], p["name"],
          os.path.basename(src)[:44], os.path.getsize(dst)))

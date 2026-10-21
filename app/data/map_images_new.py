# -*- coding: utf-8 -*-
"""把 new/ 目录下 34 张新图映射到菜品 id，裁水印 + 转 WebP"""
import json, os, glob
from PIL import Image

BASE = os.path.dirname(os.path.abspath(__file__))
RAW = os.path.abspath(os.path.join(BASE, "..", "..", "generated-images", "new"))
OUT = os.path.abspath(os.path.join(BASE, "..", "app", "src", "main", "assets", "dishes"))

prompts = json.load(open(os.path.join(BASE, "image_prompts_new.json"), encoding="utf-8"))
files = sorted(glob.glob(os.path.join(RAW, "*.png")))
print("prompts", len(prompts), "files", len(files))

STOP = set("with and the in on a an of from to into small tiny few 100g".split())

used, assign = set(), {}
for p in prompts:
    head = p["prompt"].split(". Top-down")[0]
    words = [w for w in head.lower().replace(",", " ").split()
             if len(w) > 2 and w not in STOP]
    best, bestscore = None, -1
    for f in files:
        if f in used:
            continue
        nm = os.path.basename(f).lower()
        score = sum(4 for w in words[:6] if w in nm)
        # 前置词（第一段描述的主语）额外加权，避免 "silky steamed egg custard" 撞车
        if words and words[0] in nm:
            score += 6
        if score > bestscore:
            best, bestscore = f, score
    if bestscore <= 0:
        print("!! LOW MATCH", p["id"], p["name"], bestscore)
    used.add(best)
    assign[p["id"]] = (best, bestscore)

print("assigned", len(assign), "unique", len(set(v[0] for v in assign.values())))
print("\n%-6s %-16s %-5s %s" % ("id", "name", "score", "file"))
for p in prompts:
    f, s = assign[p["id"]]
    print("%-6s %-16s %-5d %s" % (p["id"], p["name"], s, os.path.basename(f)))

unused = [f for f in files if f not in used]
if unused:
    print("\nUNUSED:")
    for f in unused:
        print("  ", os.path.basename(f))

manifest_path = os.path.join(BASE, "image_manifest_new.json")
if "--write" not in os.sys.argv:
    print("\n(dry run, add --write to output webp)")
    os.sys.exit(0)

manifest = {}
for p in prompts:
    did = p["id"]
    src = assign[did][0]
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
    im.save(os.path.join(OUT, did + ".webp"), "WEBP", quality=82, method=6)
    manifest[did] = os.path.basename(src)

json.dump(manifest, open(manifest_path, "w", encoding="utf-8"), ensure_ascii=False, indent=1)
print("\nwritten", len(manifest), "webp ->", OUT)

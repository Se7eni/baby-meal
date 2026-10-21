# -*- coding: utf-8 -*-
"""按图片内容与提示词描述的匹配度，把 raw 图精确映射到菜品 id"""
import json, os, glob
from PIL import Image

BASE = os.path.dirname(os.path.abspath(__file__))
RAW = os.path.abspath(os.path.join(BASE, "..", "..", "images_raw"))
OUT = os.path.abspath(os.path.join(BASE, "..", "app", "src", "main", "assets", "dishes"))
os.makedirs(OUT, exist_ok=True)
for f in glob.glob(os.path.join(OUT, "*.webp")):
    os.remove(f)

prompts = json.load(open(os.path.join(BASE, "image_prompts.json"), encoding="utf-8"))
files = glob.glob(os.path.join(RAW, "*.png"))
files.sort()


def nkey(p):
    return p.split(".")[0].replace("-", "_")


def ts(f):
    b = os.path.basename(f)
    return b[b.rfind("T") + 1:]


# 文件名不具区分度、按生成时间顺序才对得上的图，直接指定
FIXED_BY_TS = {
    "p08": "clear_broth_with_white_fish_sl",
    "p03": "clear_broth_with_egg_ribbons___",
    "v10": "stir_fried_hand_torn_cabbage_w",
    "s13": "three_small_round_reddish_oran",
    "k06": "three_small_round_golden_mille",
    "k03": "three_small_round_purple_sweet",
}

used, assign = set(), {}
for p in prompts:
    if p["id"] in FIXED_BY_TS:
        pref = FIXED_BY_TS[p["id"]]
        for f in files:
            if os.path.basename(f).startswith(pref) and f not in used:
                used.add(f)
                assign[p["id"]] = f
                break
        continue
    # 直属提示词名（去掉最后的样式后缀）取前 N 个词做特征
    head = p["prompt"].split(". Top-down")[0]
    words = [w for w in head.lower().replace(",", " ").split() if len(w) > 2]
    best, bestscore = None, -1
    for f in files:
        if f in used:
            continue
        nm = os.path.basename(f).lower()
        score = sum(4 for w in words[:5] if w in nm)
        if score > bestscore:
            best, bestscore = f, score
    if bestscore <= 0:
        print("LOW MATCH", p["id"], p["name"], bestscore)
    used.add(best)
    assign[p["id"]] = best

# 校验：每张图只能被用一次，且数量对
print("assigned", len(assign), "unique", len(set(assign.values())), "raw", len(files))
for did in sorted(assign):
    print(did, os.path.basename(assign[did])[:48])

manifest = {}
for did, src in assign.items():
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
    manifest[did] = {"src": os.path.basename(src), "ts": ts(src)}

# 检查排序是否与当时生成的顺序一致（用于人工核对）
ordered = sorted(assign.items(), key=lambda kv: manifest[kv[0]]["ts"])
print("\n--- by time ---")
for did, src in ordered:
    print(manifest[did]["ts"], did)

json.dump(manifest, open(os.path.join(BASE, "image_manifest.json"), "w", encoding="utf-8"), ensure_ascii=False, indent=1)

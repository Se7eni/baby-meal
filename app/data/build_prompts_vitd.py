# -*- coding: utf-8 -*-
"""生成 6 道高维D 主菜的图片提示词（沿用统一 STYLE 后缀）"""
import json, os, sys
sys.stdout.reconfigure(encoding='utf-8')

BASE = os.path.dirname(os.path.abspath(__file__))
STYLE = (". Top-down overhead food photo on a small white ceramic bowl, one wooden baby spoon beside it, "
         "soft natural window daylight from upper left, light cream linen tablecloth, warm soft pastel palette, "
         "shallow depth of field, clean minimal styling, appetizing, high-end editorial food photography. "
         "No text, no words, no watermark, no people.")

ITEMS = [
    ("m25", "三文鱼豆腐煲",
     "pale orange salmon cubes and soft white tofu cubes simmered in a light golden broth, small orange carrot dice and tiny green broccoli florets, glossy thin sauce"),
    ("m26", "三文鱼土豆泥饼",
     "four small round golden pan-fried potato cakes with pink salmon flecks visible, soft fluffy texture, stacked on a white plate"),
    ("m27", "鲈鱼蒸蛋羹",
     "silky smooth pale yellow steamed egg custard with white fish flakes embedded, mirror-like glossy surface, a few green scallion dots"),
    ("m28", "银鱼蒸肉饼",
     "round steamed pork patty with a bright golden egg yolk nestled in its center, tiny silver white fish pieces on top, glossy savory surface"),
    ("m29", "鳕鱼香菇蒸肉饼",
     "round steamed pork and white fish patty on a white plate, tiny dark brown mushroom bits, moist and tender, sliced into wedges"),
    ("m30", "香煎三文鱼豆腐丸",
     "six small round golden pan-fried fish and tofu balls, light golden crust, one cut open showing soft pale interior with green vegetable flecks"),
]

out = [{"id": i, "name": n, "prompt": p + STYLE} for i, n, p in ITEMS]
P = os.path.join(BASE, "image_prompts_vitd.json")
json.dump(out, open(P, "w", encoding="utf-8"), ensure_ascii=False, indent=1)
print("wrote", len(out), "->", P)
for o in out:
    print(o["id"], o["name"])

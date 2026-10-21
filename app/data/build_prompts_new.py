# -*- coding: utf-8 -*-
"""生成 34 道新菜的英文图片提示词（与已有 68 张同一套 STYLE）"""
import json
import os
import sys

sys.stdout.reconfigure(encoding="utf-8")

BASE = os.path.dirname(os.path.abspath(__file__))
ASSETS = os.path.abspath(os.path.join(BASE, "..", "app", "src", "main", "assets"))

STYLE = ("Top-down overhead food photo on a small white ceramic bowl, one wooden baby spoon beside it, "
         "soft natural window daylight from upper left, light cream linen tablecloth, warm soft pastel palette, "
         "shallow depth of field, clean minimal styling, appetizing, high-end editorial food photography. "
         "No text, no words, no watermark, no people.")

VIS = {
    "s16": "short baby noodles in golden broth with thin slivers of dark green kelp, tiny dried shrimp and soft yellow egg ribbons",
    "v16": "shredded dark green kelp salad in a small white dish, sprinkled with toasted white sesame seeds, a few sesame scattered beside",
    "p09": "clear light soup with floating dark purple nori pieces, tiny dried shrimp and soft white tofu cubes",
    "m16": "silky steamed egg custard in a shallow white bowl with small pieces of oyster embedded, glossy smooth surface",
    "m17": "creamy mashed potato with golden brown minced beef and tiny diced carrot, soft texture, one spoonful lifted",
    "k16": "four small square golden pan-fried cakes cut into bite-size, melted cheese strings visible inside",
    "m18": "small steamed savory cake on a white plate, pale orange from salmon, cut into neat small cubes",
    "p10": "creamy pale mushroom soup in a white bowl, soft golden egg yolk swirls on the surface, tiny mushroom slices",
    "s17": "light brown brown-rice in a white bowl with diced white chicken, tiny orange carrot cubes and bright green peas",
    "m19": "smooth dark reddish-brown liver and spinach puree in a small white bowl, glossy and fine texture",
    "k17": "three small round golden oat and banana steamed buns in paper cups, one broken open showing soft crumb",
    "m20": "steamed white cod fillet resting on soft white tofu slices, fine green scallion threads on top, very tender",
    "p11": "creamy pale orange salmon and vegetable soup in a white bowl, small visible salmon flakes and green broccoli florets",
    "m21": "chunks of tender braised beef brisket in thick bright red tomato sauce, soft onion pieces, glossy",
    "v17": "thick slices of steamed deep orange pumpkin topped with small diced red dates, glistening soft surface",
    "v18": "bright green blanched spinach in a small white dish, drizzled with creamy light brown sesame paste",
    "k18": "clear glass cup with layered white yogurt, soft oats and fresh blueberries, a few banana slices on top",
    "v19": "tender green asparagus tips stir-fried with soft yellow scrambled egg pieces",
    "v20": "small cubes of pale chicken stir-fried with bright red and yellow bell pepper dice, glossy",
    "s18": "golden orange pumpkin and beige chickpea mashed rice in a white bowl, soft and creamy, tiny chickpeas visible",
    "k19": "three toast sticks spread with creamy pale green avocado and banana mash on a small white plate",
    "p12": "clear soup in a white bowl with small round pale meatballs and translucent white winter melon slices",
    "v21": "long soft steamed white yam and orange carrot sticks arranged neatly on a white plate, finger food",
    "m22": "silky steamed egg custard with pink shrimp pieces and white tofu cubes, glossy smooth top",
    "s19": "small rhombus shaped handmade noodle pieces in bright red-orange tomato broth with yellow egg ribbons and green vegetables",
    "k20": "four small round golden pan-fried cakes with visible tiny green broccoli florets and melted cheese",
    "v22": "soft steamed purple eggplant strips in a white dish, drizzled with light golden sesame oil and garlic",
    "p13": "light clear pork rib soup with pale lotus root chunks, faint golden broth, very clean look",
    "s20": "white rice porridge with tiny silver white fish and chopped bright green bok choy",
    "m23": "dark red duck blood cubes and soft white tofu cubes in a light glossy sauce, garnished with tiny green cilantro",
    "k21": "purple sweet potato milk cubes cut into neat squares on a white plate, milky purple color, soft pudding texture",
    "v23": "bright red-orange tomato and soft white tofu pieces in thick glossy sauce, creamy texture",
    "s21": "golden yellow corn cream noodle soup in a white bowl with soft noodles and small chicken pieces",
    "m24": "milky white fish soup with soft white tofu chunks, a few green scallion pieces, gentle steam",
}

data = json.load(open(os.path.join(ASSETS, "dishes.json"), encoding="utf-8"))
ids = [x["id"] for x in data if x["id"] in VIS]
missing = [k for k in VIS if k not in {x["id"] for x in data}]
if missing:
    print("WARN 数据里没有:", missing)

out = []
for x in data:
    if x["id"] in VIS:
        out.append({"id": x["id"], "name": x["name"],
                    "prompt": VIS[x["id"]] + ". " + STYLE})

json.dump(out, open(os.path.join(BASE, "image_prompts_new.json"), "w", encoding="utf-8"),
          ensure_ascii=False, indent=1)
print("生成提示词", len(out), "条")
for o in out:
    print(o["id"], o["name"])

# -*- coding: utf-8 -*-
"""生成 68 道菜的图片提示词清单"""
import json, os

D = json.load(open(os.path.join(os.path.dirname(os.path.abspath(__file__)), "dishes.json"), encoding="utf-8"))

STYLE = ("Top-down overhead food photo on a small white ceramic bowl, one wooden baby spoon beside it, "
         "soft natural window daylight from upper left, light cream linen tablecloth, warm soft pastel palette, "
         "shallow depth of field, clean minimal styling, appetizing, high-end editorial food photography. "
         "No text, no words, no watermark, no people.")

# 每道菜的视觉描述
VIS = {
"s01":"creamy golden millet porridge with soft diced pumpkin pieces, garnished with one tiny green leaf",
"s02":"soft white rice mixed with pink shrimp cubes, small bright green broccoli florets and diced carrot",
"s03":"short baby noodles in a light broth with flaked orange-pink salmon and chopped green spinach",
"s04":"short baby pasta coated in rich red tomato meat sauce with visible minced beef and diced onion",
"s05":"fluffy rice with golden-brown diced chicken thigh, chopped shiitake mushroom and diced carrot, slightly glossy",
"s06":"two-layer purple sweet potato and white yam cake cut into neat small squares showing purple and cream layers",
"s07":"two small round golden pan-fried fish and potato patties with tiny carrot shreds, one broken open",
"s08":"thick rice porridge with fine pork mince and chopped green bok choy",
"s09":"rolled Japanese-style omelette cut into thick slices, yellow egg layers with melted cheese",
"s10":"golden corn and chicken thick soup poured over soft white rice, silky egg ribbons",
"s11":"tiny wontons in clear broth, a few small pieces of nori seaweed floating",
"s12":"moist rice stewed with golden pumpkin chunks and diced chicken, thick glossy sauce",
"s13":"three small round reddish-orange sweet potato oat pancakes, slightly golden edges",
"s14":"tomato egg drop soup with small soft flour dumplings, bright red-orange broth, yellow egg ribbons",
"s15":"soft noodles with thin pork strips, sliced shiitake mushrooms and green bok choy in light broth",
"m01":"a whole steamed white cod fillet on a small plate with fine ginger slivers and scallion, very tender",
"m02":"five small round white chicken meatballs in clear light broth with green bok choy",
"m03":"chunks of tender braised beef with bright red tomato and soft potato cubes in a rich red sauce",
"m04":"pink shrimp cubes stir-fried with small bright green broccoli florets",
"m05":"one golden pan-seared salmon cube with crispy skin, lemon slice, small broccoli on the side",
"m06":"sliced pork liver stir-fried with bright green spinach",
"m07":"braised chicken thigh pieces with golden potato and carrot chunks in light brown sauce",
"m08":"steamed pork patty with a whole egg on top, glossy, in a shallow bowl",
"m09":"pink shrimp paste balls with soft white tofu cubes and green vegetable in light broth",
"m10":"two round golden-brown chicken and mushroom patties, one showing minced meat interior",
"m11":"two flat browned beef and carrot patties with fine orange shreds visible",
"m12":"smooth silky steamed egg custard with tiny dried shrimp and translucent winter melon dice",
"m13":"pork meatballs sitting on thick pumpkin slices, steamed, glossy sauce",
"m14":"smooth beige liver puree mixed into white rice porridge with chopped green vegetable",
"m15":"bright red tomato soup with pink shrimp balls and green vegetable",
"v01":"bright green broccoli florets stir-fried with garlic, glossy, small plate",
"v02":"steamed soft orange pumpkin chunks, glistening, arranged neatly",
"v03":"smooth mash of potato and carrot, pale orange color, swirled top",
"v04":"chopped green bok choy stir-fried with sliced shiitake mushrooms",
"v05":"scrambled eggs with soft red tomato chunks, glossy",
"v06":"smooth pale mashed eggplant with a drizzle of sesame paste",
"v07":"sliced white yam stir-fried with black wood ear mushrooms and carrot slices",
"v08":"sweet corn kernels in creamy milk sauce, buttery and glossy",
"v09":"smooth pale green and white potato broccoli mash, swirled",
"v10":"stir-fried hand-torn cabbage with thin carrot strips, glossy",
"v11":"soft white tofu cubes in red tomato sauce, glossy glaze",
"v12":"several long bright orange steamed carrot sticks arranged on a plate",
"v13":"thin slices of pale green stir-fried zucchini, translucent and glossy",
"v14":"green soup with egg ribbons, soft white tofu cubes and chopped spinach",
"v15":"steamed orange pumpkin chunks and white yam chunks side by side",
"p01":"bright red-orange tomato egg drop soup with silky yellow egg ribbons",
"p02":"clear light broth with winter melon slices and pork ribs, very clean",
"p03":"clear broth with egg ribbons, small nori pieces, tofu cubes and dried shrimp",
"p04":"light milky broth with yam chunks, corn segments and carrot, pork ribs",
"p05":"clear light broth with white tofu cubes and chopped green vegetable",
"p06":"smooth thick orange pumpkin cream soup with a swirl, in a white bowl",
"p07":"clear light broth with sliced mushrooms and small green bok choy",
"p08":"clear broth with white fish slices, soft tofu cubes and green vegetable",
"k01":"three small round golden banana oat pancakes, slightly speckled",
"k02":"fluffy steamed orange pumpkin sponge cake cut into squares, airy crumb",
"k03":"three small round purple sweet potato balls with melted cheese inside, one cut open",
"k04":"pale steamed yam and apple cake cut into small squares, soft and moist",
"k05":"a small glass cup of thick white yogurt topped with banana slices and halved blueberries",
"k06":"three small round golden millet patties with tiny orange carrot specks",
"k07":"several long orange steamed sweet potato sticks arranged neatly",
"k08":"six small round pale chicken and corn meatballs, slightly golden",
"k09":"smooth creamy pale green avocado mash with a swirl, in a small bowl",
"k10":"a small pile of tiny golden round egg cookies",
"k11":"smooth creamy orange pumpkin and milk soup in a small bowl",
"k12":"smooth pale yellow steamed egg custard, silky surface, no toppings",
"k13":"small cubes of steamed green broccoli cheese omelette squares",
"k14":"smooth reddish-brown jujube and yam puree in a small bowl",
"k15":"golden egg omelette strips with pink shrimp and orange carrot specks",
}

out = []
for x in D:
    if x["id"] not in VIS:
        print("MISSING VIS:", x["id"], x["name"])
        continue
    out.append({"id": x["id"], "name": x["name"], "prompt": VIS[x["id"]] + ". " + STYLE})

p = os.path.join(os.path.dirname(os.path.abspath(__file__)), "image_prompts.json")
json.dump(out, open(p, "w", encoding="utf-8"), ensure_ascii=False, indent=1)
print("prompts:", len(out), "dishes:", len(D))

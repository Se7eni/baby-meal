# -*- coding: utf-8 -*-
"""把菜品数据里的食材写法规范化，保证能被 Nutrition.java 的食材表识别，
并顺便统一用量写法（半 → 0.5，适量 → 取消）。
"""
import json
import os
import sys

sys.stdout.reconfigure(encoding="utf-8")

BASE = os.path.dirname(os.path.abspath(__file__))
ASSETS = os.path.abspath(os.path.join(BASE, "..", "app", "src", "main", "assets"))
PATH = os.path.join(ASSETS, "dishes.json")

# 食材名 → 营养表里的标准名（None 表示不参与营养计算，仅保留展示）
NAME_MAP = {
    "老南瓜": "南瓜",
    "贝贝南瓜": "南瓜",
    "去核红枣": "红枣",
    "熟鹰嘴豆": "鹰嘴豆",
    "铁棍山药": "山药",
    "鲜香菇": "香菇",
    "鲜虾仁": "鲜虾仁",
    "虾仁": "鲜虾仁",
    "鸡胸肉末": "鸡胸肉",
    "猪里脊": "猪肉末",
    "配方奶": "牛奶",
    "全脂牛奶": "牛奶",
    "鲜牛奶": "牛奶",
    "宝宝奶酪": "奶酪",
    "马苏里拉奶酪": "奶酪",
    "原味酸奶": "无糖酸奶",
    "嫩豆腐块": "嫩豆腐",
    "内酯豆腐": "嫩豆腐",
    "青豆粒": "青豆",
    "胡萝卜末": "胡萝卜",
    "南瓜泥": "南瓜",
    "西兰花碎": "西兰花",
    "菠菜碎": "菠菜",
    "青菜碎": "青菜",
    "干海带丝": "干海带",
    "海带": "干海带",
    "紫菜": "干紫菜",
    "鳕鱼段": "鳕鱼",
    "三文鱼块": "三文鱼",
    "鲈鱼肉": "鲈鱼",
    "白芝麻粉": "白芝麻",
    "芝麻粉": "白芝麻",
    "米醋": None,
    "淀粉": None,
    "玉米淀粉": None,
    "面粉": None,
    "姜丝": "姜末",
    "姜片": None,
    "葱丝": "葱花",
    "植物油": "植物油",
}

# 用量文字 → 数字（配默认单位）
AMOUNT_MAP = {
    "半": ("0.5", None),
    "一小把": (None, None),
    "适量": (None, None),
    "少许": (None, None),
    "一根": ("1", "根"),
    "两片": ("2", "片"),
    "半个": ("0.5", "个"),
}

data = json.load(open(PATH, encoding="utf-8"))
changed_name = 0
changed_amt = 0

for d in data:
    new_ing = []
    for ing in d["ingredients"]:
        name = ing[0]
        amount = str(ing[1])
        unit = ing[2]

        # 1) 用量规范化
        if amount in AMOUNT_MAP:
            nv, nu = AMOUNT_MAP[amount]
            if nv is not None:
                amount = nv
                if nu:
                    unit = nu
                changed_amt += 1

        # 2) 食材名规范化
        if name in NAME_MAP:
            mapped = NAME_MAP[name]
            if mapped is not None:
                name = mapped
                changed_name += 1
            # mapped is None：保留原名（营养表里没有，只是不参与计算）

        new_ing.append([name, amount, unit])
    d["ingredients"] = new_ing

json.dump(data, open(PATH, "w", encoding="utf-8"), ensure_ascii=False, indent=1)
print(f"食材名规范化 {changed_name} 处，用量规范化 {changed_amt} 处，共 {len(data)} 道")

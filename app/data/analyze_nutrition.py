# -*- coding: utf-8 -*-
"""把 dishes.json 的 keyNutrients 映射成 12 项关键营养素，看现有菜单够不够均衡"""
import json
import sys
import collections

sys.stdout.reconfigure(encoding="utf-8")

NUTRIENTS = ["蛋白质", "铁", "锌", "钙", "碘", "DHA", "维生素A", "维生素C", "维生素D", "B族维生素", "膳食纤维", "钾"]

# 菜品 keyNutrients / tags 关键词 → 归入哪几项
RULE = {
    "蛋白质": ["蛋白质", "卵磷脂", "胆碱", "优质脂肪"],
    "铁": ["铁"],
    "锌": ["锌"],
    "钙": ["钙", "维生素K"],
    "碘": ["碘"],
    "DHA": ["DHA"],
    "维生素A": ["维生素A", "番茄红素", "叶黄素"],
    "维生素C": ["维生素C", "叶酸", "花青素", "果胶"],
    "维生素D": ["维生素D"],
    "B族维生素": ["B族维生素", "β-葡聚糖"],
    "膳食纤维": ["膳食纤维", "黏液蛋白", "益生菌"],
    "钾": ["钾"],
}

ROOT = r"C:\Users\dell\WorkBuddy\2026-10-21-09-42-55\app\app\src\main\assets\dishes.json"
data = json.load(open(ROOT, encoding="utf-8"))

pool = {a: [] for a in (1, 2, 3)}
for d in data:
    src = " ".join(d.get("keyNutrients", []) + d.get("tags", []))
    hits = {n for n, kws in RULE.items() if any(k in src for k in kws)}
    d["_nut"] = hits
    for a in d["age"]:
        pool[a].append(d)

print("=== 各月龄段可提供的营养素覆盖（菜品数）===")
for a in (1, 2, 3):
    c = collections.Counter()
    for d in pool[a]:
        for n in d["_nut"]:
            c[n] += 1
    total = len(pool[a])
    print(f"\n月龄段 {a}（可选 {total} 道）")
    for n in NUTRIENTS:
        print(f"  {n:<8} {c[n]:>3} 道")

print("\n=== 单道菜平均覆盖几个营养素 ===")
avg = sum(len(d["_nut"]) for d in data) / len(data)
print(f"  {avg:.2f} 项 / 道")

print("\n=== 完全没有覆盖的营养素 ===")
allhits = collections.Counter()
for d in data:
    for n in d["_nut"]:
        allhits[n] += 1
for n in NUTRIENTS:
    if allhits[n] == 0:
        print("  ⚠️", n)

print("\n=== 1 岁段缺口最严重的营养素 ===")
c1 = collections.Counter()
for d in pool[1]:
    for n in d["_nut"]:
        c1[n] += 1
for n in sorted(NUTRIENTS, key=lambda x: c1[x])[:5]:
    print(f"  {n}: 仅 {c1[n]} 道")

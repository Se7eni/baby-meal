# -*- coding: utf-8 -*-
"""定向实验：只调「维D 权重加成系数」，看能否把维D 达标率抬上来，
同时确认其余 11 项不掉。"""
import importlib.util, sys, os

sys.stdout.reconfigure(encoding='utf-8')
BASE = os.path.dirname(os.path.abspath(__file__))
spec = importlib.util.spec_from_file_location("sim", os.path.join(BASE, "sim_balance.py"))
# 只借工具函数，不执行原来的主流程
src = open(os.path.join(BASE, "sim_balance.py"), encoding="utf-8").read()
src = src.split("CONFIGS = [")[0]
ns = {}
exec(compile(src, "sim", "exec"), ns)

KEYS = ns["KEYS"]
target = ns["target"]
gen_orig = ns["gen"]
pick = ns["pick"]
CATS = ns["CATS"]
PLAN = ns["PLAN"]
import random


def gen(day_i, age, hist, cfg):
    """cfg = {('bonus', key): factor} 或 ('vd_coef', x) 之类"""
    rnd = random.Random(abs(hash((day_i, age, str(cfg)))) & 0xFFFFFF)
    used_today, acc, ids = set(), [0.0] * 12, []
    for s in range(5):
        for c in range(5):
            for _ in range(PLAN[s][c]):
                d = pick_cfg(CATS[c], age, rnd, hist, used_today, acc, cfg)
                if d:
                    used_today.add(d["id"])
                    ids.append(d["id"])
                    acc = [a + b for a, b in zip(acc, d["_n"])]
    return acc, ids


def pick_cfg(cat, age, rnd, hist, used_today, acc, cfg):
    DS = ns["DS"]
    MAG = ns["MAG"]
    pool = [d for d in DS if d["cat"] == cat and age in d["age"] and d["id"] not in used_today]
    if not pool:
        pool = [d for d in DS if d["cat"] == cat]
    if not pool:
        return None
    gap = ns["gaps"](acc, age)
    vd_bonus = cfg.get("vd_bonus", 0.0)   # 额外乘 (1 + vd_bonus * 维D缺口 * 维D强度)
    coef = cfg.get("coef", 2.4)
    w = []
    for d in pool:
        c = hist.get(d["id"], 0)
        wt = 1.0 / (1.0 + c * 3.0) if c > 0 else 1.6
        wt *= 0.85 + rnd.random() * 0.3
        wt *= 1.0 + coef * ns["fit_old"](d["_n"], gap)
        if vd_bonus:
            ivd = KEYS.index("维生素D")
            st = min(1, d["_n"][ivd] / MAG["维生素D"])
            wt *= 1.0 + vd_bonus * gap[ivd] * st
        w.append(wt)
    r = rnd.random() * sum(w)
    for d, wt in zip(pool, w):
        r -= wt
        if r <= 0:
            return d
    return pool[-1]


def run(cfg, days=30, seeds=(0, 1)):
    res = {}
    for age in (1, 2, 3):
        good = [0] * 12
        ss, tot = 0, 0
        for sd in seeds:
            hist = {}
            for i in range(days):
                acc, ids = gen((sd, i), age, hist, cfg)
                st = 0
                for k in range(12):
                    r = acc[k] / target(KEYS[k], age)
                    good[k] += 1 if r >= 0.60 else 0
                    st += min(1, r / 0.60)
                ss += round(st / 12 * 100)
                tot += 1
                for x in ids:
                    hist[x] = hist.get(x, 0) + 1
        res[age] = (round(ss / tot), [round(g * 100 / tot) for g in good])
    return res


CFGS = [
    ("现状 coef2.4", {"coef": 2.4}),
    ("vd_bonus 2", {"coef": 2.4, "vd_bonus": 2}),
    ("vd_bonus 4", {"coef": 2.4, "vd_bonus": 4}),
    ("vd_bonus 6", {"coef": 2.4, "vd_bonus": 6}),
    ("vd_bonus 8", {"coef": 2.4, "vd_bonus": 8}),
]

print("%-18s | 月1 分/维D/最低 | 月2 分/维D/最低 | 月3 分/维D/最低" % "方案")
print("-" * 88)
for name, cfg in CFGS:
    r = run(cfg)
    line = "%-18s" % name
    for age in (1, 2, 3):
        sc, rate = r[age]
        line += " | %3d  %3d%%  %3d%%" % (sc, rate[8], min(rate))
    print(line)
    for age in (1, 2, 3):
        sc, rate = r[age]
        print("     AGE%d %s" % (age, " ".join("%s=%d%%" % (KEYS[i], rate[i]) for i in range(12))))

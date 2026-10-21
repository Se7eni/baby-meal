# -*- coding: utf-8 -*-
"""离线模拟 MenuEngine 选菜算法，对比不同 fitScore / magnitude 下的营养达标率。
只用于决策，不参与 APK 构建。"""
import json, re, random, sys, os
sys.stdout.reconfigure(encoding='utf-8')

BASE = r'C:/Users/dell/WorkBuddy/2026-10-21-09-42-55/app'
KEYS = ["蛋白质", "铁", "锌", "钙", "碘", "DHA", "维生素A", "维生素C",
        "维生素D", "B族维生素", "膳食纤维", "钾"]

t = open(BASE + '/app/src/main/java/com/babymeal/app/Nutrition.java', encoding='utf-8').read()
FOOD = {}
for name, args in re.findall(r'f\("([^"]+)",([^;]+?)\);', t):
    ps = [x.strip() for x in args.split(',')]
    try:
        FOOD[name] = [float(x) for x in ps]
    except ValueError:
        pass


def grams(a, u):
    v = float(str(a).replace('半', '0.5'))
    if u in ('克', '毫升'):
        return v
    return v * {'个': 50, '片': 3, '根': 100, '朵': 8, '颗': 3, '勺': 10}.get(u, 50)


def lookup(n):
    if n in FOOD:
        return FOOD[n]
    best = None
    for k in FOOD:
        if n.startswith(k) or k.startswith(n):
            if best is None or len(k) > len(best):
                best = k
    return FOOD.get(best) if best else None


def of_dish(d):
    acc = [0.0] * 12
    for ing in d['ingredients']:
        fd = lookup(ing[0])
        if not fd:
            continue
        g = grams(ing[1], ing[2])
        for i in range(12):
            acc[i] += g * fd[i] / 100
    return acc


def target(k, age):
    mul = {1: 1.0, 2: 0.92, 3: 0.96}[age]
    R = {
        "蛋白质": 25, "铁": 9, "锌": 4, "钙": 500, "碘": 90,
        "DHA": 100, "维生素A": 310, "维生素C": 40, "维生素D": 10,
        "B族维生素": 1.0, "膳食纤维": 5, "钾": 900,
    }
    if k == "维生素D":
        return 10
    if k == "B族维生素":
        return round(1.0 * mul, 1)
    return round(R[k] * mul)


MAG = {"蛋白质": 8, "铁": 1.5, "锌": 1.0, "钙": 80, "碘": 25, "DHA": 80,
       "维生素A": 120, "维生素C": 12, "维生素D": 1.2, "B族维生素": 0.18,
       "膳食纤维": 1.2, "钾": 200}

DS = json.load(open(BASE + '/app/src/main/assets/dishes.json', encoding='utf-8'))
for d in DS:
    d['_n'] = of_dish(d)

PLAN = [[1, 0, 0, 0, 0], [1, 1, 1, 0, 0], [0, 0, 0, 0, 1], [1, 0, 1, 1, 0], [0, 0, 0, 0, 1]]
CATS = ["staple", "meat", "veg", "soup", "snack"]


def gaps(acc, age):
    g = []
    for i, k in enumerate(KEYS):
        tg = target(k, age)
        g.append(max(0.0, min(1.0, 1 - acc[i] / tg)))
    return g


def fit_old(dish, gap):
    mg = max(0.001, max(gap))
    s = 0
    for i in range(12):
        if gap[i] <= 0:
            continue
        s += gap[i] * min(1, dish[i] / MAG[KEYS[i]])
    return min(1, s / mg / 2.0)


def fit_new(dish, gap, mag):
    """主导项 = 最大单项强度；其余项少量加成。不再被 maxGap / 2 摊薄。"""
    best_one = 0
    rest = 0
    for i in range(12):
        if gap[i] <= 0:
            continue
        st = min(1, dish[i] / mag[KEYS[i]]) * gap[i]
        if st > best_one:
            best_one = st
        else:
            rest += st
    return min(1, best_one * 0.75 + rest * 0.25)


def pick(cat, age, rnd, hist, used_today, acc, mode, mag):
    pool = [d for d in DS if d['cat'] == cat and age in d['age'] and d['id'] not in used_today]
    if not pool:
        pool = [d for d in DS if d['cat'] == cat]
    if not pool:
        return None
    gap = gaps(acc, age)
    mg = dict(MAG)
    mg.update(mag)
    w = []
    for d in pool:
        c = hist.get(d['id'], 0)
        wt = 1.0 / (1.0 + c * 3.0) if c > 0 else 1.6
        wt *= 0.85 + rnd.random() * 0.3
        if mode == 'old':
            f = fit_old(d['_n'], gap)
        else:
            f = fit_new(d['_n'], gap, mg)
        wt *= 1.0 + 2.4 * f
        w.append(wt)
    tot = sum(w)
    r = rnd.random() * tot
    for d, wt in zip(pool, w):
        r -= wt
        if r <= 0:
            return d
    return pool[-1]


def run(mode, mag, days=60, seeds=(0, 1, 2)):
    out = {}
    for age in (1, 2, 3):
        good = [0] * 12
        score_sum = 0
        total = 0
        for sd in seeds:
            for i in range(days):
                acc = gen((sd, i), age, {}, mode, mag)
                st = 0
                for k in range(12):
                    r = acc[k] / target(KEYS[k], age)
                    good[k] += 1 if r >= 0.60 else 0
                    st += min(1, r / 0.60)
                score_sum += round(st / 12 * 100)
                total += 1
        rate = [round(g * 100 / total) for g in good]
        out[age] = (round(score_sum / total), rate)
    return out


def gen(day_i, age, hist, mode='old', mag=None):
    rnd = random.Random(abs(hash((day_i, age, mode))) & 0xFFFFFF)
    used_today = set()
    acc = [0.0] * 12
    picked_ids = []
    for s in range(5):
        for c in range(5):
            for _ in range(PLAN[s][c]):
                d = pick(CATS[c], age, rnd, hist, used_today, acc, mode, mag or {})
                if d:
                    used_today.add(d['id'])
                    picked_ids.append(d['id'])
                    acc = [a + b for a, b in zip(acc, d['_n'])]
    return acc, picked_ids


def run(mode, mag, days=30, seeds=(0,), use_hist=True):
    """use_hist=True 与 Java 测试一致：history 跨天累积（降权项）"""
    out = {}
    for age in (1, 2, 3):
        good = [0] * 12
        score_sum = 0
        total = 0
        for sd in seeds:
            hist = {}
            for i in range(days):
                acc, ids = gen((sd, i), age, hist, mode, mag)
                st = 0
                for k in range(12):
                    r = acc[k] / target(KEYS[k], age)
                    good[k] += 1 if r >= 0.60 else 0
                    st += min(1, r / 0.60)
                score_sum += round(st / 12 * 100)
                total += 1
                if use_hist:
                    for x in ids:
                        hist[x] = hist.get(x, 0) + 1
        rate = [round(g * 100 / total) for g in good]
        out[age] = (round(score_sum / total), rate)
    return out


CONFIGS = [
    ("A 现状 fit_old + vd1.2", 'old', {}),
    ("B fit_new + vd1.2", 'new', {}),
    ("C fit_new + vd0.9", 'new', {"维生素D": 0.9}),
    ("D fit_new + vd0.7", 'new', {"维生素D": 0.7}),
]

hdr = "%-26s" % "方案(带history)"
for age in (1, 2, 3):
    hdr += " | 月%d 分/维D/最低" % age
print(hdr)
print("-" * 100)
for name, mode, mag in CONFIGS:
    r = run(mode, mag, days=30, seeds=(0, 1))
    line = "%-26s" % name
    for age in (1, 2, 3):
        sc, rate = r[age]
        line += " | %3d %3d%% %3d%%" % (sc, rate[8], min(rate))
    print(line)
    for age in (1, 2, 3):
        sc, rate = r[age]
        print("      AGE%d: " % age + " ".join("%s=%d%%" % (KEYS[i], rate[i]) for i in range(12)))

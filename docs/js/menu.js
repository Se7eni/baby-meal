/**
 * 每日菜单生成器 —— 与安卓版 MenuEngine.java 算法逐项对齐
 *
 * 1. 以「日期 + 月龄段」为随机种子 → 同一天重复打开结果一致
 * 2. 记录最近 N 天吃过的菜，生成时降权，做到「尽量不重复」
 * 3. 每天 5 餐，每餐按类别配额选菜
 * 4. 营养缺口驱动：每选一道菜先算「现在最缺哪项营养」，优先选能补上缺口的那道
 * 5. 「换一换」只换某一餐，换出的菜当天不重复，同样考虑营养缺口
 */
(function (global) {
  'use strict';

  const SLOT_NAMES = ['早餐', '午餐', '下午加餐', '晚餐', '睡前加餐'];
  const SLOT_TIMES = ['7:30 - 8:30', '11:30 - 12:30', '15:00 - 15:30', '17:30 - 18:30', '20:00 - 20:30'];
  const SLOT_EMOJI = ['🌅', '🍚', '🍎', '🌙', '🥛'];

  /** 每个餐位的菜量配额 [staple, meat, veg, soup, snack] */
  const PLAN = [
    [1, 0, 0, 0, 0], // 早餐
    [1, 1, 1, 0, 0], // 午餐
    [0, 0, 0, 0, 1], // 加餐
    [1, 0, 1, 1, 0], // 晚餐
    [0, 0, 0, 0, 1]  // 睡前
  ];

  const CATS = ['staple', 'meat', 'veg', 'soup', 'snack'];

  /** 稀缺营养素加成系数。4 是离线模拟试出来的最优值 */
  const SCARCE_BONUS = 4.0;

  let ALL = [];

  function setDishes(list) { ALL = list; }

  /** Java String.hashCode 的等价实现，保证前后端种子一致 */
  function javaHash(s) {
    let h = 0;
    for (let i = 0; i < s.length; i++) h = (Math.imul(31, h) + s.charCodeAt(i)) | 0;
    return Math.abs(h);
  }

  /**
   * 可复现的伪随机数发生器（mulberry32）。
   * 安卓用 java.util.Random(seed)，不可移植；这里用确定性 PRNG 保证
   * 「同一天打开结果一致」这个核心体验完全成立。
   */
  function makeRng(seed) {
    let a = seed >>> 0;
    return function () {
      a = (a + 0x6D2B79F5) >>> 0;
      let t = a;
      t = Math.imul(t ^ (t >>> 15), t | 1);
      t ^= t + Math.imul(t ^ (t >>> 7), t | 61);
      return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
    };
  }

  function fitsAge(dish, age) {
    return dish.age.indexOf(age) >= 0;
  }

  function gaps(acc, age) {
    const g = new Array(Nutrition.KEYS.length).fill(0);
    for (let i = 0; i < g.length; i++) {
      const t = Nutrition.target(Nutrition.KEYS[i], age);
      g[i] = t <= 0 ? 0 : Math.max(0, Math.min(1, 1 - acc[i] / t));
    }
    return g;
  }

  /** 每项营养素的「一份有效贡献」量级，用于归一化 */
  function magnitude(key) {
    const N = Nutrition;
    switch (key) {
      case N.PROTEIN: return 8;
      case N.IRON: return 1.5;
      case N.ZINC: return 1.0;
      case N.CALCIUM: return 80;
      case N.IODINE: return 25;
      case N.DHA: return 80;
      case N.VIT_A: return 120;
      case N.VIT_C: return 12;
      case N.VIT_D: return 1.2;
      case N.VIT_B: return 0.18;
      case N.FIBER: return 1.2;
      case N.POTASSIUM: return 200;
      default: return 1;
    }
  }

  /** 这道菜对当前缺口的补足程度 0..1 */
  function fitScore(dish, gap) {
    let sum = 0;
    let maxGap = 0.001;
    for (let i = 0; i < gap.length; i++) maxGap = Math.max(maxGap, gap[i]);
    for (let i = 0; i < dish.length && i < gap.length; i++) {
      if (gap[i] <= 0) continue;
      const strength = Math.min(1, dish[i] / magnitude(Nutrition.KEYS[i]));
      sum += gap[i] * strength;
    }
    return Math.min(1, sum / maxGap / 2.0);
  }

  /**
   * 稀缺营养素加成强度 0..1。
   * 维 D 只存在于极少数食材，普通加权抢不到出场机会，所以单开一条通道。
   */
  function scarceBoost(dish, gap) {
    const i = Nutrition.KEYS.indexOf(Nutrition.VIT_D);
    if (i < 0 || i >= gap.length || gap[i] <= 0) return 0;
    const strength = Math.min(1, dish[i] / magnitude(Nutrition.VIT_D));
    return gap[i] * strength;
  }

  function addInto(acc, one) {
    for (let i = 0; i < acc.length && i < one.length; i++) acc[i] += one[i];
  }

  /** 在某类别里抽一道菜 */
  function pick(cat, age, rnd, history, usedToday, acc) {
    let pool = ALL.filter(d => d.cat === cat && fitsAge(d, age) && !usedToday.has(d.id));

    // 池子太小则放宽到相邻月龄段
    if (pool.length < 3) {
      for (const d of ALL) {
        if (d.cat !== cat) continue;
        if (usedToday.has(d.id)) continue;
        if ((fitsAge(d, age + 1) || fitsAge(d, age - 1)) && !pool.includes(d)) pool.push(d);
      }
    }
    if (pool.length === 0) {
      pool = ALL.filter(d => d.cat === cat && !usedToday.has(d.id));
    }
    if (pool.length === 0) return null;

    const gap = gaps(acc, age);
    const w = new Array(pool.length);
    let total = 0;

    for (let i = 0; i < pool.length; i++) {
      const d = pool[i];
      // 历史降权
      const cnt = history.get(d.id) || 0;
      let weight = cnt > 0 ? 1.0 / (1.0 + cnt * 3.0) : 1.6;
      // 随机抖动
      weight *= 0.85 + rnd() * 0.3;
      // 营养缺口匹配
      const nut = Nutrition.ofDish(d);
      weight *= 1.0 + 2.4 * fitScore(nut, gap);
      // 稀缺营养素加成
      weight *= 1.0 + SCARCE_BONUS * scarceBoost(nut, gap);
      w[i] = weight;
      total += weight;
    }
    if (total <= 0) return pool[Math.floor(rnd() * pool.length)];

    let r = rnd() * total;
    for (let i = 0; i < pool.length; i++) {
      r -= w[i];
      if (r <= 0) return pool[i];
    }
    return pool[pool.length - 1];
  }

  function newSlot(index) {
    return {
      index,
      dishes: [],
      name() { return SLOT_NAMES[index]; },
      time() { return SLOT_TIMES[index]; },
      emoji() { return SLOT_EMOJI[index]; },
      kcalTotal() {
        let t = 0;
        for (const d of this.dishes) t += parseKcal(d.kcal);
        return t;
      }
    };
  }

  function newDay(dateKey, age) {
    return {
      dateKey,
      age,
      slots: [],
      allDishes() {
        const r = [];
        for (const s of this.slots) r.push(...s.dishes);
        return r;
      },
      intake() { return Nutrition.ofDay(this); },
      score() { return Nutrition.balanceScore(this.intake(), this.age); }
    };
  }

  /** 生成某天完整菜单。history 为最近几天吃过的菜 id */
  function generate(dateKey, age, history, salt) {
    const day = newDay(dateKey, age);
    const seed = javaHash(dateKey + '#' + age + '#' + salt);

    const used = new Map();
    for (const id of history) used.set(id, (used.get(id) || 0) + 1);

    const usedToday = new Set();
    const acc = new Array(Nutrition.KEYS.length).fill(0);

    for (let s = 0; s < 5; s++) {
      const slot = newSlot(s);
      day.slots.push(slot);
      const plan = PLAN[s];
      const rnd = makeRng((seed * 31 + s * 7919) >>> 0);

      for (let c = 0; c < CATS.length; c++) {
        for (let n = 0; n < plan[c]; n++) {
          const picked = pick(CATS[c], age, rnd, used, usedToday, acc);
          if (picked) {
            slot.dishes.push(picked);
            usedToday.add(picked.id);
            addInto(acc, Nutrition.ofDish(picked));
          }
        }
      }
      if (slot.dishes.length === 0) {
        const d = pick('snack', age, rnd, used, usedToday, acc);
        if (d) {
          slot.dishes.push(d);
          usedToday.add(d.id);
          addInto(acc, Nutrition.ofDish(d));
        }
      }
    }
    return day;
  }

  /** 只重抽某一餐 */
  function regenerateSlot(day, slotIndex, history, salt) {
    const old = day.slots[slotIndex];
    const others = day.allDishes().filter(d => !old.dishes.includes(d));

    const acc = new Array(Nutrition.KEYS.length).fill(0);
    for (const d of others) addInto(acc, Nutrition.ofDish(d));

    const usedToday = new Set(others.map(d => d.id));

    const used = new Map();
    for (const id of history) used.set(id, (used.get(id) || 0) + 1);

    const slot = newSlot(slotIndex);
    const plan = PLAN[slotIndex];
    const seed = javaHash(day.dateKey + '#' + slotIndex + '#' + salt + '#' + Date.now() + '#' + Math.random());
    const rnd = makeRng(seed);

    for (let c = 0; c < CATS.length; c++) {
      for (let n = 0; n < plan[c]; n++) {
        const picked = pick(CATS[c], day.age, rnd, used, usedToday, acc);
        if (picked) {
          slot.dishes.push(picked);
          usedToday.add(picked.id);
          addInto(acc, Nutrition.ofDish(picked));
        }
      }
    }
    if (slot.dishes.length === 0) {
      const d = pick('snack', day.age, rnd, used, usedToday, acc);
      if (d) slot.dishes.push(d);
    }
    day.slots[slotIndex] = slot;
    return slot;
  }

  function parseKcal(s) {
    if (!s) return 0;
    const m = String(s).match(/\d+/g);
    return m ? parseInt(m.join(''), 10) : 0;
  }

  /** "d1,d2|d3,d4|..." 序列化 */
  function pack(day) {
    return day.slots.map(s => s.dishes.map(d => d.id).join(',')).join('|');
  }

  function unpack(dateKey, age, packed, byId) {
    const day = newDay(dateKey, age);
    const parts = String(packed).split('|');
    for (let i = 0; i < 5; i++) {
      const s = newSlot(i);
      if (i < parts.length && parts[i]) {
        for (const id of parts[i].split(',')) {
          const d = byId(id);
          if (d) s.dishes.push(d);
        }
      }
      day.slots.push(s);
    }
    return day;
  }

  function catName(cat) {
    switch (cat) {
      case 'staple': return '主食';
      case 'meat': return '荤菜';
      case 'veg': return '素菜';
      case 'soup': return '汤羹';
      case 'snack': return '点心';
      default: return '其他';
    }
  }

  function catEmoji(cat) {
    switch (cat) {
      case 'staple': return '🍚';
      case 'meat': return '🍖';
      case 'veg': return '🥬';
      case 'soup': return '🍲';
      case 'snack': return '🍪';
      default: return '🍽';
    }
  }

  function ageName(age) {
    if (age === 1) return '1-1.5岁';
    if (age === 2) return '1.5-2岁';
    if (age === 3) return '2-3岁';
    return '1-3岁';
  }

  function ageHint(age) {
    if (age === 1) return '质地细腻、小块软烂，不加盐不加糖';
    if (age === 2) return '可以吃小丁小条，练习咀嚼，清淡少盐';
    return '接近家庭餐，但仍需少盐少油、切小块';
  }

  /** 合并所有食材为购物清单 */
  function shoppingList(dishes) {
    const m = new Map();
    for (const d of dishes) {
      for (const ing of d.ingredients) {
        const key = ing[0] + '|' + (ing[2] || '');
        const v = parseFloat(ing[1]);
        m.set(key, (m.get(key) || 0) + (isNaN(v) ? 0 : Math.trunc(v)));
      }
    }
    return m;
  }

  global.MenuEngine = {
    SLOT_NAMES, SLOT_TIMES, SLOT_EMOJI, SCARCE_BONUS,
    setDishes, generate, regenerateSlot, pack, unpack,
    catName, catEmoji, ageName, ageHint,
    parseKcal, shoppingList, javaHash
  };
})(typeof window !== 'undefined' ? window : globalThis);

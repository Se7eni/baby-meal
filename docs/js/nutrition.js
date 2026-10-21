/**
 * 营养引擎 —— 与安卓版 Nutrition.java 逐项对齐
 *
 * 1. 12 项 1-3 岁宝宝最需要关注的营养素，以及各月龄段的每日推荐量
 * 2. 常见食材每 100 克营养含量（参考中国食物成分表）
 * 3. 把「食材 + 用量」换算成营养贡献，累加成全天摄入，与推荐量比较得出达标率
 *
 * 数据为常见品种平均值，用于「够不够」的量级判断，不替代精确营养分析，
 * 也不构成医学建议。
 */
(function (global) {
  'use strict';

  const PROTEIN = '蛋白质';
  const IRON = '铁';
  const ZINC = '锌';
  const CALCIUM = '钙';
  const IODINE = '碘';
  const DHA = 'DHA';
  const VIT_A = '维生素A';
  const VIT_C = '维生素C';
  const VIT_D = '维生素D';
  const VIT_B = 'B族维生素';
  const FIBER = '膳食纤维';
  const POTASSIUM = '钾';

  const KEYS = [PROTEIN, IRON, ZINC, CALCIUM, IODINE, DHA,
    VIT_A, VIT_C, VIT_D, VIT_B, FIBER, POTASSIUM];

  function unit(key) {
    if (key === VIT_A) return 'μgRAE';
    if (key === VIT_D) return 'μg';
    if (key === IODINE) return 'μg';
    return 'mg';
  }

  function why(key) {
    switch (key) {
      case PROTEIN: return '长肌肉、长个子、修复组织，免疫抗体的原料';
      case IRON: return '造血运氧。缺了容易贫血、面色黄、精神差、注意力不集中';
      case ZINC: return '味觉发育和免疫。缺了容易没胃口、挑食、反复感冒';
      case CALCIUM: return '骨骼和牙齿的基础，配合维 D 才吸收得好';
      case IODINE: return '合成甲状腺激素，直接影响身高和智力发育';
      case DHA: return '大脑和视网膜的结构成分，支持认知与视力';
      case VIT_A: return '保护呼吸道和肠道黏膜，维护暗光视力';
      case VIT_C: return '促进铁吸收、增强免疫，帮助伤口愈合';
      case VIT_D: return '帮助钙吸收，缺了补再多钙也进不了骨头';
      case VIT_B: return '能量代谢的辅酶，B12 参与神经发育';
      case FIBER: return '促进肠道蠕动、预防便秘，养护肠道菌群';
      case POTASSIUM: return '维持体液平衡和肌肉收缩，出汗多时尤其需要';
      default: return '';
    }
  }

  function emoji(key) {
    switch (key) {
      case PROTEIN: return '🥩';
      case IRON: return '🩸';
      case ZINC: return '🍤';
      case CALCIUM: return '🦴';
      case IODINE: return '🌊';
      case DHA: return '🐟';
      case VIT_A: return '🥕';
      case VIT_C: return '🍊';
      case VIT_D: return '☀️';
      case VIT_B: return '🌾';
      case FIBER: return '🥬';
      case POTASSIUM: return '🍌';
      default: return '•';
    }
  }

  function round1(v) { return Math.round(v * 10) / 10; }

  /**
   * 每日推荐摄入量（1-3 岁），《中国居民膳食营养素参考摄入量（2023）》
   * 维 D 固定 10μg（按日照 + 补充剂总量给出，不随月龄打折）
   */
  function target(key, age) {
    const mul = age === 1 ? 0.92 : age === 2 ? 0.96 : 1.0;
    switch (key) {
      case PROTEIN: return Math.round(25 * mul);
      case IRON: return round1(9 * mul);
      case ZINC: return round1(4.0 * mul);
      case CALCIUM: return Math.round(500 * mul);
      case IODINE: return Math.round(90 * mul);
      case DHA: return Math.round(100 * mul);
      case VIT_A: return Math.round(310 * mul);
      case VIT_C: return Math.round(40 * mul);
      case VIT_D: return 10;
      case VIT_B: return round1(1.0 * mul);
      case FIBER: return Math.round(5 * mul);
      case POTASSIUM: return Math.round(900 * mul);
      default: return 1;
    }
  }

  /* 食材营养表：每 100 克可食部分
     [蛋白质, 铁, 锌, 钙, 碘, DHA, 维A, 维C, 维D, B族, 纤维, 钾]
     单位：蛋白质/铁/锌/钙/B族/纤维/钾 = mg，碘/维A/维D = μg，DHA = mg */
  const FOOD = new Map();

  function f(name, p, fe, zn, ca, i, dha, va, vc, vd, vb, fiber, k) {
    FOOD.set(name, [p, fe, zn, ca, i, dha, va, vc, vd, vb, fiber, k]);
  }

  /* ---- 谷薯类 ---- */
  f('大米', 7.4, 2.3, 1.7, 13, 2.3, 0, 0, 0, 0, 0.11, 0.7, 103);
  f('糙米', 7.7, 1.1, 1.7, 11, 2.3, 0, 0, 0, 0, 0.33, 3.4, 230);
  f('小米', 9.0, 5.1, 1.9, 41, 3.7, 0, 17, 0, 0, 0.33, 1.6, 284);
  f('面粉', 11.2, 3.5, 1.6, 31, 2.9, 0, 0, 0, 0, 0.28, 2.7, 190);
  f('宝宝面条', 10.3, 3.6, 1.1, 16, 2.0, 0, 0, 0, 0, 0.20, 1.5, 135);
  f('宝宝意面', 11.0, 3.5, 1.2, 20, 2.0, 0, 0, 0, 0, 0.20, 2.0, 150);
  f('全麦吐司', 8.5, 3.6, 1.5, 90, 3.0, 0, 0, 0, 0, 0.30, 6.0, 250);
  f('即食燕麦', 15.0, 7.0, 2.4, 186, 5.0, 0, 0, 0, 0, 0.45, 5.3, 350);
  f('土豆', 2.0, 0.8, 0.4, 8, 0.5, 0, 0, 27, 0, 0.08, 0.7, 342);
  f('红薯', 1.1, 0.5, 0.15, 23, 0.8, 0, 125, 26, 0, 0.10, 1.6, 130);
  f('紫薯', 1.3, 0.6, 0.2, 25, 0.8, 0, 60, 20, 0, 0.10, 2.0, 200);
  f('山药', 1.9, 0.3, 0.27, 16, 0.6, 0, 3, 5, 0, 0.05, 0.8, 213);
  f('玉米粒', 4.0, 1.1, 0.9, 26, 1.0, 0, 8, 16, 0, 0.15, 2.9, 238);
  f('鹰嘴豆', 21.2, 6.0, 3.4, 150, 1.0, 0, 1, 5, 0, 0.45, 11.6, 875);

  /* ---- 蛋奶类 ---- */
  f('鸡蛋', 13.3, 2.0, 1.1, 56, 27.0, 10, 234, 0, 2.0, 0.12, 0, 154);
  f('蛋黄', 15.2, 6.5, 3.8, 112, 25.0, 30, 438, 0, 5.4, 0.30, 0, 167);
  f('牛奶', 3.0, 0.1, 0.4, 104, 15.0, 5, 24, 1, 0.5, 0.05, 0, 150);
  f('奶酪', 25.7, 2.4, 3.5, 799, 40.0, 0, 152, 0, 0.6, 0.08, 0, 75);
  f('无糖酸奶', 2.5, 0.4, 0.5, 118, 18.0, 0, 26, 1, 0.3, 0.06, 0, 155);

  /* ---- 畜禽肉 ---- */
  f('猪肉末', 20.3, 1.6, 3.0, 6, 1.2, 0, 44, 0, 0.2, 0.55, 0, 317);
  f('排骨', 18.3, 1.0, 1.7, 8, 2.0, 0, 12, 0, 0.2, 0.42, 0, 274);
  f('鸡胸肉', 19.4, 0.6, 0.5, 3, 5.0, 20, 16, 0, 0.1, 0.60, 0, 338);
  f('鸡腿肉', 16.0, 1.5, 1.1, 6, 5.0, 15, 44, 0, 0.2, 0.50, 0, 242);
  f('牛肉末', 20.2, 2.8, 3.7, 10, 3.0, 10, 7, 0, 0.1, 0.06, 0, 305);
  f('牛腩', 17.1, 2.4, 3.2, 9, 3.0, 10, 6, 0, 0.1, 0.06, 0, 238);
  f('猪肝', 19.3, 22.6, 5.8, 6, 4.0, 20, 4972, 20, 0.5, 2.20, 0, 235);
  f('鸡肝', 16.6, 12.0, 2.4, 7, 3.0, 20, 10414, 7, 0.5, 1.60, 0, 197);
  f('鸭血', 13.6, 30.5, 0.9, 5, 2.0, 0, 0, 0, 0, 0.10, 0, 186);

  /* ---- 水产（关键：碘、DHA、锌）---- */
  f('三文鱼', 17.2, 0.3, 0.4, 13, 40.0, 1200, 45, 0, 11.0, 0.50, 0, 361);
  f('鳕鱼', 20.4, 0.5, 0.5, 42, 60.0, 200, 15, 0, 1.0, 0.30, 0, 321);
  f('鲈鱼', 18.6, 2.0, 2.8, 138, 50.0, 150, 19, 0, 1.5, 0.20, 0, 205);
  f('鲜虾仁', 18.6, 1.5, 2.4, 62, 60.0, 100, 15, 0, 0.1, 0.30, 0, 228);
  f('虾皮', 30.7, 6.7, 1.9, 991, 400.0, 30, 19, 0, 0.2, 0.20, 0, 617);
  f('银鱼干', 17.2, 2.0, 2.5, 760, 150.0, 150, 20, 0, 2.0, 0.20, 0, 300);
  f('鲜牡蛎', 5.3, 7.1, 9.4, 131, 100.0, 200, 27, 0, 0.3, 0.20, 0, 200);
  f('干海带', 1.8, 0.9, 0.5, 348, 3600.0, 0, 0, 0, 0, 0.10, 6.1, 761);
  f('海带丝', 1.2, 0.6, 0.3, 230, 2400.0, 0, 0, 0, 0, 0.08, 4.0, 500);
  f('干紫菜', 26.7, 54.9, 2.5, 264, 1500.0, 100, 228, 2, 1.0, 1.50, 21.6, 1796);
  f('鱼片', 18.0, 0.8, 0.6, 30, 50.0, 300, 15, 0, 2.0, 0.25, 0, 280);

  /* ---- 豆制品 ---- */
  f('嫩豆腐', 6.2, 1.5, 0.7, 116, 3.0, 0, 0, 0, 0, 0.10, 0.4, 154);
  f('北豆腐', 12.2, 1.9, 1.1, 138, 3.0, 0, 0, 0, 0, 0.10, 0.5, 106);
  f('豆浆', 3.0, 1.1, 0.4, 15, 2.0, 0, 0, 0, 0, 0.05, 0.2, 60);

  /* ---- 蔬菜 ---- */
  f('南瓜', 0.7, 0.4, 0.3, 16, 0.5, 0, 148, 8, 0, 0.05, 0.8, 145);
  f('胡萝卜', 1.0, 1.0, 0.2, 32, 1.0, 0, 688, 13, 0, 0.07, 1.1, 190);
  f('西兰花', 4.1, 1.0, 0.8, 67, 2.0, 0, 1202, 51, 0, 0.20, 1.6, 17);
  f('菠菜', 2.6, 2.9, 0.85, 66, 3.0, 0, 487, 32, 0, 0.24, 1.7, 311);
  f('青菜', 1.5, 1.5, 0.4, 90, 3.0, 0, 252, 28, 0, 0.15, 1.1, 178);
  f('鸡毛菜', 2.0, 1.8, 0.5, 110, 3.0, 0, 260, 40, 0, 0.15, 1.2, 200);
  f('番茄', 0.9, 0.4, 0.13, 10, 0.8, 0, 55, 19, 0, 0.06, 0.5, 163);
  f('洋葱', 1.1, 0.6, 0.2, 24, 1.0, 0, 3, 8, 0, 0.05, 0.9, 147);
  f('香菇', 2.2, 0.3, 0.66, 2, 1.5, 0, 0, 1, 1.6, 0.15, 3.3, 20);
  f('蘑菇', 2.7, 1.2, 0.9, 6, 1.5, 0, 0, 2, 0.9, 0.15, 2.1, 312);
  f('冬瓜', 0.4, 0.2, 0.07, 19, 1.0, 0, 0, 18, 0, 0.03, 0.7, 78);
  f('茄子', 1.1, 0.4, 0.23, 24, 1.0, 0, 8, 5, 0, 0.06, 1.3, 142);
  f('西葫芦', 0.8, 0.6, 0.12, 15, 1.0, 0, 5, 6, 0, 0.04, 0.6, 92);
  f('芦笋', 1.4, 1.4, 0.41, 10, 1.0, 0, 42, 45, 0, 0.15, 1.9, 213);
  f('彩椒', 1.0, 0.8, 0.19, 11, 1.0, 0, 52, 104, 0, 0.06, 1.4, 142);
  f('包菜', 1.5, 0.6, 0.25, 49, 1.0, 0, 5, 40, 0, 0.08, 1.0, 124);
  f('木耳', 1.5, 5.5, 0.5, 34, 5.0, 0, 3, 1, 0, 0.10, 2.6, 52);
  f('莲藕', 1.9, 1.4, 0.23, 39, 1.0, 0, 3, 44, 0, 0.12, 1.2, 243);
  f('青豆', 5.0, 1.5, 0.7, 25, 1.5, 0, 66, 14, 0, 0.20, 3.0, 332);
  f('红枣', 3.2, 2.3, 0.65, 64, 2.0, 0, 2, 14, 0, 0.15, 6.2, 524);
  f('枸杞', 13.9, 5.4, 1.5, 60, 2.0, 0, 1625, 48, 0, 0.30, 16.9, 434);
  f('香菜', 1.8, 2.9, 0.45, 101, 2.0, 0, 193, 48, 0, 0.12, 1.2, 272);
  f('葱花', 1.7, 1.3, 0.4, 72, 2.0, 0, 140, 17, 0, 0.10, 1.3, 144);
  f('姜末', 1.3, 1.4, 0.34, 27, 1.0, 0, 28, 4, 0, 0.10, 2.7, 295);
  f('蒜末', 4.5, 1.2, 0.7, 39, 1.0, 0, 5, 7, 0, 0.15, 1.1, 302);

  /* ---- 水果 ---- */
  f('香蕉', 1.4, 0.4, 0.18, 7, 1.0, 0, 10, 8, 0, 0.08, 1.2, 256);
  f('苹果', 0.2, 0.6, 0.19, 4, 0.5, 0, 3, 4, 0, 0.04, 1.2, 119);
  f('蓝莓', 0.7, 0.3, 0.16, 6, 0.5, 0, 3, 10, 0, 0.05, 2.4, 77);
  f('牛油果', 2.0, 0.55, 0.64, 12, 1.0, 0, 7, 10, 0, 0.20, 6.7, 485);

  /* ---- 油脂坚果 ---- */
  f('植物油', 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
  f('香油', 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
  f('芝麻酱', 19.2, 22.7, 4.0, 1170, 4.0, 0, 0, 0, 0, 0.55, 5.9, 342);
  f('白芝麻', 18.4, 14.1, 6.1, 620, 4.0, 0, 0, 0, 0, 0.50, 9.8, 266);
  f('清水', 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
  f('温水', 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);

  /** 用量换算为克；-1 表示无法换算（如「适量」） */
  function grams(amount, unitStr) {
    if (amount === null || amount === undefined || amount === '') return -1;
    let v = parseFloat(amount);
    if (isNaN(v)) {
      const slash = String(amount).indexOf('/');
      if (slash > 0) {
        const a = parseFloat(String(amount).slice(0, slash));
        const b = parseFloat(String(amount).slice(slash + 1));
        if (isNaN(a) || isNaN(b)) return -1;
        v = a / b;
      } else {
        return -1;
      }
    }
    switch (unitStr) {
      case '克': return v;
      case '毫升': return v;
      case '个': return v * 50;
      case '片': return v * 3;
      case '根': return v * 100;
      case '朵': return v * 8;
      case '颗': return v * 3;
      case '勺': return v * 10;
      default: return v;
    }
  }

  /** 在营养表里查食材（精确优先，其次最长包含匹配） */
  function lookup(name) {
    if (!name) return null;
    const exact = FOOD.get(name);
    if (exact) return exact;
    let best = null;
    for (const key of FOOD.keys()) {
      if (name.indexOf(key) >= 0 || key.indexOf(name) >= 0) {
        if (best === null || key.length > best.length) best = key;
      }
    }
    return best === null ? null : FOOD.get(best);
  }

  /** 单道菜的营养贡献；无法识别的食材不计入 */
  function ofDish(dish) {
    const sum = new Array(KEYS.length).fill(0);
    for (const ing of dish.ingredients) {
      const g = grams(ing[1], ing[2]);
      if (g <= 0) continue;
      const per = lookup(ing[0]);
      if (!per) continue;
      for (let i = 0; i < KEYS.length; i++) sum[i] += per[i] * g / 100.0;
    }
    return sum;
  }

  function ofDay(day) {
    const sum = new Array(KEYS.length).fill(0);
    for (const d of day.allDishes()) {
      const one = ofDish(d);
      for (let i = 0; i < KEYS.length; i++) sum[i] += one[i];
    }
    return sum;
  }

  function indexOf(key) {
    const i = KEYS.indexOf(key);
    return i < 0 ? 0 : i;
  }

  function ratio(intake, key, age) {
    const t = target(key, age);
    if (t <= 0) return 0;
    return intake[indexOf(key)] / t;
  }

  const LEVEL_GOOD = 2;
  const LEVEL_OK = 1;
  const LEVEL_LOW = 0;

  /** 达标线：菜品贡献达到推荐量的 60% 即算达标（其余由奶和水果补齐） */
  function level(r) {
    if (r >= 0.60) return LEVEL_GOOD;
    if (r >= 0.35) return LEVEL_OK;
    return LEVEL_LOW;
  }

  function levelText(lv) {
    if (lv === LEVEL_GOOD) return '达标';
    if (lv === LEVEL_OK) return '基本够';
    return '偏少';
  }

  function levelColor(lv) {
    if (lv === LEVEL_GOOD) return '#4E9A54';
    if (lv === LEVEL_OK) return '#D99A2B';
    return '#E06B5C';
  }

  function levelSoft(lv) {
    if (lv === LEVEL_GOOD) return '#EAF5E8';
    if (lv === LEVEL_OK) return '#FFF4DE';
    return '#FDEBE8';
  }

  function fixAdvice(key) {
    switch (key) {
      case PROTEIN: return '加一份鸡蛋、豆腐或瘦肉';
      case IRON: return '加牛肉、猪肝或鸭血（搭配番茄、彩椒一起，维 C 帮铁吸收）';
      case ZINC: return '加牡蛎、牛肉或蛋黄';
      case CALCIUM: return '加奶制品、豆腐或芝麻酱拌菜';
      case IODINE: return '加紫菜虾皮汤或海带（每周 2-3 次即可）';
      case DHA: return '加三文鱼、鳕鱼或牡蛎';
      case VIT_A: return '加南瓜、胡萝卜或猪肝';
      case VIT_C: return '加彩椒、西兰花或番茄';
      case VIT_D: return '加蛋黄、三文鱼或香菇，另外记得每天补 400IU 维 D 滴剂';
      case VIT_B: return '用糙米替代部分白米，或加燕麦、猪肝';
      case FIBER: return '加绿叶菜、燕麦或红薯';
      case POTASSIUM: return '加香蕉、土豆或冬瓜';
      default: return '';
    }
  }

  /** 全天均衡分 0-100 */
  function balanceScore(intake, age) {
    let sum = 0;
    for (const k of KEYS) {
      sum += Math.min(1.0, ratio(intake, k, age) / 0.60);
    }
    return Math.round(sum / KEYS.length * 100);
  }

  function scoreWord(score) {
    if (score >= 90) return '非常均衡';
    if (score >= 75) return '比较均衡';
    if (score >= 55) return '有待改善';
    return '营养缺口较大';
  }

  /** 缺口最大的几项（按缺口从大到小，只返回未达标的） */
  function gaps(intake, age, n) {
    const list = KEYS.slice().sort((a, b) => {
      const ra = Math.min(1, ratio(intake, a, age) / 0.60);
      const rb = Math.min(1, ratio(intake, b, age) / 0.60);
      return ra - rb;
    });
    const out = [];
    for (let i = 0; i < Math.min(n, list.length); i++) {
      if (ratio(intake, list[i], age) < 0.60) out.push(list[i]);
    }
    return out;
  }

  function fmt(v) {
    if (v >= 100) return String(Math.round(v));
    if (v >= 10) return String(Math.round(v * 10) / 10).replace('.0', '');
    return String(Math.round(v * 10) / 10);
  }

  global.Nutrition = {
    PROTEIN, IRON, ZINC, CALCIUM, IODINE, DHA,
    VIT_A, VIT_C, VIT_D, VIT_B, FIBER, POTASSIUM,
    KEYS, unit, why, emoji, target,
    grams, lookup, ofDish, ofDay, ratio, indexOf,
    LEVEL_GOOD, LEVEL_OK, LEVEL_LOW,
    level, levelText, levelColor, levelSoft,
    fixAdvice, balanceScore, scoreWord, gaps, fmt,
    foodCount: FOOD.size
  };
})(typeof window !== 'undefined' ? window : globalThis);

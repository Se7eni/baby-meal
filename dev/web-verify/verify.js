/**
 * 验证脚本：在 Node 里直接跑 JS 版算法，检查
 *   1. 全部菜品营养能否换算
 *   2. 同一天同一月龄重复生成结果是否一致
 *   3. 30 天菜单的均衡分 / 各项达标率 / 重复率 / 月龄越界
 * 用 android-dev 里的 node 直接执行，不依赖浏览器。
 */
const fs = require('fs');
const path = require('path');

const ROOT = path.resolve(__dirname, '..', '..', 'docs');  // 站点根目录：docs/
const dishesRaw = JSON.parse(fs.readFileSync(path.join(ROOT, 'data/dishes.json'), 'utf8'));
const dishes = Array.isArray(dishesRaw) ? dishesRaw : dishesRaw.dishes;

// 挂载到 globalThis，模拟浏览器 window
require(path.join(ROOT, 'js/nutrition.js'));
require(path.join(ROOT, 'js/menu.js'));

const N = globalThis.Nutrition;
const M = globalThis.MenuEngine;

function byId(id) { return dishes.find(d => d.id === id) || null; }
M.setDishes(dishes);

const KEYS = N.KEYS;
const VD = N.VIT_D;

function run(label, age, days, seed) {
  const history = [];
  let totalScore = 0;
  const keyCount = {};
  const idCount = {};
  let ageViolation = 0;

  for (let i = 0; i < days; i++) {
    const date = `D${seed}-${i}`;
    const day = M.generate(date, age, history.slice(-30), seed);
    const score = day.score();
    totalScore += score;

    const intake = day.intake();
    for (const k of KEYS) {
      if (N.ratio(intake, k, age) >= 0.60) keyCount[k] = (keyCount[k] || 0) + 1;
    }
    for (const d of day.allDishes()) {
      idCount[d.id] = (idCount[d.id] || 0) + 1;
      if (!d.age.includes(age)) ageViolation++;
      history.push(d.id);
    }
  }

  const avg = (totalScore / days).toFixed(1);
  const rates = KEYS.map(k => ({
    k,
    r: Math.round((keyCount[k] || 0) / days * 100)
  })).sort((a, b) => a.r - b.r);

  const vdRate = rates.find(x => x.k === VD).r;
  const others = rates.filter(x => x.k !== VD);
  const dup = Object.values(idCount).filter(v => v > 1).length;
  const totalAppear = Object.values(idCount).reduce((a, b) => a + b, 0);
  const dupAppear = Object.values(idCount).filter(v => v > 1).reduce((a, b) => a + b, 0);

  console.log(`\n=== ${label} (月龄 ${age}, ${days} 天) ===`);
  console.log(`均衡分平均: ${avg}`);
  console.log(`其余 11 项最低: ${others[0].k} ${others[0].r}%`);
  console.log(`维生素D: ${vdRate}%  (门槛 45%)`);
  console.log(`用到的菜品种类: ${Object.keys(idCount).length} / ${dishes.length}`);
  console.log(`重复率（出现过 2 次以上的菜次 / 总上菜次数）: ${(dupAppear / totalAppear * 100).toFixed(1)}% (${dupAppear}/${totalAppear})`);
  console.log(`月龄越界: ${ageViolation}`);

  const fail = [];
  if (others[0].r < 70) fail.push(`其余项最低 ${others[0].k} ${others[0].r}% < 70%`);
  if (vdRate < 45) fail.push(`维D ${vdRate}% < 45%`);
  if (ageViolation > 0) fail.push(`月龄越界 ${ageViolation}`);
  return { avg: parseFloat(avg), low: others[0], vdRate, ageViolation, fail };
}

// 0. 各月龄菜品池
console.log('=== 各月龄可用菜品池 ===');
for (const age of [1, 2, 3]) {
  const pool = dishes.filter(d => d.age.includes(age));
  const byCat = {};
  for (const d of pool) byCat[d.cat] = (byCat[d.cat] || 0) + 1;
  console.log(`月龄 ${age}: ${pool.length} 道  ${JSON.stringify(byCat)}`);
}

// 1. 营养换算完整性
console.log('=== 营养换算完整性 ===');
let broken = 0;
for (const d of dishes) {
  const nut = N.ofDish(d);
  const sum = nut.reduce((a, b) => a + b, 0);
  if (sum <= 0) { console.log('无营养贡献:', d.id, d.name); broken++; }
}
console.log(`${dishes.length} 道菜中无法换算的: ${broken}`);
console.log(`食材营养表条目: ${N.foodCount}`);

// 2. 同一天幂等性
console.log('\n=== 同一天重复生成一致性 ===');
const a1 = M.generate('2026-10-21', 1, [], 0);
const a2 = M.generate('2026-10-21', 1, [], 0);
const p1 = M.pack(a1), p2 = M.pack(a2);
console.log(`两次生成结果相同: ${p1 === p2}`);
console.log(`菜单: ${p1}`);
if (p1 !== p2) console.log('!! 幂等性失败');

// 3. 各月龄 30 天
const results = [];
results.push(run('月龄1', 1, 30, 11));
results.push(run('月龄2', 2, 30, 22));
results.push(run('月龄3', 3, 30, 33));

console.log('\n=== 汇总 ===');
let allFail = [];
results.forEach((r, i) => {
  if (r.fail.length) allFail.push(`月龄${i + 1}: ` + r.fail.join('; '));
});
if (broken > 0) allFail.push(`营养换算失败 ${broken} 道`);
if (p1 !== p2) allFail.push('幂等性失败');
if (allFail.length === 0) console.log('全部通过');
else allFail.forEach(x => console.log('FAIL: ' + x));

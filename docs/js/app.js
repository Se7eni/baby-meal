/**
 * 宝宝吃什么 · 网页版主程序
 *
 * 页面：今日菜单 / 菜单大全 / 营养报告 / 菜品详情
 * 存储：localStorage（年龄设置、每日菜单缓存、历史记录）
 */
(function () {
  'use strict';

  const N = window.Nutrition;
  const M = window.MenuEngine;

  const LS = {
    age: 'bm.age',
    day: 'bm.day.',      // bm.day.<dateKey>
    history: 'bm.history',
    goal: 'bm.goal'
  };

  let DISHES = [];
  const DISH_MAP = new Map();
  let state = {
    age: 1,
    tab: 'today',
    day: null,
    listCat: 'all',
    listQuery: '',
    listOnlyAge: true,
    detailId: null
  };

  /* ---------------- 工具 ---------------- */

  const $ = sel => document.querySelector(sel);

  function el(tag, cls, html) {
    const e = document.createElement(tag);
    if (cls) e.className = cls;
    if (html !== undefined) e.innerHTML = html;
    return e;
  }

  function esc(s) {
    return String(s == null ? '' : s)
      .replace(/&/g, '&amp;').replace(/</g, '&lt;')
      .replace(/>/g, '&gt;').replace(/"/g, '&quot;');
  }

  function dateKey(d) {
    d = d || new Date();
    return d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0')
      + '-' + String(d.getDate()).padStart(2, '0');
  }

  function dateLabel() {
    const d = new Date();
    const w = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'][d.getDay()];
    return (d.getMonth() + 1) + ' 月 ' + d.getDate() + ' 日 · ' + w;
  }

  function greet() {
    const h = new Date().getHours();
    if (h < 5) return ['夜深了', '🌙'];
    if (h < 11) return ['早上好', '🌅'];
    if (h < 14) return ['中午好', '☀️'];
    if (h < 18) return ['下午好', '🌤'];
    if (h < 22) return ['晚上好', '🌆'];
    return ['夜深了', '🌙'];
  }

  /** 安卓版用圆形水波纹，网页版用暖色渐变块，风格一致 */
  const CAT_STYLE = {
    staple: { c: '#F5B942', bg: '#FFF6E0', softBg: 'linear-gradient(135deg,#FFF3D6,#FFF9EC)' },
    meat: { c: '#FF8A4C', bg: '#FFF0E6', softBg: 'linear-gradient(135deg,#FFEBDD,#FFF7F1)' },
    veg: { c: '#7FB069', bg: '#EDF5E8', softBg: 'linear-gradient(135deg,#E9F4E1,#F6FAF2)' },
    soup: { c: '#6FA8DC', bg: '#EDF4FB', softBg: 'linear-gradient(135deg,#E8F2FB,#F5F9FD)' },
    snack: { c: '#E88C9A', bg: '#FDEDF0', softBg: 'linear-gradient(135deg,#FCE6EA,#FEF6F7)' }
  };

  function catStyle(cat) {
    return CAT_STYLE[cat] || CAT_STYLE.snack;
  }

  const IMG = id => 'img/dishes/' + id + '.webp';

  /* ---------------- 存储 ---------------- */

  function readJSON(key, fallback) {
    try {
      const v = localStorage.getItem(key);
      return v ? JSON.parse(v) : fallback;
    } catch (e) {
      return fallback;
    }
  }

  function writeJSON(key, val) {
    try {
      localStorage.setItem(key, JSON.stringify(val));
    } catch (e) {
      /* 隐私模式下写入失败，忽略即可，不影响本次使用 */
    }
  }

  function history() {
    return readJSON(LS.history, []);
  }

  /** 记录当天吃过的菜，供后面几天降权 */
  function commitHistory(day) {
    const list = history();
    const ids = day.allDishes().map(d => d.id);
    writeJSON(LS.history, list.concat(ids).slice(-60));
  }

  /* ---------------- 菜单生成 ---------------- */

  function loadDay() {
    const k = dateKey();
    const saved = readJSON(LS.day + k, null);
    if (saved && saved.age === state.age && saved.packed) {
      return M.unpack(k, state.age, saved.packed, id => DISH_MAP.get(id));
    }
    const day = M.generate(k, state.age, history(), 0);
    saveDay(day);
    commitHistory(day);
    return day;
  }

  function saveDay(day) {
    writeJSON(LS.day + day.dateKey, {
      age: day.age,
      packed: M.pack(day),
      ts: Date.now()
    });
  }

  function swapSlot(index) {
    const day = state.day;
    M.regenerateSlot(day, index, history(), 0);
    saveDay(day);
    commitHistory(day);
    render();
    // 轻微提示，不打断
    const slots = document.querySelectorAll('.meal');
    if (slots[index]) {
      slots[index].animate(
        [{ opacity: 0.35, transform: 'scale(0.985)' }, { opacity: 1, transform: 'none' }],
        { duration: 260, easing: 'ease-out' }
      );
    }
  }

  /* ---------------- 菜品卡 ---------------- */

  function dishCard(dish, size) {
    const s = catStyle(dish.cat);
    const cls = size === 'big' ? 'dish-card big' : 'dish-card';
    return `<div class="${cls}" data-dish="${esc(dish.id)}" role="button" tabindex="0">
      <img class="thumb" src="${IMG(dish.id)}" alt="${esc(dish.name)}" loading="lazy" decoding="async"
        onerror="this.onerror=null;this.style.display='none';this.nextElementSibling.style.display='flex'">
      <div class="thumb-fallback" style="display:none">${esc(dish.emoji || '🍽')}</div>
      <div class="meta">
        <div class="dn">${esc(dish.name)}</div>
        <div class="dc"><span>${esc(M.catName(dish.cat))}</span><span>${esc(dish.emoji || '')}</span></div>
      </div>
    </div>`;
  }

  /* ---------------- 页面：今日菜单 ---------------- */

  function viewToday() {
    const day = state.day;
    const [g, gi] = greet();
    const html = [];

    html.push(`<div class="header">
      <div class="header-greet">
        <span>${g} ${gi}</span>
        <button class="age-pill" data-act="age">🍼 ${M.ageName(state.age)}</button>
      </div>
      <h1>今天宝宝吃什么？</h1>
      <div class="header-sub">${dateLabel()}　${esc(M.ageHint(state.age))}</div>
    </div>`);

    html.push('<div class="page">');

    day.slots.forEach((slot, i) => {
      const n = slot.dishes.length;
      const gridCls = n === 1 ? 'one' : n === 2 ? 'two' : 'three';
      const big = n === 1;
      html.push(`<section class="meal">
        <div class="meal-head">
          <div class="meal-title">
            <span class="em">${slot.emoji()}</span>
            <div>
              <div class="nm">${slot.name()}</div>
              <div class="tm">${slot.time()}</div>
            </div>
          </div>
          <div class="meal-right">
            <span class="kcal">≈ ${slot.kcalTotal()} kcal</span>
            <button class="btn-swap" data-swap="${i}">换一换</button>
          </div>
        </div>
        <div class="dish-grid ${gridCls}">
          ${slot.dishes.map(d => dishCard(d, big ? 'big' : '')).join('')}
        </div>
      </section>`);
    });

    // 营养小结入口
    const intake = day.intake();
    const score = day.score();
    const color = score >= 90 ? '#7FB069' : score >= 75 ? '#F5B942' : '#E06B5C';
    html.push(`<section class="card" data-act="nutrition" role="button" tabindex="0"
        style="display:flex;align-items:center;gap:14px">
      <div style="font-size:34px;font-weight:500;color:${color};min-width:74px;text-align:center;line-height:1"
        >${score}</div>
      <div style="flex:1">
        <div style="font-size:15px;font-weight:500;margin-bottom:2px">今日营养 ${N.scoreWord(score)}</div>
        <div style="font-size:12.5px;color:#8B7565">看看这 12 项补得怎么样 ›</div>
      </div>
    </section>`);

    html.push(`<div class="foot-note">按 12 项营养素缺口自动配菜，同一天打开结果不变<br>
      营养数据为常见食材平均值，仅供参考</div>`);

    html.push('</div>');
    return html.join('');
  }

  /* ---------------- 页面：菜单大全 ---------------- */

  function viewList() {
    const html = [];
    html.push(`<div class="header" style="padding-bottom:14px">
      <h1>菜单大全</h1>
      <div class="header-sub">共 ${DISHES.length} 道适合 1-3 岁宝宝的做法</div>
    </div>`);
    html.push('<div class="page">');
    html.push(`<input class="search" id="q" placeholder="🔍 搜索菜名或食材，如「牛肉」「南瓜」"
      value="${esc(state.listQuery)}">`);

    const cats = [['all', '全部'], ['staple', '主食'], ['meat', '荤菜'],
      ['veg', '素菜'], ['soup', '汤羹'], ['snack', '点心']];
    html.push('<div class="filters">' + cats.map(([k, n]) =>
      `<button class="f-btn ${state.listCat === k ? 'on' : ''}" data-cat="${k}">${n}</button>`
    ).join('') + '</div>');

    const list = filterDishes();
    html.push(`<div class="list-head">
      <span>${list.length} 道菜</span>
      <label class="toggle-age"><input type="checkbox" id="onlyAge"
        ${state.listOnlyAge ? 'checked' : ''}>仅看适合当前月龄</label>
    </div>`);

    if (list.length === 0) {
      html.push('<div class="empty">没有找到匹配的菜<br>换个关键词试试</div>');
    } else {
      html.push('<div class="dish-grid two">' + list.map(d => dishCard(d)).join('') + '</div>');
    }

    html.push('</div>');
    return html.join('');
  }

  function filterDishes() {
    const q = state.listQuery.trim();
    return DISHES.filter(d => {
      if (state.listCat !== 'all' && d.cat !== state.listCat) return false;
      if (state.listOnlyAge && d.age.indexOf(state.age) < 0) return false;
      if (!q) return true;
      if (d.name.indexOf(q) >= 0) return true;
      return d.ingredients.some(ing => String(ing[0]).indexOf(q) >= 0);
    });
  }

  /* ---------------- 页面：营养报告 ---------------- */

  function viewNutrition() {
    const day = state.day;
    const intake = day.intake();
    const age = state.age;
    const score = N.balanceScore(intake, age);
    const color = score >= 90 ? '#7FB069' : score >= 75 ? '#F5B942' : '#E06B5C';

    let good = 0, ok = 0, low = 0;
    for (const k of N.KEYS) {
      const lv = N.level(N.ratio(intake, k, age));
      if (lv === N.LEVEL_GOOD) good++;
      else if (lv === N.LEVEL_OK) ok++;
      else low++;
    }

    const html = [];
    html.push(`<div class="header" style="padding-bottom:12px">
      <button class="btn-swap" data-act="back" style="margin-bottom:10px">‹ 返回</button>
      <h1>今日营养报告</h1>
      <div class="header-sub">${M.ageName(age)} 每日推荐量 · 仅统计菜品贡献，不含奶和水果</div>
    </div>`);

    html.push('<div class="page">');

    html.push(`<div class="score-card">
      <div class="score-num" style="color:${color}">${score}</div>
      <div class="score-info">
        <div class="score-title">${N.scoreWord(score)}　<span style="font-size:12px;color:#8B7565;font-weight:400">均衡分</span></div>
        <div class="score-desc">达标 ${good} 项 · 基本够 ${ok} 项 · 偏少 ${low} 项</div>
      </div>
    </div>`);

    html.push('<section class="card" style="padding:6px 14px">');
    html.push(`<div class="sec-title" style="margin:10px 0 2px">关键营养素
      <span class="cnt">达标线 60%</span></div>`);
    for (const k of N.KEYS) {
      const r = N.ratio(intake, k, age);
      const lv = N.level(r);
      const c = N.levelColor(lv);
      const pct = Math.min(100, r * 100);
      html.push(`<div class="nut-row">
        <div class="nut-ico">${N.emoji(k)}</div>
        <div class="nut-body">
          <div class="nut-top">
            <span class="nut-name">${k}</span>
            <span class="nut-val">${N.fmt(intake[N.indexOf(k)])} / ${N.fmt(N.target(k, age))} ${N.unit(k)}</span>
          </div>
          <div class="bar"><i style="width:${pct}%;background:${c}"></i></div>
        </div>
        <span class="nut-right" style="color:${c};background:${N.levelSoft(lv)}">${N.levelText(lv)}</span>
      </div>`);
    }
    html.push('</section>');

    // 缺口补救
    const gaps = N.gaps(intake, age, 3);
    if (gaps.length) {
      html.push(`<section class="card" style="background:#FFFDF8">
        <div class="sec-title">💡 今天这样补上</div>
        <div style="font-size:12.5px;color:#8B7565;margin-bottom:10px">
          缺口从大到小排列，可以点「换一换」把对应的一餐调整一下</div>`);
      for (const k of gaps) {
        html.push(`<div style="margin-bottom:10px">
          <div style="font-size:14px;font-weight:500;margin-bottom:3px">${N.emoji(k)} 缺${k}</div>
          <div style="font-size:13px;color:#6B5748;line-height:1.6">${esc(N.fixAdvice(k))}</div>
        </div>`);
      }
      html.push('</section>');
    }

    // 维 D 诚实说明
    html.push(`<section class="card" style="background:#F8FBF7">
      <div class="sec-title">🥛 还差的部分这样补齐</div>
      <div style="font-size:13px;color:#4A5A45;line-height:1.75">
        <p style="margin:0 0 10px">1 到 3 岁每天还需要 400-500ml 奶（母乳、配方奶或全脂牛奶），能把上面约一半的钙和蛋白质缺口补上。</p>
        <p style="margin:0 0 10px">维生素 D 天然只存在于少数食物（三文鱼、蛋黄、干香菇）里，单靠吃菜很难吃够——10μg 这个推荐量本身就是按「日照 + 补充剂」总量给的。所以这一项偏低是正常的，请遵医嘱每天补 400IU 滴剂。</p>
        <p style="margin:0">每周吃 2-3 次海带或紫菜，碘基本就够了。</p>
      </div>
    </section>`);

    html.push(`<div class="foot-note">营养数据参考《中国食物成分表》，为常见品种平均值，用于判断「够不够」的量级。<br>
      每个宝宝食量不同，本报告不构成医学或营养处方建议。</div>`);

    html.push('</div>');
    return html.join('');
  }

  /* ---------------- 页面：菜品详情 ---------------- */

  function viewDetail() {
    const d = DISH_MAP.get(state.detailId);
    if (!d) return '<div class="empty">菜品不存在</div>';
    const s = catStyle(d.cat);
    const html = [];

    html.push('<div class="page">');
    html.push(`<div class="hero">
      <img src="${IMG(d.id)}" alt="${esc(d.name)}">
      <button class="hero-back" data-act="back">‹</button>
    </div>`);

    html.push(`<h1 class="detail-title">${esc(d.name)}</h1>`);
    // 只显示最小适用月龄，三个都列出来太啰嗦
    const minAge = Math.min.apply(null, d.age);
    const ageTag = minAge >= 3 ? '2-3岁' : M.ageName(minAge) + '起';
    html.push(`<div class="detail-chips">
      <span class="chip yellow">${esc(d.emoji || '')} ${esc(M.catName(d.cat))}</span>
      <span class="chip gray">${ageTag}</span>
      ${d.kcal ? `<span class="chip gray">${esc(d.kcal)}</span>` : ''}
      ${(d.tags || []).map(t => `<span class="chip green">${esc(t)}</span>`).join('')}
    </div>`);

    // 食材明细
    html.push(`<section class="sec">
      <div class="sec-title">🧺 食材明细<span class="cnt">${d.ingredients.length} 种食材</span></div>`);
    for (const ing of d.ingredients) {
      html.push(`<div class="ing-row">
        <span class="dot" style="background:${s.c}"></span>
        <span>${esc(ing[0])}</span>
        <span class="amt">${esc(ing[1] || '')}${esc(ing[2] || '')}</span>
      </div>`);
    }
    html.push('</section>');

    // 做法
    html.push(`<section class="sec">
      <div class="sec-title">👩‍🍳 做法步骤<span class="cnt">${d.steps.length} 步</span></div>`);
    d.steps.forEach((st, i) => {
      html.push(`<div class="step">
        <span class="no">${i + 1}</span>
        <span class="tx">${esc(st)}</span>
      </div>`);
    });
    html.push('</section>');

    // 这道菜补什么
    html.push(`<section class="sec">
      <div class="sec-title">📊 这道菜补什么<span class="cnt">占每日推荐</span></div>`);
    const nut = N.ofDish(d);
    const rows = N.KEYS.map((k, i) => ({ k, v: nut[i], r: nut[i] / N.target(k, 1.5) }))
      .filter(x => x.v > 0)
      .sort((a, b) => b.r - a.r)
      .slice(0, 6);
    for (const row of rows) {
      const pct = Math.min(100, row.r * 100);
      const c = catStyle(d.cat).c;
      html.push(`<div class="nut-row" style="padding:8px 0">
        <div class="nut-ico">${N.emoji(row.k)}</div>
        <div class="nut-body">
          <div class="nut-top">
            <span class="nut-name" style="font-size:13px">${row.k}</span>
            <span class="nut-val">${N.fmt(row.v)}${N.unit(row.k)}</span>
          </div>
          <div class="bar" style="height:5px"><i style="width:${pct}%;background:${c}"></i></div>
        </div>
        <span style="font-size:11px;color:#B5A395;min-width:38px;text-align:right">${Math.round(row.r * 100)}%</span>
      </div>`);
    }
    html.push('</section>');

    // 营养说明
    if (d.nutrition) {
      html.push(`<section class="sec" style="background:#F8FBF7">
        <div class="sec-title">🌱 补充的营养</div>
        <div class="nut-text">${esc(d.nutrition)}</div>
      </section>`);
    }

    // 小贴士
    if (d.tip) {
      html.push(`<section class="sec">
        <div class="sec-title">💛 妈妈小贴士</div>
        <div class="tip-box">${esc(d.tip)}</div>
      </section>`);
    }

    html.push(`<div class="foot-note">菜量与食材用量基于 1-3 岁宝宝食量估算，请按宝宝实际食量调整。<br>
      首次尝试新食材请少量并观察 3 天。</div>`);
    html.push('</div>');
    return html.join('');
  }

  /* ---------------- 路由与渲染 ---------------- */

  function render() {
    const app = $('#app');
    let body = '';
    if (state.tab === 'today') body = viewToday();
    else if (state.tab === 'list') body = viewList();
    else if (state.tab === 'nutrition') body = viewNutrition();
    else if (state.tab === 'detail') body = viewDetail();

    const showTab = state.tab !== 'detail';
    app.innerHTML = body + (showTab ? tabbar() : '');

    if (state.tab === 'detail') window.scrollTo(0, 0);

    // iPhone SEO 之外的滚动位置：切 tab 回到顶部
    if (render._lastTab !== state.tab) {
      window.scrollTo(0, 0);
      render._lastTab = state.tab;
    }
  }

  /**
   * Tab 图标用内联 SVG，不用 emoji —— emoji 在不同系统上字形差异太大，
   * 白色背景上也不统一。描边图标配上 currentColor，选中态自动变橙。
   */
  const TAB_ICONS = {
    today: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" '
      + 'stroke-linecap="round" stroke-linejoin="round"><path d="M4 10.5 12 4l8 6.5"/>'
      + '<path d="M6 10v9h12v-9"/><path d="M9.5 15h5"/></svg>',
    list: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" '
      + 'stroke-linecap="round" stroke-linejoin="round"><path d="M4 5.5h7v7H4z"/>'
      + '<path d="M13 5.5h7v7h-7z"/><path d="M4 14.5h7v4H4z"/><path d="M13 14.5h7v4h-7z"/></svg>',
    nutrition: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" '
      + 'stroke-linecap="round" stroke-linejoin="round"><path d="M5 19V9"/>'
      + '<path d="M12 19V5"/><path d="M19 19v-6"/></svg>'
  };

  function tabbar() {
    const tabs = [
      ['today', '今日'],
      ['list', '大全'],
      ['nutrition', '营养']
    ];
    return `<nav class="tabbar">${tabs.map(([k, name]) =>
      `<button class="tab ${state.tab === k ? 'on' : ''}" data-tab="${k}">
        <span class="ico">${TAB_ICONS[k]}</span><span>${name}</span>
      </button>`).join('')}</nav>`;
  }

  function goDetail(id) {
    state.detailId = id;
    state.tab = 'detail';
    render();
  }

  function goBack() {
    state.tab = 'today';
    render();
  }

  /* ---------------- 事件 ---------------- */

  document.addEventListener('click', e => {
    const t = e.target;

    const tabBtn = t.closest('[data-tab]');
    if (tabBtn) {
      state.tab = tabBtn.dataset.tab;
      render();
      return;
    }

    const dish = t.closest('[data-dish]');
    if (dish) {
      goDetail(dish.dataset.dish);
      return;
    }

    const swap = t.closest('[data-swap]');
    if (swap) {
      swapSlot(parseInt(swap.dataset.swap, 10));
      return;
    }

    const catBtn = t.closest('[data-cat]');
    if (catBtn) {
      state.listCat = catBtn.dataset.cat;
      render();
      return;
    }

    const act = t.closest('[data-act]');
    if (act) {
      const a = act.dataset.act;
      if (a === 'back') goBack();
      else if (a === 'nutrition') { state.tab = 'nutrition'; render(); }
      else if (a === 'age') cycleAge();
      return;
    }
  });

  document.addEventListener('input', e => {
    if (e.target.id === 'q') {
      state.listQuery = e.target.value;
      // 只重画列表区，保住输入焦点
      const list = filterDishes();
      const page = document.querySelector('.page');
      const head = page.querySelector('.list-head');
      const grid = page.querySelector('.dish-grid');
      const emptyEl = page.querySelector('.empty');
      if (head) head.firstElementChild.textContent = list.length + ' 道菜';
      if (grid) grid.remove();
      if (emptyEl) emptyEl.remove();
      if (list.length === 0) {
        page.appendChild(el('div', 'empty', '没有找到匹配的菜<br>换个关键词试试'));
      } else {
        const wrap = el('div', 'dish-grid two', list.map(d => dishCard(d)).join(''));
        page.appendChild(wrap);
      }
    }
  });

  document.addEventListener('change', e => {
    if (e.target.id === 'onlyAge') {
      state.listOnlyAge = e.target.checked;
      render();
    }
  });

  function cycleAge() {
    state.age = state.age >= 3 ? 1 : state.age + 1;
    writeJSON(LS.age, state.age);
    state.day = loadDay();
    render();
  }

  /* ---------------- 启动 ---------------- */

  function boot() {
    const savedAge = readJSON(LS.age, 1);
    state.age = [1, 2, 3].indexOf(savedAge) >= 0 ? savedAge : 1;

    fetch('data/dishes.json')
      .then(r => r.json())
      .then(data => {
        DISHES = Array.isArray(data) ? data : data.dishes;
        DISHES.forEach(d => DISH_MAP.set(d.id, d));
        M.setDishes(DISHES);
        state.day = loadDay();
        render();
        document.body.classList.add('ready');
      })
      .catch(err => {
        document.querySelector('#app').innerHTML =
          '<div class="empty">数据加载失败，请刷新页面重试<br><small>' + esc(err.message) + '</small></div>';
      });
  }

  // Service Worker
  if ('serviceWorker' in navigator) {
    window.addEventListener('load', () => {
      navigator.serviceWorker.register('sw.js').catch(() => { });
    });
  }

  boot();
})();

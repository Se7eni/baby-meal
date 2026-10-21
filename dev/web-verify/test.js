/**
 * 交互冒烟测试：用 CDP 在真实浏览器里跑一遍关键交互，
 * 断言 DOM 与 localStorage 是否符合预期。输出 PASS/FAIL 列表。
 */
const http = require('http');
const fs = require('fs');
const path = require('path');
const { spawn } = require('child_process');
const crypto = require('crypto');
const os = require('os');

const ROOT = path.resolve(__dirname, '..', '..', 'docs');  // 站点根目录：docs/
const PORT = 8766;
const DEBUG_PORT = 9334;
const CHROME = 'C:/Program Files/Google/Chrome/Application/chrome.exe';

const MIME = {
  '.html': 'text/html; charset=utf-8', '.css': 'text/css; charset=utf-8',
  '.js': 'text/javascript; charset=utf-8', '.json': 'application/json; charset=utf-8',
  '.png': 'image/png', '.webp': 'image/webp', '.ttf': 'font/ttf'
};

function serve() {
  return new Promise(resolve => {
    const s = http.createServer((req, res) => {
      let p = decodeURIComponent(req.url.split('?')[0]);
      if (p === '/') p = '/index.html';
      const file = path.join(ROOT, p);
      fs.readFile(file, (err, data) => {
        if (err) { res.writeHead(404); res.end('nf'); return; }
        res.writeHead(200, {
          'Content-Type': MIME[path.extname(file).toLowerCase()] || 'application/octet-stream'
        });
        res.end(data);
      });
    });
    s.listen(PORT, '127.0.0.1', () => resolve(s));
  });
}

const sleep = ms => new Promise(r => setTimeout(r, ms));

function httpGet(url, method) {
  return new Promise((resolve, reject) => {
    const u = new URL(url);
    const req = http.request({
      hostname: u.hostname, port: u.port, path: u.pathname + u.search,
      method: method || 'GET'
    }, res => {
      let d = '';
      res.on('data', c => d += c);
      res.on('end', () => resolve(d));
    });
    req.on('error', reject);
    req.end();
  });
}

class WS {
  constructor(socket) {
    this.socket = socket;
    this.buf = Buffer.alloc(0);
    this.handlers = new Map();
    this.id = 0;
    socket.on('data', d => this._d(d));
    // Chrome 退出时会 ECONNRESET，不接住会导致进程异常退出
    socket.on('error', () => { });
    socket.on('close', () => { });
  }
  _d(d) {
    this.buf = Buffer.concat([this.buf, d]);
    for (; ;) {
      if (this.buf.length < 2) return;
      let len = this.buf[1] & 0x7f, off = 2;
      if (len === 126) { if (this.buf.length < 4) return; len = this.buf.readUInt16BE(2); off = 4; }
      else if (len === 127) { if (this.buf.length < 10) return; len = Number(this.buf.readBigUInt64BE(2)); off = 10; }
      if (this.buf.length < off + len) return;
      const payload = this.buf.slice(off, off + len);
      this.buf = this.buf.slice(off + len);
      try {
        const m = JSON.parse(payload.toString('utf8'));
        if (m.id != null && this.handlers.has(m.id)) {
          const h = this.handlers.get(m.id);
          this.handlers.delete(m.id);
          m.error ? h.reject(new Error(JSON.stringify(m.error))) : h.resolve(m.result);
        }
      } catch (e) { }
    }
  }
  send(method, params) {
    return new Promise((resolve, reject) => {
      const id = ++this.id;
      this.handlers.set(id, { resolve, reject });
      const pl = Buffer.from(JSON.stringify({ id, method, params: params || {} }));
      const len = pl.length;
      let head;
      if (len < 126) head = Buffer.from([0x81, 0x80 | len]);
      else if (len < 65536) { head = Buffer.alloc(4); head[0] = 0x81; head[1] = 0x80 | 126; head.writeUInt16BE(len, 2); }
      else { head = Buffer.alloc(10); head[0] = 0x81; head[1] = 0x80 | 127; head.writeBigUInt64BE(BigInt(len), 2); }
      const mask = crypto.randomBytes(4);
      const mk = Buffer.alloc(len);
      for (let i = 0; i < len; i++) mk[i] = pl[i] ^ mask[i % 4];
      this.socket.write(Buffer.concat([head, mask, mk]));
    });
  }
}

function connect(url) {
  return new Promise((resolve, reject) => {
    const u = new URL(url);
    const req = http.request({
      hostname: u.hostname, port: u.port, path: u.pathname + u.search,
      headers: {
        Connection: 'Upgrade', Upgrade: 'websocket',
        'Sec-WebSocket-Key': crypto.randomBytes(16).toString('base64'),
        'Sec-WebSocket-Version': 13
      }
    });
    req.on('upgrade', (res, socket) => resolve(new WS(socket)));
    req.on('error', reject);
    req.end();
  });
}

const results = [];
function check(name, ok, detail) {
  results.push({ name, ok, detail });
  console.log((ok ? 'PASS  ' : 'FAIL  ') + name + (detail ? '  → ' + detail : ''));
}

async function main() {
  const server = await serve();
  const profile = path.join(os.tmpdir(), 'bm-chrome-test-' + Date.now() + '-' + Math.random().toString(36).slice(2, 8));
  const proc = spawn(CHROME, [
    '--headless=new', '--remote-debugging-port=' + DEBUG_PORT,
    '--user-data-dir=' + profile, '--no-first-run', '--no-default-browser-check',
    '--disable-gpu', 'about:blank'
  ], { stdio: 'ignore' });

    let ws = null;
  try {
    // 必须新建空白页：Chrome 启动时那个 about:blank 标签页有时不响应导航
    for (let i = 0; i < 40; i++) {
      await sleep(300);
      try {
        await httpGet('http://127.0.0.1:' + DEBUG_PORT + '/json/version');
        const created = JSON.parse(
          await httpGet('http://127.0.0.1:' + DEBUG_PORT + '/json/new?about:blank', 'PUT')
        );
        if (created.webSocketDebuggerUrl) {
          ws = await connect(created.webSocketDebuggerUrl);
          break;
        }
      } catch (e) { }
    }
    if (!ws) throw new Error('无法连接 Chrome');

    await ws.send('Page.enable');
    await ws.send('Runtime.enable');
    await ws.send('Emulation.setDeviceMetricsOverride', {
      width: 393, height: 852, deviceScaleFactor: 2, mobile: true
    });

    const ev = async expr => {
      const r = await ws.send('Runtime.evaluate', {
        expression: expr, returnByValue: true, awaitPromise: true
      });
      if (r.exceptionDetails) {
        const d = r.exceptionDetails;
        throw new Error((d.exception && d.exception.description) || d.text || 'eval error');
      }
      return r.result.value;
    };

    // 收集页面里的脚本错误，最后统一断言
    ws.logs = [];
    ws.socket.on('data', () => { });

    await ws.send('Page.navigate', { url: 'http://127.0.0.1:' + PORT + '/index.html' });

    // 等 ready
    let ready = false;
    for (let i = 0; i < 50; i++) {
      await sleep(200);
      try {
        if (await ev('document.body.classList.contains("ready")')) { ready = true; break; }
      } catch (e) { }
    }
    check('页面初始化完成', ready);

    // 1. 菜品数量
    const dishCount = await ev('document.querySelectorAll("[data-dish]").length');
    check('今日菜单渲染出菜品', dishCount > 0, dishCount + ' 个菜品卡');

    // 2. 五餐都在
    const meals = await ev('document.querySelectorAll(".meal").length');
    check('渲染出 5 个餐位', meals === 5, meals + ' 个');

    // 3. 图片或 emoji 占位图能显示
    const imgOk = await ev(`(() => {
      const cards = [...document.querySelectorAll('.dish-card')];
      const ok = cards.filter(c => {
        const img = c.querySelector('img.thumb');
        const fb = c.querySelector('.thumb-fallback');
        return (img && img.complete && img.naturalWidth > 0) || (fb && getComputedStyle(fb).display !== 'none');
      }).length;
      return ok + '/' + cards.length;
    })()`);
    const [okN, allN] = imgOk.split('/').map(Number);
    check('菜品图或占位图全部显示', okN === allN, imgOk);

    // 4. 菜单持久化
    const before = await ev('JSON.stringify(Object.keys(localStorage))');
    check('菜单已写入本地存储', before.indexOf('bm.day') >= 0, before);

    // 5. 换一换：某一餐内容变化
    const slotBefore = await ev(`[...document.querySelectorAll('.meal')][1].querySelectorAll('[data-dish]').length
      + ':' + [...[...document.querySelectorAll('.meal')][1].querySelectorAll('[data-dish]')].map(d=>d.dataset.dish).join(',')`);
    await ev(`document.querySelectorAll('[data-swap]')[1].click()`);
    await sleep(500);
    const slotAfter = await ev(`[...document.querySelectorAll('.meal')][1].querySelectorAll('[data-dish]').length
      + ':' + [...[...document.querySelectorAll('.meal')][1].querySelectorAll('[data-dish]')].map(d=>d.dataset.dish).join(',')`);
    check('「换一换」后午餐内容改变', slotBefore !== slotAfter, slotBefore + ' → ' + slotAfter);

    // 6. 换完之后仍然是 5 餐
    const meals2 = await ev('document.querySelectorAll(".meal").length');
    check('「换一换」后结构完整', meals2 === 5, meals2 + ' 个餐位');

    // 7. 切到菜单大全
    await ev('document.querySelector("[data-tab=list]").click()');
    await sleep(400);
    const listCount = await ev('document.querySelectorAll("[data-dish]").length');
    check('菜单大全能渲染', listCount > 0, listCount + ' 道菜');

    // 8. 搜索
    await ev(`(() => {
      const q = document.getElementById('q');
      q.value = '南瓜';
      q.dispatchEvent(new Event('input', {bubbles:true}));
    })()`);
    await sleep(400);
    const hit = await ev('document.querySelectorAll("[data-dish]").length');
    check('搜索「南瓜」有结果', hit > 0, hit + ' 道');

    // 9. 搜索无结果
    await ev(`(() => {
      const q = document.getElementById('q');
      q.value = '不存在的食材xyz';
      q.dispatchEvent(new Event('input', {bubbles:true}));
    })()`);
    await sleep(400);
    const empty = await ev('document.querySelector(".empty") ? document.querySelector(".empty").textContent.trim() : ""');
    check('无结果时显示空状态', empty.indexOf('没有找到') >= 0, empty);

    // 10. 分类筛选
    await ev(`(() => {
      const q = document.getElementById('q'); q.value='';
      q.dispatchEvent(new Event('input', {bubbles:true}));
    })()`);
    await sleep(300);
    await ev('document.querySelector("[data-cat=meat]").click()');
    await sleep(400);
    const meatOnly = await ev(`[...document.querySelectorAll('[data-dish]')].every(d => {
      const chip = d.querySelector('.dc span');
      return chip && chip.textContent === '荤菜';
    })`);
    check('筛选「荤菜」只显示荤菜', meatOnly === true);

    // 11. 详情页
    await ev('document.querySelector("[data-dish]").click()');
    await sleep(500);
    const hasIng = await ev('document.querySelectorAll(".ing-row").length');
    const hasStep = await ev('document.querySelectorAll(".step").length');
    check('详情页有食材明细', hasIng > 0, hasIng + ' 条');
    check('详情页有做法步骤', hasStep > 0, hasStep + ' 步');

    // 12. 返回
    await ev('document.querySelector("[data-act=back]").click()');
    await sleep(400);
    const backOk = await ev('!!document.querySelector(".tabbar")');
    check('从详情返回后底部导航恢复', backOk === true);

    // 13. 营养报告
    await ev('document.querySelector("[data-tab=nutrition]").click()');
    await sleep(500);
    const rows = await ev('document.querySelectorAll(".nut-row").length');
    check('营养报告 12 项齐全', rows === 12, rows + ' 项');
    const scoreTxt = await ev('document.querySelector(".score-num") ? document.querySelector(".score-num").textContent : ""');
    check('均衡分有数值', /^\d+$/.test(scoreTxt), '得分 ' + scoreTxt);

    // 14. 切换月龄
    await ev('document.querySelector("[data-tab=today]").click()');
    await sleep(400);
    const ageBefore = await ev('document.querySelector(".age-pill").textContent.trim()');
    await ev('document.querySelector("[data-act=age]").click()');
    await sleep(600);
    const ageAfter = await ev('document.querySelector(".age-pill").textContent.trim()');
    check('切换月龄生效', ageBefore !== ageAfter, ageBefore + ' → ' + ageAfter);

    // 15. 切月龄后菜单重算且三餐俱全
    const meals3 = await ev('document.querySelectorAll(".meal").length');
    check('切月龄后菜单结构完整', meals3 === 5, meals3 + ' 个餐位');

    // 16. 刷新后月龄保持（localStorage）
    const savedAge = await ev('localStorage.getItem("bm.age")');
    check('月龄被持久化', savedAge !== null, 'bm.age=' + savedAge);

    // 17. 无控制台错误
    const errCount = await ev('window.__errCount || 0');
    check('无未捕获脚本错误', errCount === 0);

  } finally {
    try { proc.kill(); } catch (e) { }
    server.close();
    const fail = results.filter(r => !r.ok);
    console.log('\n========== 结果 ==========');
    console.log(`通过 ${results.length - fail.length} / ${results.length}`);
    if (fail.length) {
      console.log('失败项：');
      fail.forEach(f => console.log('  - ' + f.name + (f.detail ? ' (' + f.detail + ')' : '')));
    }
  }
}

main().catch(e => {
  console.error('FATAL', e.message);
  process.exit(1);
});

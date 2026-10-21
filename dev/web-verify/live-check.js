/**
 * 线上验收：直接打 GitHub Pages 的真实 URL，起一个全新 Chrome 走一遍
 *
 *   1. 首屏能渲染
 *   2. 资源是否 404（favicon / icon / sw / manifest / 字体 / 图片）
 *   3. Service Worker 在 HTTPS 下能注册并激活
 *   4. 缓存写入数量（首次只会预缓存 CORE_FILES，图片是访问后才进）
 *   5. 断网后重新打开仍能渲染
 *   6. 六个静态资源直连状态码
 *
 * 用法：node live-check.js [url]
 */
const http = require('http');
const https = require('https');
const fs = require('fs');
const path = require('path');
const { spawn } = require('child_process');
const os = require('os');

const BASE = process.argv[2] || 'https://se7eni.github.io/baby-meal/';
const DEBUG_PORT = 9339;
const CHROME = 'C:/Program Files/Google/Chrome/Application/chrome.exe';

const sleep = ms => new Promise(r => setTimeout(r, ms));

const PROXY = process.env.HTTPS_PROXY || process.env.https_proxy || '';

function fetchHead(url) {
  return new Promise(resolve => {
    const u = new URL(url);
    const mod = u.protocol === 'https:' ? https : http;
    // 走 CONNECT 代理不是必须的：github.io 直连一般可达，失败再报错
    const req = mod.request({
      hostname: u.hostname, port: u.port || (u.protocol === 'https:' ? 443 : 80),
      path: u.pathname + u.search, method: 'GET',
      headers: { 'User-Agent': 'Mozilla/5.0', 'Accept': '*/*' }
    }, res => {
      let n = 0;
      res.on('data', c => { n += c.length; });
      res.on('end', () => resolve({ status: res.statusCode, size: n, type: res.headers['content-type'] }));
    });
    req.on('error', e => resolve({ status: 0, size: 0, type: e.message }));
    req.setTimeout(30000, () => { req.destroy(); resolve({ status: 0, size: 0, type: 'timeout' }); });
    req.end();
  });
}

function httpReq(url, method) {
  return new Promise((resolve, reject) => {
    const u = new URL(url);
    const r = http.request({
      hostname: u.hostname, port: u.port, path: u.pathname + u.search,
      method: method || 'GET'
    }, res => {
      let d = '';
      res.on('data', c => d += c);
      res.on('end', () => resolve(d));
    });
    r.on('error', reject);
    r.end();
  });
}

class WS {
  constructor(socket) {
    this.socket = socket; this.buf = Buffer.alloc(0);
    this.handlers = new Map(); this.id = 0;
    socket.on('data', d => this._d(d));
    socket.on('error', () => { }); socket.on('close', () => { });
  }
  _d(d) {
    this.buf = Buffer.concat([this.buf, d]);
    while (this.buf.length >= 2) {
      const b0 = this.buf[0], b1 = this.buf[1];
      const fin = (b0 & 0x80) !== 0, op = b0 & 0x0f;
      let len = b1 & 0x7f, off = 2;
      if (len === 126) { if (this.buf.length < 4) return; len = this.buf.readUInt16BE(2); off = 4; }
      else if (len === 127) { if (this.buf.length < 10) return; len = Number(this.buf.readBigUInt64BE(2)); off = 10; }
      const masked = (b1 & 0x80) !== 0;
      const mlen = masked ? 4 : 0;
      if (this.buf.length < off + mlen + len) return;
      let payload = this.buf.slice(off + mlen, off + mlen + len);
      if (masked) {
        const mask = this.buf.slice(off, off + 4);
        payload = Buffer.from(payload.map((v, i) => v ^ mask[i % 4]));
      }
      this.buf = this.buf.slice(off + mlen + len);
      if (op === 1 || op === 0) {
        const txt = payload.toString('utf8');
        this._msg += txt;
        if (fin) { try { this._onMsg(JSON.parse(this._msg)); } catch (e) { } this._msg = ''; }
      }
    }
  }
  _onMsg(m) {
    if (m.id != null && this.handlers.has(m.id)) {
      const h = this.handlers.get(m.id); this.handlers.delete(m.id); h(m);
    } else if (m.method && this.evHandlers.has(m.method)) {
      this.evHandlers.get(m.method).forEach(f => f(m.params));
    }
  }
  send(method, params) {
    const id = ++this.id;
    const payload = JSON.stringify({ id, method, params: params || {} });
    const data = Buffer.from(payload, 'utf8');
    const mask = Buffer.from([1, 2, 3, 4]);
    let head;
    if (data.length < 126) {
      head = Buffer.from([0x81, 0x80 | data.length]);
    } else if (data.length < 65536) {
      head = Buffer.alloc(4); head[0] = 0x81; head[1] = 0x80 | 126; head.writeUInt16BE(data.length, 2);
    } else {
      head = Buffer.alloc(10); head[0] = 0x81; head[1] = 0x80 | 127; head.writeBigUInt64BE(BigInt(data.length), 2);
    }
    const masked = Buffer.from(data.map((v, i) => v ^ mask[i % 4]));
    this.socket.write(Buffer.concat([head, mask, masked]));
    return new Promise(res => this.handlers.set(id, m => {
      if (m.error) res({ error: m.error }); else res(m.result || {});
    }));
  }
}

function connect(url) {
  return new Promise((resolve, reject) => {
    const u = new URL(url);
    const key = Buffer.from(Array.from({ length: 16 }, () => Math.floor(Math.random() * 256))).toString('base64');
    const req = http.request({
      hostname: u.hostname, port: u.port, path: u.pathname,
      headers: {
        'Connection': 'Upgrade', 'Upgrade': 'websocket',
        'Sec-WebSocket-Key': key, 'Sec-WebSocket-Version': '13'
      }
    });
    req.on('upgrade', (res, socket) => {
      const ws = new WS(socket); ws._msg = ''; ws.evHandlers = new Map();
      const orig = ws._onMsg.bind(ws);
      ws._onMsg = m => { orig(m); };
      resolve(ws);
    });
    req.on('error', reject);
    req.end();
  });
}

const results = [];
function check(name, ok, detail) {
  results.push(ok);
  console.log((ok ? 'PASS  ' : 'FAIL  ') + name + (detail !== undefined ? '  → ' + detail : ''));
}

(async () => {
  console.log('目标：' + BASE + '\n');

  // ---- 静态资源直连 ----
  const assets = [
    'index.html', 'manifest.json', 'sw.js', 'css/app.css',
    'js/app.js', 'js/menu.js', 'js/nutrition.js', 'data/dishes.json',
    'fonts/deng.ttf', 'fonts/dengb.ttf',
    'icons/apple-touch-icon-180.png', 'icons/icon-192.png',
    'img/dishes/k01.webp', 'img/dishes/v23.webp'
  ];
  const bad = [];
  for (const a of assets) {
    const r = await fetchHead(BASE + a);
    if (r.status !== 200 || r.size === 0) bad.push(a + ' ' + r.status + ' ' + r.size);
  }
  check('14 个关键资源全部 200', bad.length === 0, bad.length ? bad.join('; ') : 'ok');

  // ---- 无头浏览器 ----
  const profile = path.join(os.tmpdir(), 'bm-chrome-live');
  try { fs.rmSync(profile, { recursive: true, force: true }); } catch (e) { }

  const proc = spawn(CHROME, [
    '--headless=new', '--remote-debugging-port=' + DEBUG_PORT,
    '--user-data-dir=' + profile, '--no-first-run', '--no-default-browser-check',
    '--disable-gpu',
    '--proxy-server=' + (PROXY || 'direct://'),
    'about:blank'
  ], { stdio: 'ignore' });

  let ws = null;
  try {
    for (let i = 0; i < 40; i++) {
      await sleep(300);
      try {
        await httpReq('http://127.0.0.1:' + DEBUG_PORT + '/json/version');
        const c = JSON.parse(await httpReq('http://127.0.0.1:' + DEBUG_PORT + '/json/new?about:blank', 'PUT'));
        if (c.webSocketDebuggerUrl) { ws = await connect(c.webSocketDebuggerUrl); break; }
      } catch (e) { }
    }
    if (!ws) throw new Error('无法连接 Chrome');

    await ws.send('Page.enable');
    await ws.send('Runtime.enable');
    await ws.send('Network.enable');
    await ws.send('Emulation.setDeviceMetricsOverride', { width: 393, height: 852, deviceScaleFactor: 2, mobile: true });
    await ws.send('Emulation.setUserAgentOverride', {
      userAgent: 'Mozilla/5.0 (iPhone; CPU iPhone OS 16_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.5 Mobile/15E148 Safari/604.1'
    });

    const errors = [];
    ws.evHandlers.set('Runtime.exceptionThrown', []);
    ws.evHandlers.set('Log.entryAdded', []);
    await ws.send('Log.enable');

    const ev = async expr => {
      const r = await ws.send('Runtime.evaluate', { expression: expr, returnByValue: true, awaitPromise: true });
      if (r.error) throw new Error(JSON.stringify(r.error));
      if (r.exceptionDetails) throw new Error((r.exceptionDetails.exception || {}).description || r.exceptionDetails.text);
      return r.result.value;
    };

    await ws.send('Page.navigate', { url: BASE });

    let ready = false;
    for (let i = 0; i < 60; i++) {
      await sleep(250);
      try { if (await ev('document.body.classList.contains("ready")')) { ready = true; break; } } catch (e) { } 
    }
    check('首屏渲染完成', ready);

    const cards = await ev('document.querySelectorAll("[data-dish]").length');
    check('今日菜单有菜品卡', cards >= 5, cards + ' 个');

    // 图片是 loading=lazy 的，首屏以下尚未进入视口，必须等 decode 完成后再统计
    const imgsOk = await ev('(async () => {' +
      'const a = [...document.images];' +
      'await Promise.all(a.map(i => i.decode().catch(() => {})));' +
      'const ok = a.filter(i => i.complete && i.naturalWidth > 0).length;' +
      'const pending = a.filter(i => !i.complete).length;' +
      'return ok + "/" + a.length + (pending ? " (pending " + pending + ")" : "");' +
      '})()');
    const [g, t] = imgsOk.split('/').map(x => parseInt(x, 10));
    check('菜品图全部加载', g === t && t > 0, imgsOk);

    // favicon 是否被抓取成功
    const fav = await ev(`(async () => {
      const r = await fetch('icons/icon-192.png', { method: 'HEAD' });
      return r.status;
    })()`);
    check('图标可访问', fav === 200, 'status ' + fav);

    // SW 在 HTTPS 下注册
    let swState = 'none';
    for (let i = 0; i < 50; i++) {
      await sleep(300);
      swState = await ev(`(async () => {
        if (!('serviceWorker' in navigator)) return 'unsupported';
        const reg = await navigator.serviceWorker.getRegistration();
        if (!reg) return 'none';
        if (reg.active) return 'active:' + reg.active.state;
        if (reg.installing) return 'installing';
        if (reg.waiting) return 'waiting';
        return 'registered';
      })()`);
      if (swState.indexOf('active') === 0) break;
    }
    check('Service Worker 已激活（HTTPS）', swState.indexOf('active') === 0, swState);

    const cacheInfo = await ev(`(async () => {
      const names = await caches.keys();
      const out = {};
      for (const n of names) { const c = await caches.open(n); out[n] = (await c.keys()).length; }
      return JSON.stringify(out);
    })()`);
    const total = Object.values(JSON.parse(cacheInfo)).reduce((a, b) => a + b, 0);
    check('核心资源已预缓存', total >= 10, cacheInfo);

    // 切到菜单大全，让部分图片进缓存
    await ev(`(document.querySelector('[data-tab="list"]')||{}).click?.(), 1`);
    await sleep(2500);
    const imgCached = await ev(`(async () => {
      const names = await caches.keys();
      let n = 0;
      for (const nm of names) {
        const c = await caches.open(nm);
        const ks = await c.keys();
        n += ks.filter(r => /img\\/dishes/.test(r.url)).length;
      }
      return n;
    })()`);
    check('浏览过的菜品图已进缓存', imgCached > 5, imgCached + ' 张');

    // 断网
    await ws.send('Network.emulateNetworkConditions', {
      offline: true, latency: 0, downloadThroughput: 0, uploadThroughput: 0
    });
    await ws.send('Page.navigate', { url: BASE });
    let offReady = false;
    for (let i = 0; i < 60; i++) {
      await sleep(250);
      try { if (await ev('document.body.classList.contains("ready")')) { offReady = true; break; } } catch (e) { }
    }
    check('断网后重新打开仍能渲染', offReady);
    const offCards = await ev('document.querySelectorAll("[data-dish]").length').catch(() => 0);
    check('断网状态菜单有内容', offCards >= 5, offCards + ' 个');
    const offImgs = await ev('(async () => {' +
      'const a = [...document.images];' +
      'await Promise.all(a.map(i => i.decode().catch(() => {})));' +
      'const ok = a.filter(i => i.complete && i.naturalWidth > 0).length;' +
      'return ok + "/" + a.length;' +
      '})()').catch(() => '0/0');
    const [og, ot] = offImgs.split('/').map(Number);
    check('断网状态图片可显示', og > 0 && og === ot, offImgs);

    await ws.send('Network.emulateNetworkConditions', {
      offline: false, latency: 0, downloadThroughput: -1, uploadThroughput: -1
    });

  } catch (e) {
    check('执行未抛错', false, e.message);
  } finally {
    try { proc.kill(); } catch (e) { }
    try { fs.rmSync(profile, { recursive: true, force: true }); } catch (e) { }
    const pass = results.filter(Boolean).length;
    console.log('\n========== 结果 ==========');
    console.log('通过 ' + pass + ' / ' + results.length);
    process.exit(0);
  }
})();

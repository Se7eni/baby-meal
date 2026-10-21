/**
 * 离线能力验证：
 *   1. Service Worker 是否注册成功并进入 activated
 *   2. 核心文件是否进了缓存
 *   3. 断网后重新打开页面是否仍能渲染出菜单
 */
const http = require('http');
const fs = require('fs');
const path = require('path');
const { spawn } = require('child_process');
const crypto = require('crypto');
const os = require('os');

const ROOT = __dirname;
const PORT = 8767;
const DEBUG_PORT = 9335;
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
          'Content-Type': MIME[path.extname(file).toLowerCase()] || 'application/octet-stream',
          'Service-Worker-Allowed': '/'
        });
        res.end(data);
      });
    });
    s.listen(PORT, '127.0.0.1', () => resolve(s));
  });
}

const sleep = ms => new Promise(r => setTimeout(r, ms));

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
    for (; ;) {
      if (this.buf.length < 2) return;
      let len = this.buf[1] & 0x7f, off = 2;
      if (len === 126) { if (this.buf.length < 4) return; len = this.buf.readUInt16BE(2); off = 4; }
      else if (len === 127) { if (this.buf.length < 10) return; len = Number(this.buf.readBigUInt64BE(2)); off = 10; }
      if (this.buf.length < off + len) return;
      const pl = this.buf.slice(off, off + len);
      this.buf = this.buf.slice(off + len);
      try {
        const m = JSON.parse(pl.toString('utf8'));
        if (m.id != null && this.handlers.has(m.id)) {
          const h = this.handlers.get(m.id); this.handlers.delete(m.id);
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
  results.push({ name, ok });
  console.log((ok ? 'PASS  ' : 'FAIL  ') + name + (detail ? '  → ' + detail : ''));
}

async function main() {
  const server = await serve();
  const profile = path.join(os.tmpdir(), 'bm-chrome-offline');
  try { fs.rmSync(profile, { recursive: true, force: true }); } catch (e) { }

  const proc = spawn(CHROME, [
    '--headless=new', '--remote-debugging-port=' + DEBUG_PORT,
    '--user-data-dir=' + profile, '--no-first-run', '--no-default-browser-check',
    '--disable-gpu', 'about:blank'
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
    await ws.send('Emulation.setDeviceMetricsOverride', {
      width: 393, height: 852, deviceScaleFactor: 2, mobile: true
    });

    const ev = async expr => {
      const r = await ws.send('Runtime.evaluate', {
        expression: expr, returnByValue: true, awaitPromise: true
      });
      if (r.exceptionDetails) throw new Error((r.exceptionDetails.exception || {}).description || r.exceptionDetails.text);
      return r.result.value;
    };

    const URL0 = 'http://127.0.0.1:' + PORT + '/index.html';
    await ws.send('Page.navigate', { url: URL0 });

    let ready = false;
    for (let i = 0; i < 50; i++) {
      await sleep(200);
      try { if (await ev('document.body.classList.contains("ready")')) { ready = true; break; } } catch (e) { }
    }
    check('首次打开渲染成功', ready);

    // 等 Service Worker 注册并激活
    let swState = 'none';
    for (let i = 0; i < 40; i++) {
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
    check('Service Worker 已激活', swState.indexOf('active') === 0, swState);

    // 缓存内容
    const cacheInfo = await ev(`(async () => {
      const names = await caches.keys();
      const out = {};
      for (const n of names) {
        const c = await caches.open(n);
        out[n] = (await c.keys()).length;
      }
      return JSON.stringify(out);
    })()`);
    const total = Object.values(JSON.parse(cacheInfo)).reduce((a, b) => a + b, 0);
    check('缓存已写入文件', total > 5, cacheInfo + ' 共 ' + total + ' 项');

    const hasCore = await ev(`(async () => {
      const names = await caches.keys();
      for (const n of names) {
        const c = await caches.open(n);
        const ks = (await c.keys()).map(r => r.url);
        if (ks.some(u => u.endsWith('app.css')) && ks.some(u => u.endsWith('dishes.json'))) return true;
      }
      return false;
    })()`);
    check('核心资源（CSS / 数据）已缓存', hasCore === true);

    // ---- 断网测试 ----
    await ws.send('Network.emulateNetworkConditions', {
      offline: true, latency: 0, downloadThroughput: 0, uploadThroughput: 0
    });

    await ws.send('Page.navigate', { url: URL0 });
    let offReady = false;
    for (let i = 0; i < 40; i++) {
      await sleep(250);
      try { if (await ev('document.body.classList.contains("ready")')) { offReady = true; break; } } catch (e) { }
    }
    check('断网后重新打开仍能渲染', offReady);

    if (offReady) {
      const offDishes = await ev('document.querySelectorAll("[data-dish]").length');
      check('断网状态下菜单仍有内容', offDishes > 0, offDishes + ' 个菜品卡');
      const offImg = await ev(`(() => {
        const imgs = [...document.querySelectorAll('.dish-card img')];
        return imgs.filter(i => i.complete && i.naturalWidth > 0).length + '/' + imgs.length;
      })()`);
      const [a, b] = offImg.split('/').map(Number);
      check('断网状态下图片可显示', b > 0 && a === b, offImg);
    }

  } finally {
    try { proc.kill(); } catch (e) { }
    server.close();
    const fail = results.filter(r => !r.ok);
    console.log('\n========== 结果 ==========');
    console.log(`通过 ${results.length - fail.length} / ${results.length}`);
    if (fail.length) fail.forEach(f => console.log('  FAIL: ' + f.name));
    setTimeout(() => process.exit(fail.length ? 1 : 0), 300);
  }
}

main().catch(e => {
  console.error('FATAL', e.message);
  process.exit(1);
});

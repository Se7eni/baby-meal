/**
 * 极简静态服务器 + Chrome CDP 截图工具
 *
 * 用法：
 *   node shot.js serve          只启服务（默认 8765）
 *   node shot.js shot <url> <out.png> <width> <height>
 *   node shot.js all            截四个页面的图
 *
 * 不依赖任何第三方库：用 Node 内置 http 做服务，
 * 用 Chrome 的 --remote-debugging-port 配合 WebSocket 做截图。
 */
const http = require('http');
const fs = require('fs');
const path = require('path');
const { spawn } = require('child_process');
const crypto = require('crypto');

const ROOT = __dirname;
const PORT = 8765;

const MIME = {
  '.html': 'text/html; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.js': 'text/javascript; charset=utf-8',
  '.json': 'application/json; charset=utf-8',
  '.png': 'image/png',
  '.webp': 'image/webp',
  '.ttf': 'font/ttf',
  '.svg': 'image/svg+xml'
};

function serve() {
  const server = http.createServer((req, res) => {
    let p = decodeURIComponent(req.url.split('?')[0]);
    if (p === '/') p = '/index.html';
    const file = path.join(ROOT, p);
    if (!file.startsWith(ROOT)) {
      res.writeHead(403); res.end('forbidden'); return;
    }
    fs.readFile(file, (err, data) => {
      if (err) {
        res.writeHead(404, { 'Content-Type': 'text/plain; charset=utf-8' });
        res.end('not found: ' + p);
        return;
      }
      res.writeHead(200, {
        'Content-Type': MIME[path.extname(file).toLowerCase()] || 'application/octet-stream',
        'Cache-Control': 'no-cache'
      });
      res.end(data);
    });
  });
  server.listen(PORT, '127.0.0.1', () => {
    console.log('serving http://127.0.0.1:' + PORT);
  });
  return server;
}

/* ---------------- 最小 CDP 客户端 ---------------- */

function wsConnect(url) {
  return new Promise((resolve, reject) => {
    const u = new URL(url);
    const key = crypto.randomBytes(16).toString('base64');
    const req = http.request({
      hostname: u.hostname,
      port: u.port,
      path: u.pathname + u.search,
      headers: {
        Connection: 'Upgrade',
        Upgrade: 'websocket',
        'Sec-WebSocket-Key': key,
        'Sec-WebSocket-Version': 13
      }
    });
    req.on('upgrade', (res, socket) => {
      const ws = new WSClient(socket);
      resolve(ws);
    });
    req.on('error', reject);
    req.end();
  });
}

class WSClient {
  constructor(socket) {
    this.socket = socket;
    this.buf = Buffer.alloc(0);
    this.handlers = new Map();
    this.id = 0;
    socket.on('data', d => this._onData(d));
    socket.on('error', () => { });
  }

  _onData(d) {
    this.buf = Buffer.concat([this.buf, d]);
    while (true) {
      if (this.buf.length < 2) return;
      const b1 = this.buf[1];
      let len = b1 & 0x7f;
      let off = 2;
      if (len === 126) {
        if (this.buf.length < 4) return;
        len = this.buf.readUInt16BE(2); off = 4;
      } else if (len === 127) {
        if (this.buf.length < 10) return;
        len = Number(this.buf.readBigUInt64BE(2)); off = 10;
      }
      if (this.buf.length < off + len) return;
      const payload = this.buf.slice(off, off + len);
      this.buf = this.buf.slice(off + len);
      // 只处理文本帧（不需要分片支持，CDP 消息都在单帧内）
      try {
        const msg = JSON.parse(payload.toString('utf8'));
        if (msg.id != null && this.handlers.has(msg.id)) {
          const { resolve, reject } = this.handlers.get(msg.id);
          this.handlers.delete(msg.id);
          if (msg.error) reject(new Error(JSON.stringify(msg.error)));
          else resolve(msg.result);
        } else if (msg.method && this._listeners) {
          const ls = this._listeners.get(msg.method);
          if (ls) ls.forEach(fn => fn(msg.params));
        }
      } catch (e) { /* 忽略非 JSON 帧 */ }
    }
  }

  send(method, params) {
    return new Promise((resolve, reject) => {
      const id = ++this.id;
      this.handlers.set(id, { resolve, reject });
      const payload = Buffer.from(JSON.stringify({ id, method, params: params || {} }));
      const len = payload.length;
      let header;
      if (len < 126) {
        header = Buffer.from([0x81, 0x80 | len]);
      } else if (len < 65536) {
        header = Buffer.alloc(4);
        header[0] = 0x81; header[1] = 0x80 | 126;
        header.writeUInt16BE(len, 2);
      } else {
        header = Buffer.alloc(10);
        header[0] = 0x81; header[1] = 0x80 | 127;
        header.writeBigUInt64BE(BigInt(len), 2);
      }
      // 客户端帧必须掩码
      const mask = crypto.randomBytes(4);
      const masked = Buffer.alloc(len);
      for (let i = 0; i < len; i++) masked[i] = payload[i] ^ mask[i % 4];
      this.socket.write(Buffer.concat([header, mask, masked]));
    });
  }

  on(method, fn) {
    if (!this._listeners) this._listeners = new Map();
    if (!this._listeners.has(method)) this._listeners.set(method, []);
    this._listeners.get(method).push(fn);
  }
}

const CHROME = 'C:/Program Files/Google/Chrome/Application/chrome.exe';
const DEBUG_PORT = 9333;

function sleep(ms) { return new Promise(r => setTimeout(r, ms)); }

async function httpGet(url) {
  return new Promise((resolve, reject) => {
    http.get(url, res => {
      let d = '';
      res.on('data', c => d += c);
      res.on('end', () => resolve(d));
    }).on('error', reject);
  });
}

async function startChrome() {
  const userDir = path.join(require('os').tmpdir(), 'bm-chrome-profile');
  const args = [
    '--headless=new',
    '--remote-debugging-port=' + DEBUG_PORT,
    '--user-data-dir=' + userDir,
    '--hide-scrollbars',
    '--no-first-run',
    '--no-default-browser-check',
    '--disable-gpu',
    '--force-device-scale-factor=2',
    'about:blank'
  ];
  const proc = spawn(CHROME, args, { stdio: 'ignore', detached: false });
  for (let i = 0; i < 40; i++) {
    await sleep(300);
    try {
      const txt = await httpGet('http://127.0.0.1:' + DEBUG_PORT + '/json/version');
      if (txt) return { proc, info: JSON.parse(txt) };
    } catch (e) { }
  }
  throw new Error('Chrome 启动超时');
}

async function newPage() {
  const txt = await httpGet('http://127.0.0.1:' + DEBUG_PORT + '/json/new?about:blank');
  // 新版 Chrome 的 /json/new 需要 PUT，兜底用 /json/list 的第一个
  try { return JSON.parse(txt); } catch (e) { }
  const list = JSON.parse(await httpGet('http://127.0.0.1:' + DEBUG_PORT + '/json/list'));
  return list[0];
}

async function shot(url, out, width, height, opts) {
  opts = opts || {};
  const target = await newPage();
  const ws = await wsConnect(target.webSocketDebuggerUrl);

  await ws.send('Page.enable');
  await ws.send('Runtime.enable');
  await ws.send('Emulation.setDeviceMetricsOverride', {
    width, height, deviceScaleFactor: 2, mobile: true
  });
  // iPhone 14 Pro 的 UA
  if (opts.ua !== false) {
    await ws.send('Emulation.setUserAgentOverride', {
      userAgent: 'Mozilla/5.0 (iPhone; CPU iPhone OS 16_5 like Mac OS X) AppleWebKit/605.1.15 '
        + '(KHTML, like Gecko) Version/16.5 Mobile/15E148 Safari/604.1'
    });
  }

  await ws.send('Page.navigate', { url });

  // 等应用渲染完成（body.ready 或超时）
  const deadline = Date.now() + 15000;
  while (Date.now() < deadline) {
    await sleep(250);
    const r = await ws.send('Runtime.evaluate', {
      expression: 'document.body.classList.contains("ready")',
      returnByValue: true
    });
    if (r.result && r.result.value === true) break;
  }
  await sleep(600);

  if (opts.js) {
    await ws.send('Runtime.evaluate', { expression: opts.js, awaitPromise: true });
    await sleep(opts.jsWait || 700);
  }

  // 全页高度
  const hr = await ws.send('Runtime.evaluate', {
    expression: 'Math.min(document.documentElement.scrollHeight, 6000)',
    returnByValue: true
  });
  const fullH = hr.result.value || height;

  await ws.send('Emulation.setDeviceMetricsOverride', {
    width, height: fullH, deviceScaleFactor: 2, mobile: true
  });
  await sleep(400);

  const res = await ws.send('Page.captureScreenshot', { format: 'png', captureBeyondViewport: true });
  fs.writeFileSync(out, Buffer.from(res.data, 'base64'));
  console.log('saved', out, width + 'x' + fullH);

  try { ws.socket.destroy(); } catch (e) { }
  return { width, height: fullH };
}

async function main() {
  const cmd = process.argv[2] || 'serve';
  if (cmd === 'serve') {
    serve();
    return;
  }

  const server = serve();
  const { proc } = await startChrome();
  const base = 'http://127.0.0.1:' + PORT + '/index.html';
  const W = 393, H = 852;

  try {
    if (cmd === 'shot') {
      await shot(process.argv[3], process.argv[4], 393, 852);
    } else if (cmd === 'all') {
      const outDir = path.join(ROOT, 'shots');
      fs.mkdirSync(outDir, { recursive: true });

      await shot(base, path.join(outDir, '01-today.png'), W, H);

      await shot(base, path.join(outDir, '02-list.png'), W, H, {
        js: 'document.querySelector("[data-tab=list]").click()'
      });

      await shot(base, path.join(outDir, '03-nutrition.png'), W, H, {
        js: 'document.querySelector("[data-tab=nutrition]").click()'
      });

      await shot(base, path.join(outDir, '04-detail.png'), W, H, {
        js: 'document.querySelector("[data-dish]").click()'
      });

      // iPad 视口（横屏不测，只测竖屏宽度）
      await shot(base, path.join(outDir, '05-ipad.png'), 820, 1180);
    }
  } finally {
    try { proc.kill(); } catch (e) { }
    server.close();
  }
}

main().catch(e => {
  console.error('ERROR', e.message);
  process.exit(1);
});

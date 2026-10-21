/**
 * Service Worker —— 让这个应用离线可用
 *
 * 策略：
 *   - 页面与脚本：先网络、失败回缓存（保证更新能生效，断网也能开）
 *   - 图片/字体/数据：先缓存、后台更新（省流量、启动快）
 *   - 导航请求离线时回 index.html
 *
 * 版本号变了才会触发更新，改完文件记得改这里。
 */
const VERSION = 'v1.1.1';
const CORE = `bm-core-${VERSION}`;
const ASSETS = `bm-assets-${VERSION}`;

const CORE_FILES = [
  './',
  './index.html',
  './manifest.json',
  './css/app.css',
  './js/nutrition.js',
  './js/menu.js',
  './js/app.js',
  './data/dishes.json',
  './fonts/deng.ttf',
  './fonts/dengb.ttf',
  './icons/apple-touch-icon-180.png',
  './icons/icon-192.png',
  './icons/icon-512.png'
];

self.addEventListener('install', event => {
  event.waitUntil(
    caches.open(CORE).then(cache =>
      // 逐个添加，某一个失败不影响整体安装
      Promise.all(CORE_FILES.map(url =>
        cache.add(url).catch(() => null)
      ))
    ).then(() => self.skipWaiting())
  );
});

self.addEventListener('activate', event => {
  event.waitUntil(
    caches.keys().then(keys =>
      Promise.all(keys
        .filter(k => k !== CORE && k !== ASSETS)
        .map(k => caches.delete(k)))
    ).then(() => self.clients.claim())
  );
});

self.addEventListener('fetch', event => {
  const req = event.request;
  if (req.method !== 'GET') return;

  const url = new URL(req.url);
  if (url.origin !== self.location.origin) return;

  // 导航：网络优先，断网回 index
  if (req.mode === 'navigate') {
    event.respondWith(
      fetch(req)
        .then(res => {
          const copy = res.clone();
          caches.open(CORE).then(c => c.put('./index.html', copy));
          return res;
        })
        .catch(() => caches.match('./index.html'))
    );
    return;
  }

  // 图片、字体、数据：缓存优先
  const isStatic = /\.(webp|png|jpg|jpeg|svg|ttf|woff2?|json)$/i.test(url.pathname);
  if (isStatic) {
    event.respondWith(
      caches.match(req).then(hit => {
        if (hit) return hit;
        return fetch(req).then(res => {
          if (res && res.status === 200) {
            const copy = res.clone();
            caches.open(ASSETS).then(c => c.put(req, copy));
          }
          return res;
        });
      })
    );
    return;
  }

  // 其余脚本样式：网络优先
  event.respondWith(
    fetch(req)
      .then(res => {
        if (res && res.status === 200) {
          const copy = res.clone();
          caches.open(CORE).then(c => c.put(req, copy));
        }
        return res;
      })
      .catch(() => caches.match(req))
  );
});

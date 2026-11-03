// Checks the shared packaged assets in headless Chromium; host boundaries are tested separately.
const assert = require('node:assert/strict');
const fs = require('node:fs/promises');
const path = require('node:path');
const { chromium } = require(process.env.PAYKI_PLAYWRIGHT_MODULE || 'playwright');
const root = path.resolve(__dirname, '../..');
const assets = path.join(root, 'app/src/main/assets');
const evidence = process.argv[2];
if (!evidence) throw new Error('Supply an evidence directory outside the repository');
const origin = 'https://payki.content';
const allowed = new Set([
  'content-viewer/index.html', 'content-viewer/viewer.css', 'content-viewer/viewer.js',
  'content-viewer/vendor/marked.js', 'content-viewer/vendor/purify.js', 'wiki/index.html', 'wiki/wiki.js'
]);

(async () => {
  await fs.mkdir(evidence, { recursive: true });
  const browser = await chromium.launch({ headless: true,
    ...(process.env.PAYKI_BROWSER_EXE ? { executablePath: process.env.PAYKI_BROWSER_EXE } : {}) });
  const page = await browser.newPage({ viewport: { width: 320, height: 720 } });
  const errors = [], external = [], stages = [];
  page.on('pageerror', error => errors.push(error.message));
  await page.route('**/*', async route => {
    const url = new URL(route.request().url());
    const relative = url.pathname.slice(1);
    if (url.origin !== origin) { external.push(url.origin); await route.abort(); return; }
    if (!allowed.has(relative) || url.search) { await route.fulfill({ status: 403, body: 'Blocked' }); return; }
    const contentType = relative.endsWith('.js') ? 'application/javascript' : relative.endsWith('.css') ? 'text/css' : 'text/html';
    await route.fulfill({ contentType, body: await fs.readFile(path.join(assets, relative)) });
  });
  const render = async payload => {
    await page.evaluate(value => window.postMessage(JSON.stringify(value), location.origin), payload);
    await page.waitForFunction(value => document.title === value.title && document.getElementById('content').innerText !== '正在打开…', payload);
  };
  const fits = async () => assert.equal(await page.evaluate(() => document.documentElement.scrollWidth <= document.documentElement.clientWidth + 1), true);
  const record = name => stages.push(name);
  try {
    await page.goto(origin + '/content-viewer/index.html');
    const source = await fs.readFile(path.join(root, 'tools/tests/fixtures/report-syntax.md'), 'utf8');
    await render({ content: source, title: '共用语法', fontSize: 17, dark: false });
    assert.deepEqual(await page.evaluate(() => ({
      headings: [...document.querySelectorAll('h1,h2,h3,h4,h5,h6')].map(node => node.tagName),
      tables: document.querySelectorAll('.table-scroll table').length,
      tasks: document.querySelectorAll('input[type=checkbox]:disabled').length,
      nested: document.querySelectorAll('ol ul').length,
      code: document.querySelectorAll('pre code').length,
      unsafe: document.querySelectorAll('img,iframe,object,script:not([src]),a[href^=javascript],a[href^=file]').length
    })), { headings: ['H1','H2','H3','H4','H5','H6'], tables: 1, tasks: 2, nested: 1, code: 1, unsafe: 0 });
    await fits(); record('GFM共用样例、只读任务项、原始HTML和危险协议处理');

    await page.evaluate(() => window.dispatchEvent(new MessageEvent('message', {
      origin: '', source: null, data: JSON.stringify({content:'# 原生宿主消息', title:'原生宿主', fontSize:17})
    })));
    assert.equal(await page.title(), '原生宿主');
    for (const origin of ['https://untrusted.example', '']) {
      await page.evaluate(origin => window.dispatchEvent(new MessageEvent('message', {
        origin, source: window, data: JSON.stringify({content:'# 跨来源内容', title:'不应替换'})
      })), origin);
      assert.equal(await page.title(), '原生宿主');
    }
    record('原生空origin且无Window消息可接收、普通跨来源及空origin页面消息被拒绝');

    const wideTable = '| ' + Array.from({length: 20}, (_, i) => '字段' + i).join(' | ') + ' |\n| ' + Array(20).fill('---').join(' | ') + ' |\n| ' + Array(20).fill('较长单元格内容').join(' | ') + ' |';
    const narrow = '# 窄屏压力\n\n' + 'https://example.com/' + 'a'.repeat(800) + '\n\n' + wideTable + '\n\n```text\n' + 'code'.repeat(250) + '\n```';
    for (const dark of [false, true]) for (const fontSize of [12, 24, 36]) {
      await render({ content: narrow, title: `窄屏-${dark}-${fontSize}`, dark, fontSize });
      await fits();
      assert.equal(await page.evaluate(() => ['pre', '.table-scroll'].every(selector => {
        const node = document.querySelector(selector);
        return node.scrollWidth > node.clientWidth && getComputedStyle(node).overflowX === 'auto';
      })), true);
    }
    await page.screenshot({ path: path.join(evidence, 'reader-320-dark-36.png'), fullPage: true });
    record('320px明暗主题12/24/36px正文不溢出、宽表格和长代码独立横滚');

    await render({ content: '<script>window.__unsafe=1</script>\n\n<img src=https://example.com/test.png onerror=alert(1)>\n\n[脚本](javascript:alert(1))\n\n[文件](file:///C:/test.txt)\n\n[资料](data:text/html,bad)\n\n[普通链接](https://example.com/)', title: '安全边界', fontSize: 17 });
    assert.equal(await page.evaluate(() => window.__unsafe === undefined && document.querySelectorAll('img,script:not([src])').length === 0 && [...document.querySelectorAll('a[href]')].every(link => /^https?:\/\//.test(link.getAttribute('href')))), true);
    assert.equal(await page.locator('a[href="https://example.com/"]').count(), 1);
    assert.deepEqual(external, []); record('不执行HTML、不自动取远程图片、不保留脚本文件或data链接');

    await render({ content: '', title: '空报告' });
    assert.match(await page.locator('#content').innerText(), /空报告/);
    await render({ content: '普通文本\n未闭合 **强调\n```\n保留剩余原文', title: '未闭合报告' });
    assert.match(await page.locator('#content').innerText(), /保留剩余原文/);
    record('空报告、纯文本和未闭合Markdown可读');

    const long = '# 长报告\n\n' + Array.from({length: 2000}, (_, i) => `第${i}条：今天的记录仍保留原文。`).join('\n\n');
    const started = Date.now();
    await render({ content: long, title: '长报告', fontSize: 17 });
    assert.match(await page.locator('#content').innerText(), /第1999条/);
    await fits(); record(`数万字报告完整排版（本次${Date.now() - started}ms，不作为性能保证）`);

    await page.goto(origin + '/wiki/index.html');
    assert.ok(await page.locator('details nav a').count() >= 10);
    await page.locator('details summary').click();
    await page.locator('details nav a').nth(2).click();
    assert.equal(await page.locator('details').getAttribute('open'), null);
    assert.ok(new URL(page.url()).hash.startsWith('#chapter-'));
    await page.evaluate(() => window.postMessage(JSON.stringify({ dark: true, fontSize: 24, fontFamily: 'sans-serif' }), location.origin));
    await page.waitForFunction(() => document.documentElement.classList.contains('dark'));
    await fits(); await page.screenshot({ path: path.join(evidence, 'help-320-dark-24.png') });
    record('实际打包手机HTML目录可展开、点击锚点后收起、窄屏和暗色可读');
    assert.deepEqual(errors, []); assert.deepEqual(external, []);
    await fs.writeFile(path.join(evidence, 'shared-reader-results.json'), JSON.stringify({ scope: 'Shared assets in headless Chromium, not platform host or account tests', stages, pageErrors: errors, externalRequests: external }, null, 2));
    console.log(JSON.stringify({ passed: stages.length, pageErrors: errors.length, externalRequests: external.length }));
  } finally { await browser.close(); }
})().catch(error => { console.error(error); process.exitCode = 1; });

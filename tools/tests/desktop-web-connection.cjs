// Run with Playwright available through NODE_PATH; uses the installed Edge browser.
const { chromium } = require('playwright');
const { readFileSync } = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');

(async () => {
  const browser = await chromium.launch({ channel: 'msedge', headless: true });
  const page = await browser.newPage({ viewport: { width: 1440, height: 1000 } });
  const root = path.resolve(__dirname, '../../app/src/main/assets/desktop-web');
  let offline = false;
  let pendingResponse;
  const snapshot = { generatedAtMillis: Date.now(), groups: [{ id: 1, name: '默认', colorHex: '#4CB782' }],
    quickChecklistGroups: [], todos: [], events: [], announcements: [], todayBoard: null };
  await page.route('http://payki.test/**', async route => {
    const url = new URL(route.request().url());
    if (url.pathname.startsWith('/api/')) {
      if (offline) return route.abort('connectionrefused');
      if (url.pathname === '/api/race') { pendingResponse = route; return; }
      return route.fulfill({ json: url.pathname === '/api/status' ? { running: true } : snapshot });
    }
    try {
      const filename = url.pathname === '/' ? 'index.html' : url.pathname.slice(1);
      const buffer = readFileSync(path.join(root, filename));
      return route.fulfill({ body: filename === 'index.html' ? buffer.toString().replaceAll('__PAYKI_VERSION__', '1.16.20').replaceAll('__PAYKI_ICON_DATA__', 'data:image/png;base64,') : buffer,
        contentType: filename.endsWith('.js') ? 'application/javascript' : filename.endsWith('.css') ? 'text/css' : 'text/html' });
    } catch { return route.fulfill({ status: 404, body: '' }); }
  });
  try {
    await page.goto('http://payki.test/');
    async function connect() {
      await page.locator('#token').fill('ABCD'); await page.locator('#connect').click();
      await page.locator('.shell').waitFor({ state: 'visible' });
    }
    await connect();
    const modalIds = await page.locator('.modal-backdrop').evaluateAll(nodes => nodes.map(node => node.id));
    for (const modalId of modalIds) {
      await page.evaluate(id => { openModal(id); document.querySelectorAll('#' + id + ' input, #' + id + ' textarea').forEach(input => { if (!['checkbox','radio','number','date','time','file'].includes(input.type)) input.value = '未完成的私人草稿'; }); }, modalId);
      await page.evaluate(() => disconnect());
      assert.equal(await page.locator('.modal-backdrop:not(.hidden)').count(), 0, modalId);
      assert.equal(await page.locator('.modal-backdrop input, .modal-backdrop textarea').evaluateAll(inputs => inputs.some(input => input.value.includes('私人草稿'))), false);
      await connect();
    }
    await page.evaluate(() => { window.cancelResult = undefined; confirmDanger('删除', '测试').then(value => window.cancelResult = value); disconnect(); });
    assert.equal(await page.evaluate(() => window.cancelResult), false);
    await connect();
    await page.evaluate(() => { window.formResult = 'pending'; showFormDialog({ title: '编辑', fields: [{ id: 'title', value: 'draft' }] }).then(value => window.formResult = value); disconnect(); });
    assert.equal(await page.evaluate(() => window.formResult), null);
    await connect();
    await page.evaluate(() => { openModal('event-modal'); window.race = api('/api/race').then(() => 'accepted', () => 'rejected'); });
    await page.waitForTimeout(100);
    await page.evaluate(() => disconnect());
    await connect();
    if (pendingResponse) await pendingResponse.fulfill({ json: { stale: true } }).catch(() => {});
    assert.equal(await page.evaluate(() => window.race), 'rejected');
    await page.evaluate(() => openModal('event-modal'));
    offline = true;
    await page.evaluate(() => api('/api/status').catch(() => {}));
    assert.equal(await page.locator('.modal-backdrop:not(.hidden)').count(), 0);
    await page.locator('#token').fill('ABCD'); await page.locator('#connect').click();
    await page.waitForFunction(() => document.querySelector('#login-status').textContent.includes('手机尚未开启'));
    assert.doesNotMatch(await page.locator('#login-status').innerText(), /failed to fetch/i);
    await page.screenshot({ path: process.env.PAYKI_WEB_EVIDENCE || '.tmp/resume-20260923/web-disconnected.png' });
    console.log(`PASS: ${modalIds.length} dialogs, canceled confirmations/forms, stale response, disconnect and reconnect error`);
  } finally { await browser.close(); }
})().catch(error => { console.error(error); process.exitCode = 1; });

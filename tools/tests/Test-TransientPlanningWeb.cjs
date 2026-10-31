// Headless browser acceptance against disposable HTTP fixtures; no account or user data.
const assert = require('node:assert/strict');
const fs = require('node:fs/promises');
const path = require('node:path');
const http = require('node:http');
const { chromium } = require(process.env.PAYKI_PLAYWRIGHT_MODULE || 'playwright');
const root = path.resolve(__dirname, '../..');
const assets = path.join(root, 'app/src/main/assets/desktop-web');
const evidence = process.argv[2];
if (!evidence) throw new Error('Supply an evidence directory outside the repository');
const snapshot = { groups: [{ id: 1, name: '常规' }], todos: [], events: [], quickChecklistGroups: [], partial: true, generatedAtMillis: Date.now() };
let previews = 0, imports = 0, lastImport = null, failNext = false;
const sourceCandidate = { id: '1:3', parentId: null, lineNumber: 1, title: '阅读', time: '16:30', reminders: '', range: '', notes: '', location: '', calendarId: 'default', type: 'TODO', slots: [] };
const server = http.createServer(async (request, response) => {
  try {
    const pathname = new URL(request.url, 'http://localhost').pathname;
    if (pathname.startsWith('/api/')) {
      let raw = ''; for await (const chunk of request) raw += chunk;
      const body = raw ? JSON.parse(raw) : {};
      let result;
      if (pathname === '/api/planning/transient/preview') {
        previews++;
        result = { sessionId: String(previews), candidates: [{ ...sourceCandidate }], diagnostics: [], calendars: [{ id: 'default', name: '常规' }] };
        if (body.text.includes('错误输入')) result.diagnostics = [{ lineNumber: 1, column: 4, message: '结束时间必须晚于开始时间' }];
      } else if (pathname === '/api/planning/transient/import') {
        if (failNext) { failNext = false; response.writeHead(500, { 'Content-Type': 'application/json' }); response.end(JSON.stringify({ error: '预览错误请修正' })); return; }
        imports++; lastImport = body;
        await new Promise(resolve => setTimeout(resolve, 100));
        result = { ok: true, imported: body.candidates.length };
      } else if (pathname === '/api/planning/transient/continuation') {
        result = { prefix: body.line.includes(';') ? '- ' : '  - ' };
      } else if (pathname === '/api/planning/archive') {
        result = { notes: [{ title: '已归档原文', contentMarkdown: '保留,全部;旧材料' }] };
      } else if (pathname === '/api/snapshot') result = snapshot;
      else if (pathname === '/api/ai-report/status') result = {};
      else result = { items: [], groups: [], reports: [] };
      response.writeHead(200, { 'Content-Type': 'application/json' }); response.end(JSON.stringify(result));
      return;
    }
    const file = pathname === '/' ? 'index.html' : path.basename(pathname);
    const bytes = await fs.readFile(path.join(assets, file));
    response.writeHead(200, { 'Content-Type': file.endsWith('.js') ? 'application/javascript' : file.endsWith('.css') ? 'text/css' : 'text/html' });
    response.end(bytes);
  } catch { response.writeHead(404); response.end('Fixture resource unavailable'); }
});
(async () => {
  await fs.mkdir(evidence, { recursive: true });
  await new Promise(resolve => server.listen(0, '127.0.0.1', resolve));
  const browser = await chromium.launch({ headless: true, ...(process.env.PAYKI_BROWSER_EXE ? { executablePath: process.env.PAYKI_BROWSER_EXE } : {}) });
  const page = await browser.newPage({ viewport: { width: 1280, height: 900 } });
  const errors = []; page.on('pageerror', error => errors.push(error.message));
  const stages = [];
  try {
    await page.goto(`http://127.0.0.1:${server.address().port}/`);
    await page.evaluate(data => { state.token = 'TEST'; state.snapshot = data; showAppShell(); syncTopbar(); }, snapshot);
    assert.equal(await page.locator('[data-tab="planning"]').count(), 0);
    assert.equal(await page.locator('#open-quick-checklist').count(), 0);
    await page.locator('#open-transient-planning').click();
    await page.locator('#transient-planning-text').fill('- 阅读,16:30;');
    await page.locator('#transient-planning-confirm').click();
    await page.getByRole('heading', { name: '确认规划', exact: true }).waitFor();
    assert.equal(imports, 0);
    await page.locator('#transient-planning-cancel').click(); assert.equal(imports, 0);
    stages.push('输入和识别后取消没有提交');

    await page.locator('#open-transient-planning').click();
    await page.locator('#transient-planning-text').fill('- 父事项');
    await page.locator('#transient-planning-text').press('Enter');
    await page.waitForFunction(() => document.getElementById('transient-planning-text').value.endsWith('\n  - '));
    await page.locator('#transient-planning-text').press('Shift+Tab');
    assert.equal(await page.locator('#transient-planning-text').inputValue(), '- 父事项\n- ');
    stages.push('Enter和Shift+Tab输入辅助可操作');
    await page.locator('#transient-planning-text').fill('- 阅读,16:30;');
    await page.locator('#transient-planning-confirm').click();
    await page.getByRole('heading', { name: '确认规划', exact: true }).waitFor();
    await page.locator('[data-field="title"]').fill('修改,标题; "原样"');
    await page.locator('[data-field="notes"]').fill('先读第二章; #备注');
    failNext = true;
    await page.locator('#transient-planning-confirm').click();
    await page.locator('#transient-planning-error').filter({ hasText: '预览错误请修正' }).waitFor();
    assert.equal(imports, 0); assert.equal(await page.locator('[data-field="title"]').inputValue(), '修改,标题; "原样"');
    await page.screenshot({ path: path.join(evidence, 'web-preview-wide.png'), fullPage: true });
    await page.evaluate(() => { const button = document.getElementById('transient-planning-confirm'); button.click(); button.click(); });
    await page.waitForFunction(() => document.getElementById('transient-planning-modal').classList.contains('hidden'));
    assert.equal(imports, 1); assert.equal(lastImport.candidates[0].title, '修改,标题; "原样"');
    assert.equal(lastImport.candidates[0].notes, '先读第二章; #备注');
    stages.push('预览可编辑、失败保留草稿、重复点击只提交一次');

    await page.setViewportSize({ width: 480, height: 720 });
    await page.locator('#open-transient-planning').click();
    await page.locator('#transient-planning-text').fill('错误输入,23:00>01:00;');
    await page.locator('#transient-planning-confirm').click();
    await page.locator('#transient-planning-error').filter({ hasText: '第1行，第4列' }).waitFor();
    assert.equal(await page.locator('#transient-planning-confirm').isDisabled(), true);
    await page.screenshot({ path: path.join(evidence, 'web-diagnostics-narrow.png'), fullPage: true });
    await page.locator('#transient-planning-back').click();
    assert.equal(await page.locator('#transient-planning-text').inputValue(), '错误输入,23:00>01:00;');
    await page.evaluate(() => endConnection('独立测试连接结束'));
    assert.equal(await page.locator('#transient-planning-modal').isVisible(), false);
    assert.equal(await page.locator('#transient-planning-text').inputValue(), '');
    stages.push('错误定位、返回原文、连接结束关闭草稿');

    await page.evaluate(data => { state.token = 'TEST'; state.snapshot = data; showAppShell(); syncTopbar(); }, snapshot);
    await page.locator('#open-legacy-planning').click();
    await page.getByText('已归档原文', { exact: true }).waitFor();
    assert.equal(imports, 1); stages.push('旧原文只读入口可访问');
    assert.deepEqual(errors, []);
    await fs.writeFile(path.join(evidence, 'result.json'), JSON.stringify({ passed: true, stages, imports, previews, pageErrors: errors, scope: '真实网页控件与模拟HTTP；业务解析/事务另由Android设备测试覆盖' }, null, 2));
    console.log(JSON.stringify({ passed: true, stages: stages.length, imports, pageErrors: errors }));
  } finally { await browser.close(); server.close(); }
})().catch(error => { console.error(error); server.close(); process.exitCode = 1; });

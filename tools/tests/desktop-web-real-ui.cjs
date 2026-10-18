// Uses the phone's actual LAN service through adb forward, not mocked routes.
const { chromium } = require('playwright');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

(async () => {
  const folder = process.env.PAYKI_UI_EVIDENCE;
  if (!folder) throw new Error('PAYKI_UI_EVIDENCE required');
  const browser = await chromium.launch({ channel: 'msedge', headless: false });
  const page = await browser.newPage({ viewport: { width: 1280, height: 850 } });
  const errors = [];
  page.on('pageerror', error => errors.push(error.message));
  const record = (id, result) => fs.appendFileSync(path.join(folder, 'lan-visible-steps.txt'), `${id} PASS ${result}\n`);
  const headers = { 'X-Payki-Token': 'ABCD' };
  try {
    await page.goto('http://127.0.0.1:18765');
    await page.locator('#token').fill('ABCD');
    await page.locator('#connect').click();
    await page.locator('.shell').waitFor({ state: 'visible' });
    record('L01', 'Visible Edge connects to actual Android LAN service with synthetic token');
    await page.locator('#open-quick-checklist').click();
    const input = page.locator('#quick-checklist-input');
    const source = '- LAN parent\n-- LAN child\n--- LAN leaf\n---# LAN note\n// ignored comment';
    await input.fill(source);
    await page.waitForTimeout(600);
    await page.locator('#save-quick-checklist').click();
    await page.waitForTimeout(1000);
    const response = await page.request.get('http://127.0.0.1:18765/api/snapshot', { headers });
    assert.equal(response.status(), 200);
    const data = await response.json();
    fs.writeFileSync(path.join(folder, 'lan-synthetic-snapshot.json'), JSON.stringify(data));
    assert.equal(data.quickChecklistGroups.find(group => group.title === '').title, '');
    assert.ok(JSON.stringify(data).includes('LAN child'));
    assert.ok(JSON.stringify(data).includes('LAN note'));
    assert.ok(!JSON.stringify(data).includes('ignored comment'));
    record('L02', 'Actual browser titleless hierarchy save creates Android snapshot group/tasks with note and no comment');
    await page.locator('#open-quick-checklist').click();
    await input.fill('- Enter continuation');
    await input.press('Enter');
    assert.ok((await input.inputValue()).endsWith('\n- '));
    await page.keyboard.press('Escape');
    record('L03', 'Real browser Enter key continues dash');
    await page.screenshot({ path: path.join(folder, 'lan-real-titleless.png') });
    assert.deepEqual(errors, []);
    record('L04', 'No JavaScript runtime exceptions during actual connection/create/edit flow');
  } finally { await browser.close(); }
})().catch(error => { console.error(error); process.exitCode = 1; });

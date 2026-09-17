// 本轮修复的回归：树形表格删除删对行、批量启停保护 admin、H2 控制台仅 admin、
// 渲染函数 HTML 编码、菜单 URL 白名单、部门上级成环校验
const { chromium } = require('@playwright/test');
const BASE = process.env.BASE || 'http://127.0.0.1:8082';
const OUT = process.env.OUT || '.';
(async () => {
  const browser = await chromium.launch();
  const page = await browser.newPage({ viewport: { width: 1400, height: 900 } });
  const errors = [];
  page.on('pageerror', e => errors.push('[pageerror] ' + e.message));
  page.on('response', r => { if (r.status() >= 500) errors.push('[http ' + r.status() + '] ' + r.url()); });
  const step = async (name, fn) => {
    try { await fn(); console.log('OK  ' + name); }
    catch (e) { console.log('FAIL ' + name + ': ' + e.message.split('\n').slice(0, 2).join(' | ')); await page.screenshot({ path: OUT + '/vfail-' + name.replace(/[^\w一-龥]/g, '_') + '.png' }); }
  };
  const openMenu = async (text, slug) => {
    await page.goto(BASE + '/');
    await page.waitForSelector('#treeMenu .f-tree-node', { timeout: 15000 });
    await page.locator('#treeMenu .f-tree-node', { hasText: text }).first().click();
    const handle = await page.waitForSelector('iframe[src$="/admin/' + slug + '"]', { timeout: 20000 });
    const frame = await handle.contentFrame();
    await frame.waitForSelector('.f-grid, .f-form, .f-panel', { timeout: 20000 });
    await page.waitForTimeout(600);
    return frame;
  };
  const rowIds = (frame, gridId) => frame.evaluate(id => Array.from(document.querySelectorAll('#' + id + ' .f-grid-row')).map(r => r.getAttribute('data-rowid')), gridId);
  const boxText = async (ctx) => (await ctx.locator('.f-messagebox').allInnerTexts()).join(' ').replace(/\s+/g, ' ');

  await page.goto(BASE + '/login');
  await page.waitForSelector('#tbxUserName input', { timeout: 30000 });
  await page.fill('#tbxUserName input', 'admin');
  await page.fill('#tbxPassword input', 'admin');
  await page.click('#btnSubmit');
  await page.waitForSelector('#treeMenu .f-tree-node', { timeout: 15000 });

  // 1) 树形表格的删除必须删「点的那一行」：树的行号是层内序号，与扁平数据顺序不同
  await step('树形表格删除删对行（部门）', async () => {
    // 自建一个二级部门再删它：本用例可重复运行，且删除目标一定是无子部门无用户的叶子
    const target = '冒烟部门' + Date.now().toString().slice(-5);
    let frame = await openMenu('部门管理', 'dept');
    await frame.click('#btnNew');
    const nh = await page.waitForSelector('iframe[src*="dept-new"]', { timeout: 20000 });
    const nw = await nh.contentFrame();
    await nw.waitForSelector('#dept_name input');
    await nw.fill('#dept_name input', target);
    await nw.fill('#dept_sortIndex input', '1');
    await page.waitForTimeout(500);
    await nw.evaluate(() => {
      const row = Array.from(document.querySelectorAll('#Grid1 .f-grid-row')).find(r => r.innerText.indexOf('研发部') >= 0);
      F.ui.ddbParent.setValue(row.getAttribute('data-rowid'));
    });
    await nw.click('#btnSaveClose');
    await page.waitForTimeout(2000);
    frame = await openMenu('部门管理', 'dept');
    const before = await rowIds(frame, 'Grid1');
    const targetId = await frame.evaluate(t => {
      const row = Array.from(document.querySelectorAll('#Grid1 .f-grid-row')).find(r => r.innerText.includes(t));
      return row ? row.getAttribute('data-rowid') : null;
    }, target);
    await frame.locator('#Grid1 .f-grid-row', { hasText: target }).locator('.f-grid-command').nth(1).click();
    await page.locator('.f-messagebox .f-btn', { hasText: '确定' }).first().click();
    await page.waitForTimeout(1800);
    const after = await rowIds(frame, 'Grid1');
    const gone = before.filter(id => !after.includes(id));
    console.log('     被删主键：' + JSON.stringify(gone) + '（目标 ' + targetId + '）');
    if (gone.length !== 1 || gone[0] !== targetId) throw new Error('删错行');
  });

  // 2) 批量启停不能作用于 admin（回传主键不可信）
  await step('批量禁用 admin 被拒绝', async () => {
    const frame = await openMenu('用户管理', 'user-list');
    await frame.fill('#ttbSearchMessage input', 'admin');
    await frame.press('#ttbSearchMessage input', 'Enter');
    await page.waitForTimeout(1500);
    const ids = await rowIds(frame, 'Grid1');
    console.log('     admin 行主键：' + JSON.stringify(ids));
    await frame.evaluate(id => F.customEvent('Grid1_EnableRows', { action: 'disable', rowIDs: [Number(id)] }), ids[0]);
    await page.waitForTimeout(2000);
    const boxes = await boxText(page);
    console.log('     提示：' + boxes.slice(0, 120));
    if (boxes.indexOf('超级管理员') < 0) throw new Error('未拒绝');
    await page.locator('.f-messagebox .f-btn').first().click();
  });

  // 3) H2 控制台：admin 可进
  await step('H2 控制台 admin 可访问', async () => {
    const r = await page.request.get(BASE + '/h2-console');
    if (r.status() >= 400) throw new Error('status ' + r.status());
  });

  // 4) 菜单链接拒绝伪协议
  await step('菜单链接拒绝 javascript 伪协议', async () => {
    const frame = await openMenu('菜单管理', 'menu');
    await frame.click('#btnNew');
    const wh = await page.waitForSelector('iframe[src*="menu-new"]', { timeout: 20000 });
    const win = await wh.contentFrame();
    await win.waitForSelector('#menu_name input');
    await win.fill('#menu_name input', '恶意菜单');
    await win.fill('#menu_sortIndex input', '999');
    await win.fill('#menu_navigateUrl input', 'javascript:alert(1)');
    await page.waitForTimeout(600);
    await win.click('#btnSaveClose');
    await page.waitForTimeout(2000);
    const boxes = await boxText(win);
    console.log('     提示：' + boxes.slice(0, 120));
    if (boxes.indexOf('站内路径') < 0) throw new Error('未拒绝');
  });

  // 5) 部门上级不能选自己的下级（成环）
  await step('部门上级拒绝选下级（成环）', async () => {
    const frame = await openMenu('部门管理', 'dept');
    await frame.locator('#Grid1 .f-grid-row', { hasText: '研发部' }).first().locator('.f-grid-command').first().click();
    const wh = await page.waitForSelector('iframe[src*="dept-edit"]', { timeout: 20000 });
    const win = await wh.contentFrame();
    await win.waitForSelector('#ddbParent');
    await win.evaluate(() => {
      const child = Array.from(document.querySelectorAll('#Grid1 .f-grid-row')).find(r => r.innerText.indexOf('开发部') >= 0);
      F.ui.ddbParent.setValue(child.getAttribute('data-rowid'));
    });
    await win.click('#btnSaveClose');
    await page.waitForTimeout(2000);
    const boxes = await boxText(win);
    console.log('     提示：' + boxes.slice(0, 120));
    if (boxes.indexOf('下级部门') < 0) throw new Error('未拒绝');
  });

  // 6) 渲染函数编码：权限标题里的 HTML 不当标签渲染
  await step('角色权限页渲染函数编码', async () => {
    const frame = await openMenu('权限管理', 'power');
    await frame.click('#btnNew');
    const wh = await page.waitForSelector('iframe[src*="power-new"]', { timeout: 20000 });
    const win = await wh.contentFrame();
    await win.waitForSelector('#power_name input');
    await win.fill('#power_name input', 'XssProbe' + Date.now().toString().slice(-4));
    await win.fill('#power_groupName input', 'XssGroup');
    await win.fill('#power_title input', '<img src=x onerror=window.__xss=1>');
    await page.waitForTimeout(600);
    await win.click('#btnSaveClose');
    await page.waitForTimeout(1800);
    const rf = await openMenu('角色权限管理', 'role-power');
    await rf.waitForSelector('#Grid2 .f-grid-row');
    const r = await rf.evaluate(() => {
      const row = Array.from(document.querySelectorAll('#Grid2 .f-grid-row')).find(x => x.innerText.indexOf('XssGroup') >= 0);
      return { xss: window.__xss || null, text: row ? row.innerText.replace(/\s+/g, ' ').slice(0, 100) : null, imgCount: row ? row.querySelectorAll('img').length : -1 };
    });
    console.log('     window.__xss=' + r.xss + ' | 行文本：' + r.text + ' | 注入的 img 数：' + r.imgCount);
    if (r.xss || r.imgCount > 0) throw new Error('HTML 被当标签渲染');
  });

  console.log(errors.length ? '错误：\n' + errors.join('\n') : '无 pageerror / 5xx');
  await browser.close();
})();

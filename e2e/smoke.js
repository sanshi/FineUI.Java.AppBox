// FineUI.Java.AppBox 冒烟：登录 → 主框架 → 用户管理列表 → 新增/查看弹窗；收集控制台错误与请求失败
const { chromium } = require('@playwright/test');
const BASE = process.env.BASE || 'http://127.0.0.1:8082';
const OUT = process.env.OUT || '.';
(async () => {
  const browser = await chromium.launch();
  const page = await browser.newPage({ viewport: { width: 1400, height: 900 } });
  const errors = [];
  page.on('console', m => { if (m.type() === 'error') errors.push('[console] ' + m.text()); });
  page.on('pageerror', e => errors.push('[pageerror] ' + e.message));
  page.on('requestfailed', r => errors.push('[requestfailed] ' + r.url() + ' ' + (r.failure() || {}).errorText));
  page.on('response', r => { if (r.status() >= 400) errors.push('[http ' + r.status() + '] ' + r.url()); });
  const step = async (name, fn) => { try { await fn(); console.log('OK  ' + name); } catch (e) { console.log('FAIL ' + name + ': ' + e.message.split('\n')[0]); } };

  await step('未登录访问 / 跳转到 /login', async () => {
    await page.goto(BASE + '/');
    await page.waitForURL(/\/login$/);
    await page.waitForSelector('#tbxUserName input', { timeout: 15000 });
  });
  await step('登录 admin/admin 进入首页', async () => {
    await page.fill('#tbxUserName input', 'admin');
    await page.fill('#tbxPassword input', 'admin');
    await page.click('#btnSubmit');
    await page.waitForURL(u => !/\/login/.test(u.toString()), { timeout: 15000 });
    await page.waitForSelector('#treeMenu .f-tree-node', { timeout: 15000 });
    await page.screenshot({ path: OUT + '/01-index.png' });
  });
  await step('菜单树含「用户管理」并打开', async () => {
    const node = page.locator('#treeMenu .f-tree-node', { hasText: '用户管理' }).first();
    await node.waitFor({ timeout: 10000 });
    await node.click();
    const frame = page.frameLocator('iframe[src*="user-list"]');
    await frame.locator('#Grid1 .f-grid-row').first().waitFor({ timeout: 20000 });
    const rows = await frame.locator('#Grid1 .f-grid-row').count();
    console.log('     用户列表行数：' + rows);
    await page.screenshot({ path: OUT + '/02-user-list.png' });
  });
  await step('点击「新增用户」弹出新增窗口', async () => {
    const frame = page.frameLocator('iframe[src*="user-list"]');
    await frame.locator('#btnNew').click();
    const win = page.frameLocator('iframe[src*="user-new"]');
    await win.locator('#SimpleForm1').waitFor({ timeout: 20000 });
    await page.screenshot({ path: OUT + '/03-user-new.png' });
    await win.locator('#btnClose').click();
  });
  await step('行内「查看」弹窗', async () => {
    const frame = page.frameLocator('iframe[src*="user-list"]');
    await frame.locator('#Grid1 .f-grid-row').first().locator('.f-grid-command').first().click();
    const win = page.frameLocator('iframe[src*="user-view"]');
    await win.locator('#labRoles').waitFor({ timeout: 20000 });
    await page.screenshot({ path: OUT + '/04-user-view.png' });
    await win.locator('#btnClose').click();
  });
  await step('搜索框过滤', async () => {
    const frame = page.frameLocator('iframe[src*="user-list"]');
    await frame.locator('#ttbSearchMessage input').fill('user1');
    await frame.locator('#ttbSearchMessage input').press('Enter');
    await page.waitForTimeout(1500);
    const rows = await frame.locator('#Grid1 .f-grid-row').count();
    console.log('     搜索 user1 后行数：' + rows);
    await page.screenshot({ path: OUT + '/05-user-search.png' });
  });
  const pages = [['职称管理', 'title'], ['职称用户管理', 'title-user'], ['部门管理', 'dept'], ['部门用户管理', 'dept-user'],
                 ['角色管理', 'role'], ['角色用户管理', 'role-user'], ['权限管理', 'power'], ['角色权限管理', 'role-power'],
                 ['菜单管理', 'menu'], ['在线统计', 'online'], ['系统配置', 'config'], ['修改密码', 'change-password']];
  for (const [text, slug] of pages) {
    await step('打开「' + text + '」', async () => {
      const before = errors.length;
      await page.locator('#treeMenu .f-tree-node', { hasText: text }).first().click();
      const frame = page.frameLocator('iframe[src*="' + slug + '"]');
      await frame.locator('.f-panel, .f-grid, .f-form').first().waitFor({ timeout: 20000 });
      await page.waitForTimeout(800);
      await page.screenshot({ path: OUT + '/page-' + slug + '.png' });
      if (errors.length > before) throw new Error('新增错误 ' + (errors.length - before) + ' 条');
    });
  }

  console.log(errors.length ? '错误：\n' + errors.join('\n') : '无控制台/HTTP 错误');
  await browser.close();
})();

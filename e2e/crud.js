// FineUI.Java.AppBox 功能冒烟：登录后走一遍角色 CRUD、用户新增/编辑/删除、角色权限保存、修改密码校验
const { chromium } = require('@playwright/test');
const BASE = process.env.BASE || 'http://127.0.0.1:8082';
const OUT = process.env.OUT || '.';
(async () => {
  const browser = await chromium.launch();
  const page = await browser.newPage({ viewport: { width: 1400, height: 900 } });
  const errors = [];
  page.on('console', m => { if (m.type() === 'error') errors.push('[console] ' + m.text()); });
  page.on('pageerror', e => errors.push('[pageerror] ' + e.message));
  page.on('response', r => { if (r.status() >= 400) errors.push('[http ' + r.status() + '] ' + r.url()); });
  const step = async (name, fn) => { try { await fn(); console.log('OK  ' + name); } catch (e) { console.log('FAIL ' + name + ': ' + e.message.split('\n').slice(0, 3).join(' | ')); await page.screenshot({ path: OUT + '/fail-' + name.replace(/[^\w一-龥]/g, '_') + '.png' }); } };
  const openMenu = async (text, slug) => {
    await page.goto(BASE + '/');
    await page.waitForSelector('#treeMenu .f-tree-node', { timeout: 15000 });
    await page.locator('#treeMenu .f-tree-node', { hasText: text }).first().click();
    const frame = page.frameLocator('iframe[src$="/admin/' + slug + '"]');
    await frame.locator('.f-grid, .f-form, .f-panel').first().waitFor({ timeout: 20000 });
    await page.waitForTimeout(600);
    return frame;
  };
  const stamp = Date.now().toString().slice(-6);

  await page.goto(BASE + '/login');
  await page.fill('#tbxUserName input', 'admin');
  await page.fill('#tbxPassword input', 'admin');
  await page.click('#btnSubmit');
  await page.waitForSelector('#treeMenu .f-tree-node', { timeout: 15000 });

  // ---- 角色：新增 → 编辑 → 删除 ----
  let frame = await openMenu('角色管理', 'role');
  const roleName = '冒烟角色' + stamp;
  await step('角色-新增', async () => {
    await frame.locator('#btnNew').click();
    const win = page.frameLocator('iframe[src*="role-new"]');
    await win.locator('#role_name input').waitFor({ timeout: 20000 });
    await win.locator('#role_name input').fill(roleName);
    await win.locator('#role_remark textarea').fill('冒烟测试');
    await win.locator('#btnSaveClose').click();
    await frame.locator('#Grid1 .f-grid-row', { hasText: roleName }).waitFor({ timeout: 15000 });
  });
  await step('角色-编辑', async () => {
    const row = frame.locator('#Grid1 .f-grid-row', { hasText: roleName });
    await row.locator('.f-grid-command').first().click();
    const win = page.frameLocator('iframe[src*="role-edit"]');
    await win.locator('#role_name input').waitFor({ timeout: 20000 });
    await win.locator('#role_name input').fill(roleName + '改');
    await win.locator('#btnSaveClose').click();
    await frame.locator('#Grid1 .f-grid-row', { hasText: roleName + '改' }).waitFor({ timeout: 15000 });
  });
  await step('角色-删除', async () => {
    const row = frame.locator('#Grid1 .f-grid-row', { hasText: roleName + '改' });
    await row.locator('.f-grid-command').nth(1).click();
    await page.locator('.f-messagebox .f-btn', { hasText: '确定' }).first().click();
    await page.waitForTimeout(1500);
    const n = await frame.locator('#Grid1 .f-grid-row', { hasText: roleName + '改' }).count();
    if (n !== 0) throw new Error('删除后仍存在');
  });

  // ---- 用户：新增（角色/职称多选 + 部门下拉树）→ 编辑 → 删除 ----
  frame = await openMenu('用户管理', 'user-list');
  const userName = 'smoke' + stamp;
  await step('用户-新增', async () => {
    await frame.locator('#btnNew').click();
    const win = page.frameLocator('iframe[src*="user-new"]');
    await win.locator('#currentUser_name input').waitFor({ timeout: 20000 });
    await win.locator('#currentUser_name input').fill(userName);
    await win.locator('#currentUser_chineseName input').fill('冒烟用户');
    await win.locator('#currentUser_gender .f-field-radiobutton-wrap', { hasText: '男' }).locator('.f-field-body-checkboxlabel').click();
    await win.locator('#currentUser_password input').fill('123456');
    await win.locator('#currentUser_email input').fill(userName + '@test.com');
    await win.locator('#ddbRoles .f-triggerbox-trigger2').click();
    await win.locator('#cblRoles .f-field-checkbox-wrap').first().locator('.f-field-body-checkboxlabel').click();
    await win.locator('#ddbRoles .f-triggerbox-trigger2').click();
    await page.waitForTimeout(400);
    await win.locator('#btnSaveClose').click();
    await frame.locator('#ttbSearchMessage input').fill(userName);
    await frame.locator('#ttbSearchMessage input').press('Enter');
    await frame.locator('#Grid1 .f-grid-row', { hasText: userName }).waitFor({ timeout: 15000 });
    await page.screenshot({ path: OUT + '/crud-user-created.png' });
  });
  await step('用户-编辑', async () => {
    const row = frame.locator('#Grid1 .f-grid-row', { hasText: userName });
    await row.locator('.f-grid-command').nth(2).click();
    const win = page.frameLocator('iframe[src*="user-edit"]');
    await win.locator('#currentUser_chineseName input').waitFor({ timeout: 20000 });
    await win.locator('#currentUser_chineseName input').fill('冒烟用户改');
    await win.locator('#btnSaveClose').click();
    await frame.locator('#Grid1 .f-grid-row', { hasText: '冒烟用户改' }).waitFor({ timeout: 15000 });
  });
  await step('用户-删除', async () => {
    const row = frame.locator('#Grid1 .f-grid-row', { hasText: userName });
    await row.locator('.f-grid-command').nth(3).click();
    await page.locator('.f-messagebox .f-btn', { hasText: '确定' }).first().click();
    await page.waitForTimeout(1500);
    const n = await frame.locator('#Grid1 .f-grid-row', { hasText: userName }).count();
    if (n !== 0) throw new Error('删除后仍存在');
  });

  // ---- 角色权限：勾一个权限保存 ----
  frame = await openMenu('角色权限管理', 'role-power');
  await step('角色权限-保存', async () => {
    await frame.locator('#Grid1 .f-grid-row', { hasText: '开发人员' }).click();
    await page.waitForTimeout(800);
    const cb = frame.locator('#power_checkbox_1');
    await cb.check();
    await frame.locator('#btnGroupUpdate').click();
    await page.locator('.f-messagebox', { hasText: '更新成功' }).waitFor({ timeout: 15000 });
    await page.locator('.f-messagebox .f-btn').first().click();
  });

  // ---- 修改密码：当前密码错 → 红框提示 ----
  frame = await openMenu('修改密码', 'change-password');
  await step('修改密码-旧密码错误提示', async () => {
    await frame.locator('#tbxOldPassword input').fill('wrong');
    await frame.locator('#tbxNewPassword input').fill('123456');
    await frame.locator('#tbxConfirmNewPassword input').fill('123456');
    // 文本框的延迟 change 校验会清掉字段的无效标记：等它先触发，再点保存（真人操作天然有这段间隔）
    await page.waitForTimeout(800);
    await frame.locator('#btnSave').click();
    await page.waitForTimeout(1500);
    // markInvalid 在字段旁的错误图标上挂提示文案（data-qtip），以此判定服务端校验提示已到达
    const invalidCount = await frame.locator('#tbxOldPassword [data-qtip*="当前密码不正确"]').count();
    console.log('     tbxOldPassword error markers: ' + invalidCount);
    if (invalidCount === 0) throw new Error('未显示「当前密码不正确」提示');
  });

  console.log(errors.length ? '错误：\n' + errors.join('\n') : '无控制台/HTTP 错误');
  await browser.close();
})();

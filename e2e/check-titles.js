// 逐页点开弹窗列命令，断言窗体标题栏文本非空且与 FineUI.Core.AppBox 逐字一致
const { chromium } = require('@playwright/test');
const BASE = process.env.BASE || 'http://127.0.0.1:8082';

// 页面路由 → [命令 tooltip, 期望标题]
const CASES = [
    // [路由, [[命令 tooltip, 期望标题], …], 打开弹窗前要先点的左侧节点/行（主从页需先选中才出右表）]
    ['/admin/dept',       [['编辑', '编辑']]],
    ['/admin/menu',       [['编辑', '编辑']]],
    ['/admin/power',      [['编辑', '编辑']]],
    ['/admin/role',       [['编辑', '编辑']]],
    ['/admin/title',      [['编辑', '编辑']]],
    ['/admin/online',     [['查看', '查看用户信息']]],
    ['/admin/dept-user',  [['查看用户信息', '查看用户信息']], '#Grid1 .f-grid-row'],
    ['/admin/role-user',  [['查看用户信息', '查看用户信息']]],
    ['/admin/title-user', [['查看用户信息', '查看用户信息']], '#Grid1 .f-grid-row'],
    ['/admin/user-list',  [['查看用户信息', '查看用户信息'], ['修改密码', '修改密码'], ['编辑', '编辑']]],
];

let fail = 0;
const ok = (m) => console.log('OK  ' + m);
const bad = (m) => { fail++; console.log('FAIL ' + m); };

(async () => {
    const browser = await chromium.launch();
    const page = await browser.newPage();

    await page.goto(BASE + '/login');
    await page.waitForSelector('#tbxUserName input', { timeout: 15000 });
    await page.fill('#tbxUserName input', 'admin');
    await page.fill('#tbxPassword input', 'admin');
    await page.click('#btnSubmit');
    await page.waitForURL(u => !/\/login/.test(u.toString()), { timeout: 15000 });

    for (const [route, cmds, setup] of CASES) {
        for (const [tip, expected] of cmds) {
            await page.goto(BASE + route);
            await page.waitForTimeout(900);
            if (setup) {
                // 主从页：右表要先选中左表某行才有数据；靠前的部门/职称可能一个用户都没有，逐行试到出命令为止
                const rows = page.locator(setup);
                const total = await rows.count();
                for (let i = 0; i < total; i++) {
                    await rows.nth(i).click();
                    await page.waitForTimeout(1000);
                    if (await page.locator(`[data-qtip="${tip}"]`).count()) break;
                }
            }
            const cell = page.locator(`[data-qtip="${tip}"], [title="${tip}"]`).first();
            if (await cell.count() === 0) { bad(`${route} 找不到命令「${tip}」`); continue; }
            await cell.click();
            await page.waitForTimeout(1200);
            // 弹窗标题栏：可见的 f-window 的标题元素
            const title = await page.evaluate(() => {
                const wins = [...document.querySelectorAll('.f-window')].filter(w => w.offsetParent !== null);
                if (!wins.length) return '__NO_WINDOW__';
                const t = wins[wins.length - 1].querySelector('.f-panel-title-text');
                return t ? t.textContent.replace(/ /g, '').trim() : '__NO_TITLE_EL__';
            });
            if (title === expected) ok(`${route} 「${tip}」→ 标题「${title}」`);
            else bad(`${route} 「${tip}」→ 标题「${title}」，期望「${expected}」`);
        }
    }
    await browser.close();
    console.log(fail === 0 ? '\n全部通过' : `\n${fail} 项失败`);
    process.exit(fail === 0 ? 0 : 1);
})();

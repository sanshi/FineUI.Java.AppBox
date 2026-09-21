package com.fineui.java.appbox.pages.pub;

import com.fineui.java.appbox.business.PageBase;
import com.fineui.java.appbox.business.AuthService;
import com.fineui.java.core.EventArgs;
import com.fineui.java.core.FineUIPage;
import com.fineui.java.core.controls.Grid;
import com.fineui.java.core.controls.Label;
import com.fineui.java.core.controls.TwinTriggerBox;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 通知公告（路由 {@code public/notice}）：**无需登录**即可访问的公开页示例。
 *
 * <p>能匿名访问靠的是安全配置里放行了 {@code /public/**} 这一条路径规则，页面自身不需要任何声明。
 * 两件事因此成立：
 * <ul>
 *   <li>本类不标 {@code @CheckPower}——那个注解管的是「登录用户有没有这一页的权限」，与「要不要登录」无关；</li>
 *   <li>本类继承 {@link PageBase} 而不是后台功能页基类，公开页不加当前用户水印。</li>
 * </ul>
 *
 * <p>页面里的搜索与翻页都是回发，能正常工作说明匿名会话下的回发通道是通的——放行的那条路径规则
 * 同时覆盖首屏 GET 与回发 POST（回发请求发往页面自身的地址）。
 *
 * <p>读当前用户必须判空：匿名访问时没有身份。本页据此在右上角显示不同的入口文字。
 */
@FineUIPage("public/notice")
public class NoticeModel extends PageBase {

    Grid Grid1;
    TwinTriggerBox ttbSearchTitle;
    Label labSignedIn;

    private List<Notice> notices;

    public List<Notice> getNotices() {
        return notices;
    }

    public void Page_Load(Object sender, EventArgs e) {
        if (!isPostBack()) {
            // 匿名访问时 currentUser() 为 null，取任何用户信息之前都要判空
            labSignedIn.setText(AuthService.currentUser() == null
                    ? "您当前未登录，可直接浏览公告"
                    : "您已登录为 " + AuthService.currentUser().getName());

            loadData();
        }
    }

    private void loadData() {
        String searchText = ttbSearchTitle.getValue().trim().toLowerCase(Locale.ROOT);
        List<Notice> filtered = new ArrayList<>();
        for (Notice notice : NoticeData.all()) {
            if (searchText.isEmpty() || notice.getTitle().toLowerCase(Locale.ROOT).contains(searchText)) {
                filtered.add(notice);
            }
        }

        // 数据在内存里，仍走与其它列表页相同的分页基建（越界回退、总数回写都在里面）
        Page<Notice> page = loadPage(Grid1, pageable -> {
            int from = (int) pageable.getOffset();
            if (from >= filtered.size()) {
                return new PageImpl<>(List.of(), pageable, filtered.size());
            }
            int to = Math.min(from + pageable.getPageSize(), filtered.size());
            return new PageImpl<>(filtered.subList(from, to), pageable, filtered.size());
        });

        notices = page.getContent();
        Grid1.setDataSource(notices);
        Grid1.dataBind();
    }

    public void Grid1_PageIndexChanged(Object sender, EventArgs e) {
        loadData();
    }

    public void ttbSearchTitle_Trigger1Click(Object sender, EventArgs e) {
        ttbSearchTitle.setValue("");
        ttbSearchTitle.setShowTrigger1(false);
        Grid1.setPageIndex(0);
        loadData();
    }

    public void ttbSearchTitle_Trigger2Click(Object sender, EventArgs e) {
        ttbSearchTitle.setShowTrigger1(true);
        Grid1.setPageIndex(0);
        loadData();
    }
}

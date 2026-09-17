package com.fineui.java.appbox.pages.pub;

import com.fineui.java.appbox.business.AppBoxPageBase;
import com.fineui.java.core.AbortPageException;
import com.fineui.java.core.ActiveWindow;
import com.fineui.java.core.EventArgs;
import com.fineui.java.core.FineUIPage;
import com.fineui.java.core.controls.Label;

/**
 * 公告详情（路由 {@code public/notice-detail}，弹窗内打开）：同样无需登录。
 *
 * <p>它落在放行的 {@code /public/**} 路径下，所以既能被列表页弹窗打开，也能把地址直接发给别人打开
 * （例如 {@code /public/notice-detail?id=8}）——这正是「公告链接可以往外发」想要的效果。
 *
 * <p>参数取值仍要当作不可信输入：{@code getQueryInt} 解析失败给默认值，查不到记录就中止首屏。
 */
@FineUIPage("public/notice-detail")
public class NoticeDetailModel extends AppBoxPageBase {

    Label labTitle;
    Label labPublishTime;
    Label labDepartment;
    Label labContent;

    private Notice notice;

    public Notice getNotice() {
        return notice;
    }

    public void Page_Get(Object sender, EventArgs e) {
        notice = NoticeData.find(getQueryInt("id", 0));
        if (notice == null) {
            throw new AbortPageException("公告不存在或已撤回！", ActiveWindow.hideReference());
        }
    }

    public void Page_Load(Object sender, EventArgs e) {
        if (!isPostBack()) {
            labTitle.setText(notice.getTitle());
            labPublishTime.setText(notice.getPublishTime().format(
                    java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            labDepartment.setText(notice.getDepartment());
            labContent.setText(notice.getContent());
        }
    }
}

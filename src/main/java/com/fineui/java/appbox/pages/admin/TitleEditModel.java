package com.fineui.java.appbox.pages.admin;

import com.fineui.java.appbox.business.AdminPageBase;
import com.fineui.java.appbox.business.CheckPower;
import com.fineui.java.appbox.model.Title;
import com.fineui.java.appbox.repository.TitleRepository;
import com.fineui.java.binding.BindProperty;
import com.fineui.java.core.PageContext;
import com.fineui.java.core.ActiveWindow;
import com.fineui.java.core.EventArgs;
import com.fineui.java.core.FineUIPage;

/** 编辑职称（路由 {@code admin/title-edit}，弹窗内打开）：首屏按 {@code ?id} 加载回显，保存时只覆盖本次查询实体中的表单字段。 */
@FineUIPage("admin/title-edit")
@CheckPower("CoreTitleEdit")
public class TitleEditModel extends AdminPageBase {

    private int titleId;

    @BindProperty
    private Title title;

    /** 本次按 URL 查询并校验的保存目标；不接受表单或隐藏字段决定主键。 */
    private Title storedTitle;

    public Title getTitle() {
        return title;
    }

    private final TitleRepository titleRepository;

    public TitleEditModel(TitleRepository titleRepository) {
        this.titleRepository = titleRepository;
    }

    public void Page_Init(Object sender, EventArgs e) {
        titleId = getQueryIntValue("id");
        storedTitle = titleRepository.findById(titleId).orElse(null);
        if (storedTitle == null) {
            PageContext.abortPage("无效参数！", ActiveWindow.hideCommand());
            return;
        }
        if (!isPostBack()) {
            title = storedTitle;
        }
    }

    public void btnSaveClose_Click(Object sender, EventArgs e) {
        if (!getModelState().isValid()) {
            return;
        }
        storedTitle.setName(title.getName());
        storedTitle.setRemark(title.getRemark());
        titleRepository.save(storedTitle);
        ActiveWindow.hidePostBack();
    }
}

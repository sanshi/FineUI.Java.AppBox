package com.fineui.java.appbox.pages.admin;

import com.fineui.java.appbox.business.AdminPageBase;
import com.fineui.java.appbox.business.CheckPower;
import com.fineui.java.appbox.model.Title;
import com.fineui.java.appbox.repository.TitleRepository;
import com.fineui.java.binding.BindProperty;
import com.fineui.java.binding.HiddenProperty;
import com.fineui.java.core.AbortPageException;
import com.fineui.java.core.ActiveWindow;
import com.fineui.java.core.EventArgs;
import com.fineui.java.core.FineUIPage;

/** 编辑职称（路由 {@code admin/title-edit}，弹窗内打开）：首屏按 {@code ?id} 加载回显，保存时按主键重读后只覆盖表单字段。 */
@FineUIPage("admin/title-edit")
@CheckPower("CoreTitleEdit")
public class TitleEditModel extends AdminPageBase {

    @HiddenProperty
    private int titleId;

    @BindProperty
    private Title title;

    public Title getTitle() {
        return title;
    }

    private final TitleRepository titleRepository;

    public TitleEditModel(TitleRepository titleRepository) {
        this.titleRepository = titleRepository;
    }

    public void Page_Get(Object sender, EventArgs e) {
        titleId = getQueryInt("id", 0);
        title = titleRepository.findById(titleId).orElse(null);
        if (title == null) {
            throw new AbortPageException("无效参数！", ActiveWindow.hideReference());
        }
    }

    public void btnSaveClose_Click(Object sender, EventArgs e) {
        if (!getModelState().isValid()) {
            return;
        }
        Title stored = titleRepository.findById(titleId).orElse(null);
        if (stored == null) {
            showAlert("该职称不存在或已被删除！");
            return;
        }
        stored.setName(title.getName());
        stored.setRemark(title.getRemark());
        titleRepository.save(stored);
        ActiveWindow.hidePostBack();
    }
}

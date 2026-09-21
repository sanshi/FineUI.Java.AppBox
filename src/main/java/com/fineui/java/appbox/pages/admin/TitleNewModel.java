package com.fineui.java.appbox.pages.admin;

import com.fineui.java.appbox.business.AdminPageBase;
import com.fineui.java.appbox.business.CheckPower;
import com.fineui.java.appbox.model.Title;
import com.fineui.java.appbox.repository.TitleRepository;
import com.fineui.java.binding.BindProperty;
import com.fineui.java.core.ActiveWindow;
import com.fineui.java.core.EventArgs;
import com.fineui.java.core.FineUIPage;

/** 新增职称（路由 {@code admin/title-new}，弹窗内打开）。 */
@FineUIPage("admin/title-new")
@CheckPower("CoreTitleNew")
public class TitleNewModel extends AdminPageBase {

    @BindProperty
    private Title title;

    public Title getTitle() {
        return title;
    }

    private final TitleRepository titleRepository;

    public TitleNewModel(TitleRepository titleRepository) {
        this.titleRepository = titleRepository;
    }

    public void btnSaveClose_Click(Object sender, EventArgs e) {
        if (!getModelState().isValid()) {
            return;
        }
        titleRepository.save(title);
        // 关闭本窗体（触发父页窗体的关闭事件以刷新列表）
        ActiveWindow.hidePostBack();
    }
}

package com.fineui.java.appbox.pages.admin;

import com.fineui.java.appbox.business.AppBoxAdminPageBase;
import com.fineui.java.appbox.business.AuthService;
import com.fineui.java.appbox.business.CheckPower;
import com.fineui.java.appbox.model.Power;
import com.fineui.java.appbox.repository.PowerRepository;
import com.fineui.java.binding.BindProperty;
import com.fineui.java.core.ActiveWindow;
import com.fineui.java.core.EventArgs;
import com.fineui.java.core.FineUIPage;

/** 新增权限（路由 {@code admin/power-new}，弹窗内打开）。 */
@FineUIPage("admin/power-new")
@CheckPower("CorePowerNew")
public class PowerNewModel extends AppBoxAdminPageBase {

    @BindProperty
    private Power power;

    public Power getPower() {
        return power;
    }

    private final PowerRepository powerRepository;

    public PowerNewModel(PowerRepository powerRepository) {
        this.powerRepository = powerRepository;
    }

    public void btnSaveClose_Click(Object sender, EventArgs e) {
        if (!getModelState().isValid()) {
            return;
        }
        powerRepository.save(power);
        // 超级管理员的权限视图是「权限表全集」：新增了一个权限，它的缓存也要作废，
        // 否则新权限守卫的页面要等到会话结束才进得去
        AuthService.invalidatePermissionCaches();
        // 关闭本窗体（触发父页窗体的关闭事件以刷新列表）
        ActiveWindow.hidePostBack();
    }
}

package com.fineui.java.appbox.pages.admin;

import com.fineui.java.appbox.business.AppBoxAdminPageBase;
import com.fineui.java.appbox.business.AuthService;
import com.fineui.java.appbox.business.CheckPower;
import com.fineui.java.appbox.model.Power;
import com.fineui.java.appbox.repository.PowerRepository;
import com.fineui.java.binding.BindProperty;
import com.fineui.java.binding.HiddenProperty;
import com.fineui.java.core.AbortPageException;
import com.fineui.java.core.ActiveWindow;
import com.fineui.java.core.EventArgs;
import com.fineui.java.core.FineUIPage;

/** 编辑权限（路由 {@code admin/power-edit}，弹窗内打开）：首屏按 {@code ?id} 加载回显，保存时按主键重读后只覆盖表单字段。 */
@FineUIPage("admin/power-edit")
@CheckPower("CorePowerEdit")
public class PowerEditModel extends AppBoxAdminPageBase {

    @HiddenProperty
    private int powerId;

    @BindProperty
    private Power power;

    public Power getPower() {
        return power;
    }

    private final PowerRepository powerRepository;

    public PowerEditModel(PowerRepository powerRepository) {
        this.powerRepository = powerRepository;
    }

    public void Page_Get(Object sender, EventArgs e) {
        powerId = getQueryInt("id", 0);
        power = powerRepository.findById(powerId).orElse(null);
        if (power == null) {
            throw new AbortPageException("无效参数！", ActiveWindow.hideReference());
        }
    }

    public void btnSaveClose_Click(Object sender, EventArgs e) {
        if (!getModelState().isValid()) {
            return;
        }
        Power stored = powerRepository.findById(powerId).orElse(null);
        if (stored == null) {
            showAlert("该权限不存在或已被删除！");
            return;
        }
        stored.setGroupName(power.getGroupName());
        stored.setName(power.getName());
        stored.setTitle(power.getTitle());
        stored.setRemark(power.getRemark());
        powerRepository.save(stored);
        AuthService.invalidatePermissionCaches();   // 权限按名判定：改名后各会话要重新解析
        ActiveWindow.hidePostBack();
    }
}

package com.fineui.java.appbox.pages.admin;

import com.fineui.java.appbox.business.AdminPageBase;
import com.fineui.java.appbox.business.AuthService;
import com.fineui.java.appbox.business.CheckPower;
import com.fineui.java.appbox.model.Power;
import com.fineui.java.appbox.repository.PowerRepository;
import com.fineui.java.binding.BindProperty;
import com.fineui.java.core.PageContext;
import com.fineui.java.core.ActiveWindow;
import com.fineui.java.core.EventArgs;
import com.fineui.java.core.FineUIPage;

/** 编辑权限（路由 {@code admin/power-edit}，弹窗内打开）：首屏按 {@code ?id} 加载回显，保存时只覆盖本次查询实体中的表单字段。 */
@FineUIPage("admin/power-edit")
@CheckPower("CorePowerEdit")
public class PowerEditModel extends AdminPageBase {

    private int powerId;

    @BindProperty
    private Power power;

    /** 本次按 URL 查询并校验的保存目标；不接受表单或隐藏字段决定主键。 */
    private Power storedPower;

    public Power getPower() {
        return power;
    }

    private final PowerRepository powerRepository;

    public PowerEditModel(PowerRepository powerRepository) {
        this.powerRepository = powerRepository;
    }

    public void Page_Init(Object sender, EventArgs e) {
        powerId = getQueryInt("id", 0);
        storedPower = powerRepository.findById(powerId).orElse(null);
        if (storedPower == null) {
            PageContext.abortPage("无效参数！", ActiveWindow.hideCommand());
            return;
        }
        if (!isPostBack()) {
            power = storedPower;
        }
    }

    public void btnSaveClose_Click(Object sender, EventArgs e) {
        if (!getModelState().isValid()) {
            return;
        }
        storedPower.setGroupName(power.getGroupName());
        storedPower.setName(power.getName());
        storedPower.setTitle(power.getTitle());
        storedPower.setRemark(power.getRemark());
        powerRepository.save(storedPower);
        AuthService.invalidatePermissionCaches();   // 权限按名判定：改名后各会话要重新解析
        ActiveWindow.hidePostBack();
    }
}

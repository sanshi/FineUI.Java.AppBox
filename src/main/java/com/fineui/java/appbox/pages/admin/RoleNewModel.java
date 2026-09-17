package com.fineui.java.appbox.pages.admin;

import com.fineui.java.appbox.business.AppBoxAdminPageBase;
import com.fineui.java.appbox.business.CheckPower;
import com.fineui.java.appbox.model.Role;
import com.fineui.java.appbox.repository.RoleRepository;
import com.fineui.java.binding.BindProperty;
import com.fineui.java.core.ActiveWindow;
import com.fineui.java.core.EventArgs;
import com.fineui.java.core.FineUIPage;

/** 新增角色（路由 {@code admin/role-new}，弹窗内打开）。 */
@FineUIPage("admin/role-new")
@CheckPower("CoreRoleNew")
public class RoleNewModel extends AppBoxAdminPageBase {

    @BindProperty
    private Role role;

    public Role getRole() {
        return role;
    }

    private final RoleRepository roleRepository;

    public RoleNewModel(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    public void btnSaveClose_Click(Object sender, EventArgs e) {
        if (!getModelState().isValid()) {
            return;
        }
        roleRepository.save(role);
        // 关闭本窗体（触发父页窗体的关闭事件以刷新列表）
        ActiveWindow.hidePostBack();
    }
}

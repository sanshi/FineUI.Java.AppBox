package com.fineui.java.appbox.pages.admin;

import com.fineui.java.appbox.business.AppBoxAdminPageBase;
import com.fineui.java.appbox.business.AuthService;
import com.fineui.java.appbox.business.CheckPower;
import com.fineui.java.appbox.model.Role;
import com.fineui.java.appbox.repository.RoleRepository;
import com.fineui.java.binding.BindProperty;
import com.fineui.java.binding.HiddenProperty;
import com.fineui.java.core.AbortPageException;
import com.fineui.java.core.ActiveWindow;
import com.fineui.java.core.EventArgs;
import com.fineui.java.core.FineUIPage;

/** 编辑角色（路由 {@code admin/role-edit}，弹窗内打开）：首屏按 {@code ?id} 加载回显，保存时按主键重读后只覆盖表单字段。 */
@FineUIPage("admin/role-edit")
@CheckPower("CoreRoleEdit")
public class RoleEditModel extends AppBoxAdminPageBase {

    @HiddenProperty
    private int roleId;

    @BindProperty
    private Role role;

    public Role getRole() {
        return role;
    }

    private final RoleRepository roleRepository;

    public RoleEditModel(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    public void Page_Get(Object sender, EventArgs e) {
        roleId = getQueryInt("id", 0);
        role = roleRepository.findById(roleId).orElse(null);
        if (role == null) {
            throw new AbortPageException("无效参数！", ActiveWindow.hideReference());
        }
    }

    public void btnSaveClose_Click(Object sender, EventArgs e) {
        if (!getModelState().isValid()) {
            return;
        }
        Role stored = roleRepository.findById(roleId).orElse(null);
        if (stored == null) {
            showAlert("该角色不存在或已被删除！");
            return;
        }
        stored.setName(role.getName());
        stored.setRemark(role.getRemark());
        roleRepository.save(stored);
        AuthService.invalidatePermissionCaches();   // 角色名变了，按名判定的地方要重新解析
        ActiveWindow.hidePostBack();
    }
}

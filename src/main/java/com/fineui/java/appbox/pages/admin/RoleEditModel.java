package com.fineui.java.appbox.pages.admin;

import com.fineui.java.appbox.business.AdminPageBase;
import com.fineui.java.appbox.business.AuthService;
import com.fineui.java.appbox.business.CheckPower;
import com.fineui.java.appbox.model.Role;
import com.fineui.java.appbox.repository.RoleRepository;
import com.fineui.java.binding.BindProperty;
import com.fineui.java.core.PageContext;
import com.fineui.java.core.ActiveWindow;
import com.fineui.java.core.EventArgs;
import com.fineui.java.core.FineUIPage;

/** 编辑角色（路由 {@code admin/role-edit}，弹窗内打开）：首屏按 {@code ?id} 加载回显，保存时只覆盖本次查询实体中的表单字段。 */
@FineUIPage("admin/role-edit")
@CheckPower("CoreRoleEdit")
public class RoleEditModel extends AdminPageBase {

    private int roleId;

    @BindProperty
    private Role role;

    /** 本次按 URL 查询并校验的保存目标；不接受表单或隐藏字段决定主键。 */
    private Role storedRole;

    public Role getRole() {
        return role;
    }

    private final RoleRepository roleRepository;

    public RoleEditModel(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    public void Page_Init(Object sender, EventArgs e) {
        roleId = getQueryIntValue("id");
        storedRole = roleRepository.findById(roleId).orElse(null);
        if (storedRole == null) {
            PageContext.abortPage("无效参数！", ActiveWindow.hideCommand());
            return;
        }
        if (!isPostBack()) {
            role = storedRole;
        }
    }

    public void btnSaveClose_Click(Object sender, EventArgs e) {
        if (!getModelState().isValid()) {
            return;
        }
        storedRole.setName(role.getName());
        storedRole.setRemark(role.getRemark());
        roleRepository.save(storedRole);
        AuthService.invalidatePermissionCaches();   // 角色名变了，按名判定的地方要重新解析
        ActiveWindow.hidePostBack();
    }
}

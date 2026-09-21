package com.fineui.java.appbox.pages.admin;

import com.fineui.java.appbox.business.AdminPageBase;
import com.fineui.java.appbox.business.AuthService;
import com.fineui.java.appbox.business.CheckPower;
import com.fineui.java.appbox.model.Role;
import com.fineui.java.appbox.repository.RoleRepository;
import com.fineui.java.appbox.repository.UserRepository;
import com.fineui.java.core.EventArgs;
import com.fineui.java.core.FineUIPage;
import com.fineui.java.core.GridCommandEventArgs;
import com.fineui.java.core.MessageBoxIcon;
import com.fineui.java.core.controls.Button;
import com.fineui.java.core.controls.DropDownList;
import com.fineui.java.core.controls.Grid;
import com.fineui.java.core.controls.TwinTriggerBox;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

/** 角色列表（路由 {@code admin/role}）：搜索 + 数据库分页排序 + 每页条数切换 + 行内编辑/删除（弹窗）。 */
@FineUIPage("admin/role")
@CheckPower("CoreRoleView")
public class RoleModel extends AdminPageBase {

    Grid Grid1;
    TwinTriggerBox ttbSearchMessage;
    DropDownList ddlGridPageSize;
    Button btnNew;

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;

    private List<Role> roles;

    public List<Role> getRoles() {
        return roles;
    }

    public RoleModel(RoleRepository roleRepository, UserRepository userRepository) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
    }

    public void Page_Load(Object sender, EventArgs e) {
        if (!isPostBack()) {
            // 根据用户权限控制页面控件的可用状态
            btnNew.setEnabled(checkPower("CoreRoleNew"));
            Grid1.findCommand("Edit").setEnabled(checkPower("CoreRoleEdit"));
            Grid1.findCommand("Delete").setEnabled(checkPower("CoreRoleDelete"));

            ddlGridPageSize.setSelectedValue(String.valueOf(configService.getPageSize()));
            Grid1.setPageSize(configService.getPageSize());

            loadData();
        }
    }

    private void loadData() {
        String searchText = ttbSearchMessage.getValue().trim();
        Specification<Role> spec = (root, query, cb) -> searchText.isEmpty()
                ? cb.conjunction()
                : cb.like(root.get("name"), "%" + searchText + "%");

        Page<Role> page = loadPage(Grid1, pageable -> roleRepository.findAll(spec, pageable));
        roles = page.getContent();
        Grid1.setDataSource(roles);
        Grid1.dataBind();
    }

    public void Window1_Close(Object sender, EventArgs e) {
        loadData();
    }

    public void Grid1_Sort(Object sender, EventArgs e) {
        loadData();
    }

    public void Grid1_PageIndexChanged(Object sender, EventArgs e) {
        loadData();
    }

    public void ddlGridPageSize_SelectedIndexChanged(Object sender, EventArgs e) {
        Grid1.setPageSize(intOrDefault(ddlGridPageSize.getSelectedValue(), configService.getPageSize()));
        loadData();
    }

    public void ttbSearchMessage_Trigger1Click(Object sender, EventArgs e) {
        ttbSearchMessage.setValue("");
        ttbSearchMessage.setShowTrigger1(false);
        loadData();
    }

    public void ttbSearchMessage_Trigger2Click(Object sender, EventArgs e) {
        ttbSearchMessage.setShowTrigger1(true);
        loadData();
    }

    public void Grid1_RowCommand(Object sender, GridCommandEventArgs e) {
        if ("Delete".equals(e.getCommandName())) {
            Integer rowId = getRowId(e);
            if (rowId == null) {
                return;
            }
            // 在操作之前进行权限检查
            if (!checkPower("CoreRoleDelete")) {
                checkPowerFailWithAlert();
                return;
            }
            if (userRepository.countByRolesId(rowId) > 0) {
                showAlertInTop("删除失败！需要先清空属于此角色的用户！", "", MessageBoxIcon.Warning);
                return;
            }
            roleRepository.deleteById(rowId);
            AuthService.invalidatePermissionCaches();   // 角色没了，其成员的权限视图变了
            loadData();
        }
    }
}

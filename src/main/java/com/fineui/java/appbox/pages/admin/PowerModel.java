package com.fineui.java.appbox.pages.admin;

import com.fineui.java.appbox.business.AppBoxAdminPageBase;
import com.fineui.java.appbox.business.AuthService;
import com.fineui.java.appbox.business.CheckPower;
import com.fineui.java.appbox.model.Power;
import com.fineui.java.appbox.repository.PowerRepository;
import com.fineui.java.appbox.repository.MenuRepository;
import com.fineui.java.appbox.repository.RoleRepository;
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

/** 权限列表（路由 {@code admin/power}）：搜索 + 数据库分页排序 + 每页条数切换 + 行内编辑/删除（弹窗）。 */
@FineUIPage("admin/power")
@CheckPower("CorePowerView")
public class PowerModel extends AppBoxAdminPageBase {

    Grid Grid1;
    TwinTriggerBox ttbSearchMessage;
    DropDownList ddlGridPageSize;
    Button btnNew;

    private final PowerRepository powerRepository;
    private final RoleRepository roleRepository;
    private final MenuRepository menuRepository;

    private List<Power> powers;

    public List<Power> getPowers() {
        return powers;
    }

    public PowerModel(PowerRepository powerRepository, RoleRepository roleRepository, MenuRepository menuRepository) {
        this.powerRepository = powerRepository;
        this.roleRepository = roleRepository;
        this.menuRepository = menuRepository;
    }

    public void Page_Load(Object sender, EventArgs e) {
        if (!isPostBack()) {
            // 根据用户权限控制页面控件的可用状态
            btnNew.setEnabled(checkPower("CorePowerNew"));
            Grid1.findCommand("Edit").setEnabled(checkPower("CorePowerEdit"));
            Grid1.findCommand("Delete").setEnabled(checkPower("CorePowerDelete"));

            ddlGridPageSize.setSelectedValue(String.valueOf(configService.getPageSize()));
            Grid1.setPageSize(configService.getPageSize());

            loadData();
        }
    }

    private void loadData() {
        String searchText = ttbSearchMessage.getValue().trim();
        Specification<Power> spec = (root, query, cb) -> searchText.isEmpty()
                ? cb.conjunction()
                : cb.or(cb.like(root.get("name"), "%" + searchText + "%"),
                        cb.like(root.get("title"), "%" + searchText + "%"));

        Page<Power> page = loadPage(Grid1, pageable -> powerRepository.findAll(spec, pageable));
        powers = page.getContent();
        Grid1.setDataSource(powers);
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
            if (!checkPower("CorePowerDelete")) {
                checkPowerFailWithAlert();
                return;
            }
            if (menuRepository.countByViewPowerId(rowId) > 0) {
                showAlertInTop("删除失败！需要先清空引用此权限的菜单！", "", MessageBoxIcon.Warning);
                return;
            }
            if (roleRepository.countByPowersId(rowId) > 0) {
                showAlertInTop("删除失败！需要先清空使用此权限的角色！", "", MessageBoxIcon.Warning);
                return;
            }
            powerRepository.deleteById(rowId);
            AuthService.invalidatePermissionCaches();   // 权限没了，引用它的角色成员权限视图变了
            loadData();
        }
    }
}

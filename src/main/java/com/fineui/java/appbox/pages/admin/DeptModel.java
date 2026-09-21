package com.fineui.java.appbox.pages.admin;

import com.fineui.java.appbox.business.AdminPageBase;
import com.fineui.java.appbox.business.CheckPower;
import com.fineui.java.appbox.model.Dept;
import com.fineui.java.appbox.repository.DeptRepository;
import com.fineui.java.appbox.repository.UserRepository;
import com.fineui.java.core.EventArgs;
import com.fineui.java.core.FineUIPage;
import com.fineui.java.core.GridCommandEventArgs;
import com.fineui.java.core.MessageBoxIcon;
import com.fineui.java.core.controls.Button;
import com.fineui.java.core.controls.Grid;

import java.util.List;

/** 部门管理（路由 {@code admin/dept}）：树形表格展示全部部门，行内编辑/删除（弹窗）；有下级部门或有用户的部门不能删。 */
@FineUIPage("admin/dept")
@CheckPower("CoreDeptView")
public class DeptModel extends AdminPageBase {

    Grid Grid1;
    Button btnNew;

    private final DeptRepository deptRepository;
    private final UserRepository userRepository;

    private List<Dept> depts;

    public List<Dept> getDepts() {
        return depts;
    }

    public DeptModel(DeptRepository deptRepository, UserRepository userRepository) {
        this.deptRepository = deptRepository;
        this.userRepository = userRepository;
    }

    public void Page_Load(Object sender, EventArgs e) {
        if (!isPostBack()) {
            // 根据用户权限控制页面控件的可用状态
            btnNew.setEnabled(checkPower("CoreDeptNew"));
            Grid1.findCommand("Edit").setEnabled(checkPower("CoreDeptEdit"));
            Grid1.findCommand("Delete").setEnabled(checkPower("CoreDeptDelete"));
            loadData();
        }
    }

    private void loadData() {
        depts = deptRepository.findAllByOrderBySortIndexAsc();
        Grid1.setDataSource(depts);
        Grid1.dataBind();
    }

    public void Window1_Close(Object sender, EventArgs e) {
        loadData();
    }

    public void Grid1_RowCommand(Object sender, GridCommandEventArgs e) {
        if ("Delete".equals(e.getCommandName())) {
            Integer rowId = getRowId(e);
            if (rowId == null) {
                return;
            }
            // 在操作之前进行权限检查
            if (!checkPower("CoreDeptDelete")) {
                checkPowerFailWithAlert();
                return;
            }
            if (userRepository.countByDeptId(rowId) > 0) {
                showAlertInTop("删除失败！需要先清空属于此部门的用户！", "", MessageBoxIcon.Warning);
                return;
            }
            if (!deptRepository.findByParentIdOrderBySortIndexAsc(rowId).isEmpty()) {
                showAlertInTop("删除失败！请先删除子部门！", "", MessageBoxIcon.Warning);
                return;
            }
            deptRepository.deleteById(rowId);
            loadData();
        }
    }
}

package com.fineui.java.appbox.pages.admin;

import com.fineui.java.appbox.business.AppBoxAdminPageBase;
import com.fineui.java.appbox.business.CheckPower;
import com.fineui.java.appbox.model.Dept;
import com.fineui.java.appbox.repository.DeptRepository;
import com.fineui.java.binding.BindProperty;
import com.fineui.java.core.ActiveWindow;
import com.fineui.java.core.EventArgs;
import com.fineui.java.core.FineUIPage;
import com.fineui.java.core.controls.DropDownBox;
import com.fineui.java.core.controls.Grid;

/** 新增部门（路由 {@code admin/dept-new}，弹窗内打开）：上级部门从下拉树表格中选择。 */
@FineUIPage("admin/dept-new")
@CheckPower("CoreDeptNew")
public class DeptNewModel extends AppBoxAdminPageBase {

    DropDownBox ddbParent;
    Grid Grid1;

    @BindProperty
    private Dept dept;

    public Dept getDept() {
        return dept;
    }

    private final DeptRepository deptRepository;

    public DeptNewModel(DeptRepository deptRepository) {
        this.deptRepository = deptRepository;
    }

    public void Page_Load(Object sender, EventArgs e) {
        if (!isPostBack()) {
            // 绑定下拉树表格
            Grid1.setDataSource(deptRepository.findAllByOrderBySortIndexAsc());
            Grid1.dataBind();
        }
    }

    public void btnSaveClose_Click(Object sender, EventArgs e) {
        if (!getModelState().isValid()) {
            return;
        }
        // 设置父部门（未选择则为顶级部门）
        String parentValue = ddbParent.getValue();
        dept.setParentId(intOrNull(parentValue));
        deptRepository.save(dept);
        ActiveWindow.hidePostBack();
    }
}

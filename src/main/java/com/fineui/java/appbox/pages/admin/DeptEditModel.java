package com.fineui.java.appbox.pages.admin;

import com.fineui.java.appbox.business.AdminPageBase;
import com.fineui.java.appbox.business.CheckPower;
import com.fineui.java.appbox.model.Dept;
import com.fineui.java.appbox.repository.DeptRepository;
import com.fineui.java.binding.BindProperty;
import com.fineui.java.binding.HiddenProperty;
import com.fineui.java.core.AbortPageException;
import com.fineui.java.core.ActiveWindow;
import com.fineui.java.core.EventArgs;
import com.fineui.java.core.FineUIPage;
import com.fineui.java.core.GridRowDataBoundEventArgs;
import com.fineui.java.core.controls.DropDownBox;
import com.fineui.java.core.controls.Grid;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 编辑部门（路由 {@code admin/dept-edit}，弹窗内打开）。上级部门下拉树表格里，本部门及其全部下级不可选
 * （否则会把树挂成环）；保存走 read-first：按主键重读、只覆盖表单字段。
 */
@FineUIPage("admin/dept-edit")
@CheckPower("CoreDeptEdit")
public class DeptEditModel extends AdminPageBase {

    DropDownBox ddbParent;
    Grid Grid1;

    @HiddenProperty
    private int deptId;

    @BindProperty
    private Dept dept;

    public Dept getDept() {
        return dept;
    }

    private final DeptRepository deptRepository;

    /** 下拉树表格的数据源按主键索引，供行绑定事件判断「是否本部门或其下级」。 */
    private final Map<Integer, Dept> deptsById = new HashMap<>();

    public DeptEditModel(DeptRepository deptRepository) {
        this.deptRepository = deptRepository;
    }

    public void Page_Get(Object sender, EventArgs e) {
        deptId = getQueryInt("id", 0);
        dept = deptRepository.findById(deptId).orElse(null);
        if (dept == null) {
            throw new AbortPageException("无效参数！", ActiveWindow.hideReference());
        }
    }

    public void Page_Load(Object sender, EventArgs e) {
        if (!isPostBack()) {
            List<Dept> all = deptRepository.findAllByOrderBySortIndexAsc();
            for (Dept d : all) {
                deptsById.put(d.getId(), d);
            }
            Grid1.setDataSource(all);
            Grid1.dataBind();
            // 当前节点的父节点
            if (dept.getParent() != null) {
                ddbParent.setValue(String.valueOf(dept.getParentId()));
                ddbParent.setText(dept.getParent().getName());
            }
        }
    }

    /** 行绑定：本部门及其下级部门不可选为上级。 */
    public void Grid1_RowDataBound(Object sender, GridRowDataBoundEventArgs e) {
        Dept row = (Dept) e.getDataItem();
        e.setRowSelectable(!isOrChildOfCurrent(row.getId()));
    }

    /** 保存时重新读全表判断：候选上级是不是本部门或它的下级（回发不跑 Page_Load，索引是空的）。 */
    private boolean isSelfOrDescendant(Integer candidateId) {
        Map<Integer, Dept> all = new HashMap<>();
        for (Dept d : deptRepository.findAllByOrderBySortIndexAsc()) {
            all.put(d.getId(), d);
        }
        Integer id = candidateId;
        int guard = 0;
        while (id != null) {
            if (guard++ >= 100) {
                return true;   // 层级异常（超深或库里本来就有环）：保守拒绝，不把环坐实
            }
            if (id == deptId) {
                return true;
            }
            Dept d = all.get(id);
            id = d == null ? null : d.getParentId();
        }
        return false;
    }

    private boolean isOrChildOfCurrent(Integer id) {
        // 沿上级链向上走：碰到本部门即为其下级（或本身）
        int guard = 0;
        while (id != null && guard++ < 100) {
            if (id == deptId) {
                return true;
            }
            Dept d = deptsById.get(id);
            id = d == null ? null : d.getParentId();
        }
        return false;
    }

    public void btnSaveClose_Click(Object sender, EventArgs e) {
        if (!getModelState().isValid()) {
            return;
        }
        Dept stored = deptRepository.findById(deptId).orElse(null);
        if (stored == null) {
            showAlert("该部门不存在或已被删除！");
            return;
        }
        String parentValue = ddbParent.getValue();
        Integer parentId = intOrNull(parentValue);
        if (parentId != null && isSelfOrDescendant(parentId)) {
            showAlert("上级部门不能是本部门或其下级部门！");
            return;
        }
        stored.setName(dept.getName());
        stored.setSortIndex(dept.getSortIndex());
        stored.setRemark(dept.getRemark());
        stored.setParentId(parentId);
        deptRepository.save(stored);
        ActiveWindow.hidePostBack();
    }
}

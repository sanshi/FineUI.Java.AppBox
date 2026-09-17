package com.fineui.java.appbox.pages.admin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fineui.java.appbox.business.AppBoxAdminPageBase;
import com.fineui.java.appbox.business.CheckPower;
import com.fineui.java.appbox.model.Dept;
import com.fineui.java.appbox.model.User;
import com.fineui.java.appbox.repository.DeptRepository;
import com.fineui.java.appbox.repository.UserRepository;
import com.fineui.java.core.CustomEventArgs;
import com.fineui.java.core.EventArgs;
import com.fineui.java.core.FineUIPage;
import com.fineui.java.core.GridCommandEventArgs;
import com.fineui.java.core.controls.Button;
import com.fineui.java.core.controls.DropDownList;
import com.fineui.java.core.controls.Grid;
import com.fineui.java.core.controls.TwinTriggerBox;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * 部门用户管理（路由 {@code admin/dept-user}）：左侧部门树，右侧当前部门下的用户（搜索 + 分页排序），
 * 可从部门移除选中用户、或弹窗添加用户到当前部门。
 */
@FineUIPage("admin/dept-user")
@CheckPower("CoreDeptUserView")
public class DeptUserModel extends AppBoxAdminPageBase {

    Grid Grid1;
    Grid Grid2;
    TwinTriggerBox ttbSearchMessage;
    DropDownList ddlGridPageSize;
    Button btnNew;
    Button btnDeleteSelected;

    private final DeptRepository deptRepository;
    private final UserRepository userRepository;

    private List<Dept> depts;
    private List<User> users;

    public List<Dept> getDepts() {
        return depts;
    }

    public List<User> getUsers() {
        return users;
    }

    public DeptUserModel(DeptRepository deptRepository, UserRepository userRepository) {
        this.deptRepository = deptRepository;
        this.userRepository = userRepository;
    }

    public void Page_Load(Object sender, EventArgs e) {
        if (!isPostBack()) {
            btnNew.setEnabled(checkPower("CoreDeptUserNew"));
            boolean powerDelete = checkPower("CoreDeptUserDelete");
            btnDeleteSelected.setEnabled(powerDelete);
            Grid2.findCommand("Delete").setEnabled(powerDelete);

            List<Dept> list = loadGrid1Data();
            if (list.isEmpty()) {
                showAlert("请先添加部门！");
                return;
            }
            Grid1.setSelectedRowIdArray(new String[] { String.valueOf(list.get(0).getId()) });

            ddlGridPageSize.setSelectedValue(String.valueOf(configService.getPageSize()));
            Grid2.setPageSize(configService.getPageSize());
            loadGrid2Data();
        }
    }

    /** 左侧：全部部门（按表格排序）。 */
    private List<Dept> loadGrid1Data() {
        depts = deptRepository.findAllByOrderBySortIndexAsc();
        Grid1.setDataSource(depts);
        Grid1.dataBind();
        return depts;
    }

    /** 右侧：当前选中部门下的用户（非 admin），可按名称搜索。 */
    private void loadGrid2Data() {
        Integer deptId = selectedDeptId();
        if (deptId == null) {
            Grid2.setDataSource(null);
            Grid2.dataBind();
            return;
        }
        String searchText = ttbSearchMessage.getValue().trim();
        Specification<User> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (!searchText.isEmpty()) {
                String pattern = "%" + searchText + "%";
                predicates.add(cb.or(cb.like(root.get("name"), pattern), cb.like(root.get("chineseName"), pattern),
                        cb.like(root.get("englishName"), pattern)));
            }
            predicates.add(cb.notEqual(root.get("name"), "admin"));
            predicates.add(cb.equal(root.get("deptId"), deptId));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Page<User> page = loadPage(Grid2, pageable -> userRepository.findAll(spec, pageable));
        users = page.getContent();
        Grid2.setDataSource(users);
        Grid2.dataBind();
    }

    private Integer selectedDeptId() {
        String selected = Grid1.getSelectedRowId();
        return intOrNull(selected);
    }


    public void Grid1_RowClick(Object sender, EventArgs e) {
        loadGrid2Data();
    }

    public void Window1_Close(Object sender, EventArgs e) {
        loadGrid2Data();
    }

    public void Grid2_Sort(Object sender, EventArgs e) {
        loadGrid2Data();
    }

    public void Grid2_PageIndexChanged(Object sender, EventArgs e) {
        loadGrid2Data();
    }

    public void ddlGridPageSize_SelectedIndexChanged(Object sender, EventArgs e) {
        Grid2.setPageSize(intOrDefault(ddlGridPageSize.getSelectedValue(), configService.getPageSize()));
        loadGrid2Data();
    }

    public void ttbSearchMessage_Trigger1Click(Object sender, EventArgs e) {
        ttbSearchMessage.setValue("");
        ttbSearchMessage.setShowTrigger1(false);
        loadGrid2Data();
    }

    public void ttbSearchMessage_Trigger2Click(Object sender, EventArgs e) {
        ttbSearchMessage.setShowTrigger1(true);
        loadGrid2Data();
    }

    public void Page_CustomEvent(Object sender, CustomEventArgs e) {
        if ("Grid2_DeleteRows".equals(e.getEventName())) {
            JsonNode args = parseJson(e.getArgument());
            deleteRows(toIntList(args.get("rowIDs")));
        }
    }

    /** 把这些用户从当前部门移除（清空用户的所属部门）。 */
    private void deleteRows(List<Integer> userIds) {
        Integer deptId = selectedDeptId();
        if (deptId == null) {
            return;
        }
        if (!checkPower("CoreDeptUserDelete")) {
            checkPowerFailWithAlert();
            return;
        }
        List<User> selected = userRepository.findAllById(userIds);
        for (User user : selected) {
            if (deptId.equals(user.getDeptId())) {
                user.setDeptId(null);
            }
        }
        userRepository.saveAll(selected);
        loadGrid2Data();
    }

    public void Grid2_RowCommand(Object sender, GridCommandEventArgs e) {
        if ("Delete".equals(e.getCommandName())) {
            Integer rowId = getRowId(e);
            if (rowId != null) {
                deleteRows(List.of(rowId));
            }
        }
    }
}

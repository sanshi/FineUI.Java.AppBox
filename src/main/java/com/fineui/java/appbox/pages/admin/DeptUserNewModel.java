package com.fineui.java.appbox.pages.admin;

import com.fineui.java.appbox.business.AppBoxAdminPageBase;
import com.fineui.java.appbox.business.CheckPower;
import com.fineui.java.appbox.model.User;
import com.fineui.java.appbox.repository.DeptRepository;
import com.fineui.java.appbox.repository.UserRepository;
import com.fineui.java.binding.HiddenProperty;
import com.fineui.java.core.AbortPageException;
import com.fineui.java.core.ActiveWindow;
import com.fineui.java.core.EventArgs;
import com.fineui.java.core.FineUIPage;
import com.fineui.java.core.controls.DropDownList;
import com.fineui.java.core.controls.Grid;
import com.fineui.java.core.controls.TwinTriggerBox;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * 添加用户到当前部门（路由 {@code admin/dept-user-new}，弹窗内打开）：列出尚未分配部门的用户（可搜索、跨页保持勾选），
 * 勾选后一次归入。部门主键首屏从 {@code ?deptID} 取、之后随状态往返。
 */
@FineUIPage("admin/dept-user-new")
@CheckPower("CoreDeptUserNew")
public class DeptUserNewModel extends AppBoxAdminPageBase {

    Grid Grid1;
    TwinTriggerBox ttbSearchMessage;
    DropDownList ddlGridPageSize;

    @HiddenProperty
    private int deptId;

    private final DeptRepository deptRepository;
    private final UserRepository userRepository;

    private List<User> users;

    public List<User> getUsers() {
        return users;
    }

    public DeptUserNewModel(DeptRepository deptRepository, UserRepository userRepository) {
        this.deptRepository = deptRepository;
        this.userRepository = userRepository;
    }

    public void Page_Get(Object sender, EventArgs e) {
        deptId = getQueryInt("deptID", 0);
        if (!deptRepository.existsById(deptId)) {
            throw new AbortPageException("参数错误！", ActiveWindow.hideReference());
        }
    }

    public void Page_Load(Object sender, EventArgs e) {
        if (!isPostBack()) {
            ddlGridPageSize.setSelectedValue(String.valueOf(configService.getPageSize()));
            Grid1.setPageSize(configService.getPageSize());
            loadData();
        }
    }

    /** 尚未分配部门的用户（非 admin），可按名称搜索。 */
    private void loadData() {
        String searchText = ttbSearchMessage.getValue().trim();
        Specification<User> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (!searchText.isEmpty()) {
                String pattern = "%" + searchText + "%";
                predicates.add(cb.or(cb.like(root.get("name"), pattern), cb.like(root.get("chineseName"), pattern),
                        cb.like(root.get("englishName"), pattern)));
            }
            predicates.add(cb.notEqual(root.get("name"), "admin"));
            predicates.add(cb.isNull(root.get("deptId")));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Page<User> page = loadPage(Grid1, pageable -> userRepository.findAll(spec, pageable));
        users = page.getContent();
        Grid1.setDataSource(users);
        Grid1.dataBind();
    }

    public void btnSaveClose_Click(Object sender, EventArgs e) {
        String[] selectedRowIds = Grid1.getSelectedRowIdArray();
        if (selectedRowIds == null || selectedRowIds.length == 0) {
            showAlert("请至少选择一项！");
            return;
        }
        if (!deptRepository.existsById(deptId)) {
            showAlert("该部门不存在或已被删除！");
            return;
        }
        List<Integer> userIds = new ArrayList<>();
        for (String id : selectedRowIds) {
            Integer userId = intOrNull(id);
            if (userId != null) {
                userIds.add(userId);
            }
        }
        // 把选中用户的所属部门设为当前部门
        // 列表里排除了 admin，但回传的主键不可信：保存时再挡一次
        List<User> selected = new ArrayList<>(userRepository.findAllById(userIds));
        selected.removeIf(user -> "admin".equals(user.getName()));
        for (User user : selected) {
            user.setDeptId(deptId);
        }
        userRepository.saveAll(selected);
        ActiveWindow.hidePostBack();
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
}

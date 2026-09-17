package com.fineui.java.appbox.pages.admin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fineui.java.appbox.business.AppBoxAdminPageBase;
import com.fineui.java.appbox.business.CheckPower;
import com.fineui.java.appbox.model.User;
import com.fineui.java.appbox.repository.UserRepository;
import com.fineui.java.core.CustomEventArgs;
import com.fineui.java.core.EventArgs;
import com.fineui.java.core.FineUIPage;
import com.fineui.java.core.GridCommandEventArgs;
import com.fineui.java.core.MessageBoxIcon;
import com.fineui.java.core.controls.Button;
import com.fineui.java.core.controls.DropDownList;
import com.fineui.java.core.controls.Grid;
import com.fineui.java.core.controls.RadioButtonList;
import com.fineui.java.core.controls.TwinTriggerBox;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * 用户列表（路由 {@code admin/user-list}）：搜索 + 启用状态筛选 + 数据库分页排序 + 每页条数切换 +
 * 批量删除/启停（勾选行经自定义事件回传）+ 行内查看/改密/编辑/删除（弹窗）。
 * 按钮与行命令的可用状态按当前用户权限设置；删除与启停在事件里再校验一次权限。
 */
@FineUIPage("admin/user-list")
@CheckPower("CoreUserView")
public class UserListModel extends AppBoxAdminPageBase {

    Grid Grid1;
    TwinTriggerBox ttbSearchMessage;
    RadioButtonList rblEnableStatus;
    DropDownList ddlGridPageSize;
    Button btnNew;
    Button btnChangeEnableUsers;
    Button btnDeletedSelected;

    private final UserRepository userRepository;

    // 表格行模型来源：row-type-from="users" 从此 getter 的泛型推导列头与类型
    private List<User> users;

    public List<User> getUsers() {
        return users;
    }

    public UserListModel(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void Page_Load(Object sender, EventArgs e) {
        if (!isPostBack()) {
            boolean powerCoreUserNew = checkPower("CoreUserNew");
            boolean powerCoreUserEdit = checkPower("CoreUserEdit");
            boolean powerCoreUserDelete = checkPower("CoreUserDelete");
            boolean powerCoreUserChangePassword = checkPower("CoreUserChangePassword");

            // 根据用户权限控制页面控件的可用状态
            btnNew.setEnabled(powerCoreUserNew);
            btnChangeEnableUsers.setEnabled(powerCoreUserEdit);
            btnDeletedSelected.setEnabled(powerCoreUserDelete);
            // 行内按钮的权限
            Grid1.findCommand("Edit").setEnabled(powerCoreUserEdit);
            Grid1.findCommand("Delete").setEnabled(powerCoreUserDelete);
            Grid1.findCommand("ChangePassword").setEnabled(powerCoreUserChangePassword);

            // 初始化页面控件属性
            ddlGridPageSize.setSelectedValue(String.valueOf(configService.getPageSize()));
            Grid1.setPageSize(configService.getPageSize());

            loadData();
        }
    }

    private void loadData() {
        String searchText = ttbSearchMessage.getValue().trim();
        String enableStatus = rblEnableStatus.getSelectedValue();
        boolean isAdmin = "admin".equals(getIdentityName());

        Specification<User> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (!searchText.isEmpty()) {
                String pattern = "%" + searchText + "%";
                predicates.add(cb.or(
                        cb.like(root.get("name"), pattern),
                        cb.like(root.get("chineseName"), pattern),
                        cb.like(root.get("englishName"), pattern)));
            }
            // 非超级管理员看不到 admin 这一行
            if (!isAdmin) {
                predicates.add(cb.notEqual(root.get("name"), "admin"));
            }
            // 过滤启用状态
            if (!"all".equals(enableStatus)) {
                predicates.add(cb.equal(root.get("enabled"), "enabled".equals(enableStatus)));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        // 分页 + 排序 + 总记录数（页码越界自动回退）
        Page<User> page = loadPage(Grid1, pageable -> userRepository.findAll(spec, pageable));
        users = page.getContent();
        Grid1.setDataSource(users);
        Grid1.dataBind();
    }

    // —— 事件 ——

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
        // 设置每页显示的项数
        Grid1.setPageSize(intOrDefault(ddlGridPageSize.getSelectedValue(), configService.getPageSize()));
        loadData();
    }

    public void ttbSearchMessage_Trigger1Click(Object sender, EventArgs e) {
        // 清空输入框的内容，并隐藏清空图标
        ttbSearchMessage.setValue("");
        ttbSearchMessage.setShowTrigger1(false);
        loadData();
    }

    public void ttbSearchMessage_Trigger2Click(Object sender, EventArgs e) {
        // 显示清空图标
        ttbSearchMessage.setShowTrigger1(true);
        loadData();
    }

    public void rblEnableStatus_SelectedIndexChanged(Object sender, EventArgs e) {
        loadData();
    }

    /** 批量操作：客户端把勾选行的主键经 F.customEvent 回传。 */
    public void Page_CustomEvent(Object sender, CustomEventArgs e) {
        JsonNode args = parseJson(e.getArgument());
        List<Integer> rowIds = toIntList(args.get("rowIDs"));
        if ("Grid1_DeleteRows".equals(e.getEventName())) {
            deleteRows(rowIds);
        } else if ("Grid1_EnableRows".equals(e.getEventName())) {
            if (!checkPower("CoreUserEdit")) {
                checkPowerFailWithAlert();
                return;
            }
            boolean enabled = "enable".equals(args.path("action").asText());
            List<User> toUpdate = userRepository.findAllById(rowIds);
            // 列表里看不到 admin，但回传的主键不可信：先整体校验，确认没有 admin 再改值
            for (User user : toUpdate) {
                if ("admin".equals(user.getName())) {
                    showAlertInTop("不能修改超级管理员（admin）的启用状态！", "", MessageBoxIcon.Warning);
                    return;
                }
            }
            for (User user : toUpdate) {
                user.setEnabled(enabled);
            }
            userRepository.saveAll(toUpdate);
            loadData();
        }
    }

    private void deleteRows(List<Integer> rowIds) {
        if (!checkPower("CoreUserDelete")) {
            checkPowerFailWithAlert();
            return;
        }
        List<User> toDelete = userRepository.findAllById(rowIds);
        for (User user : toDelete) {
            if ("admin".equals(user.getName())) {
                showAlertInTop("不能删除超级管理员（admin）！", "", MessageBoxIcon.Warning);
                return;
            }
        }
        userRepository.deleteAll(toDelete);
        loadData();
    }

    /** 行命令：删除（行主键从数据键取）。 */
    public void Grid1_RowCommand(Object sender, GridCommandEventArgs e) {
        if ("Delete".equals(e.getCommandName())) {
            Integer rowId = getRowId(e);
            if (rowId != null) {
                deleteRows(List.of(rowId));
            }
        }
    }
}

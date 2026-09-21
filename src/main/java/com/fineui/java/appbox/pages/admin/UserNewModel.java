package com.fineui.java.appbox.pages.admin;

import com.fineui.java.appbox.business.AdminPageBase;
import com.fineui.java.appbox.business.AuthService;
import com.fineui.java.appbox.business.CheckPower;
import com.fineui.java.appbox.business.PasswordUtil;
import com.fineui.java.appbox.model.User;
import com.fineui.java.appbox.repository.DeptRepository;
import com.fineui.java.appbox.repository.RoleRepository;
import com.fineui.java.appbox.repository.TitleRepository;
import com.fineui.java.appbox.repository.UserRepository;
import com.fineui.java.binding.BindProperty;
import com.fineui.java.core.ActiveWindow;
import com.fineui.java.core.EventArgs;
import com.fineui.java.core.FineUIPage;
import com.fineui.java.core.controls.CheckBoxList;
import com.fineui.java.core.controls.DropDownBox;
import com.fineui.java.core.controls.Grid;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 新增用户（路由 {@code admin/user-new}，弹窗内打开）：表单经 {@code @BindProperty} 绑定到用户实体，
 * 角色/职称多选下拉、部门下拉树在首屏填充候选项；保存时哈希密码、写入关联，然后关窗并通知父页刷新。
 */
@FineUIPage("admin/user-new")
@CheckPower("CoreUserNew")
public class UserNewModel extends AdminPageBase {

    DropDownBox ddbRoles;
    DropDownBox ddbTitles;
    DropDownBox ddbDept;
    CheckBoxList cblRoles;
    CheckBoxList cblTitles;
    Grid gridDept;

    /** 表单绑定根：首屏为空壳供输入；回发时框架新建空壳承载表单值。 */
    @BindProperty
    private User currentUser;

    public User getCurrentUser() {
        return currentUser;
    }

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final TitleRepository titleRepository;
    private final DeptRepository deptRepository;

    public UserNewModel(UserRepository userRepository, RoleRepository roleRepository,
                        TitleRepository titleRepository, DeptRepository deptRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.titleRepository = titleRepository;
        this.deptRepository = deptRepository;
    }

    public void Page_Load(Object sender, EventArgs e) {
        if (!isPostBack()) {
            // 候选角色 / 职称 / 部门树
            cblRoles.setDataSource(roleRepository.findAll());
            cblRoles.dataBind();
            cblTitles.setDataSource(titleRepository.findAll());
            cblTitles.dataBind();
            gridDept.setDataSource(deptRepository.findAllByOrderBySortIndexAsc());
            gridDept.dataBind();
        }
    }

    public void btnSaveClose_Click(Object sender, EventArgs e) {
        if (!getModelState().isValid()) {
            return;
        }
        String password = currentUser.getPassword() == null ? "" : currentUser.getPassword().trim();
        if (password.isEmpty()) {
            showAlert("密码不能为空！");
            return;
        }
        if (userRepository.existsByName(currentUser.getName())) {
            showAlert("用户 " + currentUser.getName() + " 已经存在！");
            return;
        }

        // 创建保存到数据库的密码哈希
        currentUser.setPassword(PasswordUtil.createDbPassword(password));
        currentUser.setCreateTime(LocalDateTime.now());
        // 所属部门
        String deptValue = ddbDept.getValue();
        if (deptValue != null && !deptValue.isEmpty()) {
            currentUser.setDeptId(intOrNull(deptValue));
        }
        // 所属角色 / 拥有职称
        currentUser.setRoles(new ArrayList<>(roleRepository.findAllById(toIds(ddbRoles.getValues()))));
        currentUser.setTitles(new ArrayList<>(titleRepository.findAllById(toIds(ddbTitles.getValues()))));

        userRepository.save(currentUser);
        AuthService.invalidatePermissionCaches();   // 新用户带了角色，权限视图变了

        // 关闭本窗体（触发父页窗体的关闭事件以刷新列表）
        ActiveWindow.hidePostBack();
    }

    static List<Integer> toIds(List<String> values) {
        List<Integer> ids = new ArrayList<>();
        if (values != null) {
            for (String v : values) {
                if (v != null && !v.isEmpty()) {
                    Integer parsed = intOrNull(v);
                    if (parsed != null) {
                        ids.add(parsed);
                    }
                }
            }
        }
        return ids;
    }
}

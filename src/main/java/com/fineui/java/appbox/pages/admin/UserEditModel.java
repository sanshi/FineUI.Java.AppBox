package com.fineui.java.appbox.pages.admin;

import com.fineui.java.appbox.business.AdminPageBase;
import com.fineui.java.appbox.business.AuthService;
import com.fineui.java.appbox.business.CheckPower;
import com.fineui.java.appbox.model.Role;
import com.fineui.java.appbox.model.Title;
import com.fineui.java.appbox.model.User;
import com.fineui.java.appbox.repository.DeptRepository;
import com.fineui.java.appbox.repository.RoleRepository;
import com.fineui.java.appbox.repository.TitleRepository;
import com.fineui.java.appbox.repository.UserRepository;
import com.fineui.java.binding.BindProperty;
import com.fineui.java.core.PageContext;
import com.fineui.java.core.ActiveWindow;
import com.fineui.java.core.EventArgs;
import com.fineui.java.core.FineUIPage;
import com.fineui.java.core.controls.CheckBoxList;
import com.fineui.java.core.controls.DropDownBox;
import com.fineui.java.core.controls.Grid;

import java.util.ArrayList;
import java.util.List;

/**
 * 编辑用户（路由 {@code admin/user-edit}，弹窗内打开）。每次按 {@code ?id} 查询并校验实体，首屏填入表单；保存时只覆盖表单里出现过的字段（用户名、密码、创建时间等不在表单里的字段保持原值），再更新角色/职称/部门。
 */
@FineUIPage("admin/user-edit")
@CheckPower("CoreUserEdit")
public class UserEditModel extends AdminPageBase {

    DropDownBox ddbRoles;
    DropDownBox ddbTitles;
    DropDownBox ddbDept;
    CheckBoxList cblRoles;
    CheckBoxList cblTitles;
    Grid gridDept;

    /** 编辑目标主键：每次从当前 URL 的 ?id 取。 */
    private int userId;

    /** 表单绑定根：首屏由 Page_Init 加载供回显；回发时是只承载表单值的空壳。 */
    @BindProperty
    private User currentUser;

    /** 本次按 URL 查询并校验的保存目标；不接受表单或隐藏字段决定主键。 */
    private User storedUser;

    public User getCurrentUser() {
        return currentUser;
    }

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final TitleRepository titleRepository;
    private final DeptRepository deptRepository;

    public UserEditModel(UserRepository userRepository, RoleRepository roleRepository,
                         TitleRepository titleRepository, DeptRepository deptRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.titleRepository = titleRepository;
        this.deptRepository = deptRepository;
    }

    public void Page_Init(Object sender, EventArgs e) {
        userId = getQueryIntValue("id");
        storedUser = userRepository.findById(userId).orElse(null);
        if (storedUser == null) {
            PageContext.abortPage("无效参数！", ActiveWindow.hideCommand());
            return;
        }
        if ("admin".equals(storedUser.getName()) && !"admin".equals(getIdentityName())) {
            PageContext.abortPage("你无权编辑超级管理员！", ActiveWindow.hideCommand());
            return;
        }
        if (!isPostBack()) {
            currentUser = storedUser;
        }
    }

    public void Page_Load(Object sender, EventArgs e) {
        if (!isPostBack()) {
            // 用户所属角色：已选值 + 候选项
            List<String> roleIds = new ArrayList<>();
            List<String> roleNames = new ArrayList<>();
            for (Role role : currentUser.getRoles()) {
                roleIds.add(String.valueOf(role.getId()));
                roleNames.add(role.getName());
            }
            if (!roleIds.isEmpty()) {
                ddbRoles.setValues(roleIds);
                ddbRoles.setTexts(roleNames);
            }
            cblRoles.setDataSource(roleRepository.findAll());
            cblRoles.dataBind();

            // 用户拥有职称
            List<String> titleIds = new ArrayList<>();
            List<String> titleNames = new ArrayList<>();
            for (Title title : currentUser.getTitles()) {
                titleIds.add(String.valueOf(title.getId()));
                titleNames.add(title.getName());
            }
            if (!titleIds.isEmpty()) {
                ddbTitles.setValues(titleIds);
                ddbTitles.setTexts(titleNames);
            }
            cblTitles.setDataSource(titleRepository.findAll());
            cblTitles.dataBind();

            // 用户所属部门
            if (currentUser.getDeptId() != null) {
                ddbDept.setValue(String.valueOf(currentUser.getDeptId()));
                ddbDept.setText(currentUser.getDeptName());
            }
            gridDept.setDataSource(deptRepository.findAllByOrderBySortIndexAsc());
            gridDept.dataBind();
        }
    }

    public void btnSaveClose_Click(Object sender, EventArgs e) {
        // 用户名不在表单里（只读标签），空壳上必然报「不能为空」——显式移除
        getModelState().remove("currentUser.name");
        if (!getModelState().isValid()) {
            return;
        }

        // 保存目标来自本次 Page_Init 的查询和权限校验，不信任表单中的主键。
        User user = storedUser;

        // 只覆盖表单里出现过的字段（这份清单与模板里的 for 保持一致）
        user.setChineseName(currentUser.getChineseName());
        user.setGender(currentUser.getGender());
        user.setEnabled(currentUser.isEnabled());
        user.setEmail(currentUser.getEmail());
        user.setCompanyEmail(currentUser.getCompanyEmail());
        user.setOfficePhone(currentUser.getOfficePhone());
        user.setOfficePhoneExt(currentUser.getOfficePhoneExt());
        user.setHomePhone(currentUser.getHomePhone());
        user.setCellPhone(currentUser.getCellPhone());
        user.setRemark(currentUser.getRemark());

        // 更新用户所属的角色 / 拥有的职称（整体替换）
        user.getRoles().clear();
        user.getRoles().addAll(roleRepository.findAllById(UserNewModel.toIds(ddbRoles.getValues())));
        user.getTitles().clear();
        user.getTitles().addAll(titleRepository.findAllById(UserNewModel.toIds(ddbTitles.getValues())));
        AuthService.invalidatePermissionCaches();   // 用户的角色集合变了，其在线会话的权限缓存作废

        // 选择了部门则更新，否则清空
        String deptValue = ddbDept.getValue();
        user.setDeptId(intOrNull(deptValue));

        userRepository.save(user);

        // 关闭本窗体（触发父页窗体的关闭事件以刷新列表）
        ActiveWindow.hidePostBack();
    }
}

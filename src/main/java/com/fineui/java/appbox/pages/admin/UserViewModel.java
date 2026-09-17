package com.fineui.java.appbox.pages.admin;

import com.fineui.java.appbox.business.AppBoxAdminPageBase;
import com.fineui.java.appbox.business.CheckPower;
import com.fineui.java.appbox.model.Role;
import com.fineui.java.appbox.model.Title;
import com.fineui.java.appbox.model.User;
import com.fineui.java.appbox.repository.UserRepository;
import com.fineui.java.binding.BindProperty;
import com.fineui.java.core.AbortPageException;
import com.fineui.java.core.ActiveWindow;
import com.fineui.java.core.EventArgs;
import com.fineui.java.core.FineUIPage;
import com.fineui.java.core.controls.Label;

import java.util.ArrayList;
import java.util.List;

/** 查看用户信息（路由 {@code admin/user-view}，弹窗内打开）：只读表单，首屏按 {@code ?id} 加载。 */
@FineUIPage("admin/user-view")
@CheckPower("CoreUserView")
public class UserViewModel extends AppBoxAdminPageBase {

    Label labEnabled;
    Label labRoles;
    Label labTitles;
    Label labDept;

    /** 表单回显根：首屏由 Page_Get 加载。 */
    @BindProperty
    private User currentUser;

    public User getCurrentUser() {
        return currentUser;
    }

    private final UserRepository userRepository;

    public UserViewModel(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void Page_Get(Object sender, EventArgs e) {
        currentUser = userRepository.findById(getQueryInt("id", 0)).orElse(null);
        if (currentUser == null) {
            throw new AbortPageException("无效参数！", ActiveWindow.hideReference());
        }
        if ("admin".equals(currentUser.getName()) && !"admin".equals(getIdentityName())) {
            throw new AbortPageException("你无权编辑超级管理员！", ActiveWindow.hideReference());
        }
    }

    public void Page_Load(Object sender, EventArgs e) {
        if (!isPostBack()) {
            labRoles.setText(String.join(",", names(currentUser.getRoles())));
            labTitles.setText(String.join(",", titleNames(currentUser.getTitles())));
            if (currentUser.getDeptId() != null) {
                labDept.setText(currentUser.getDeptName());
            }
            labEnabled.setText(currentUser.isEnabled() ? "启用" : "禁用");
        }
    }

    private static List<String> names(List<Role> roles) {
        List<String> list = new ArrayList<>();
        for (Role role : roles) {
            list.add(role.getName());
        }
        return list;
    }

    private static List<String> titleNames(List<Title> titles) {
        List<String> list = new ArrayList<>();
        for (Title title : titles) {
            list.add(title.getName());
        }
        return list;
    }
}

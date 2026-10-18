package com.fineui.java.appbox.pages.admin;

import com.fineui.java.core.Alert;
import com.fineui.java.appbox.business.AdminPageBase;
import com.fineui.java.appbox.business.CheckPower;
import com.fineui.java.appbox.business.PasswordUtil;
import com.fineui.java.appbox.model.User;
import com.fineui.java.appbox.repository.UserRepository;
import com.fineui.java.binding.BindProperty;
import com.fineui.java.core.PageContext;
import com.fineui.java.core.ActiveWindow;
import com.fineui.java.core.EventArgs;
import com.fineui.java.core.FineUIPage;
import com.fineui.java.core.controls.TextBox;

/**
 * 修改指定用户的登录密码（路由 {@code admin/user-change-password}，弹窗内打开）。
 * 每次按当前 URL 的 {@code ?id} 查询并校验目标用户；保存时修改本次查询的实体。
 */
@FineUIPage("admin/user-change-password")
@CheckPower("CoreUserChangePassword")
public class UserChangePasswordModel extends AdminPageBase {

    TextBox tbxPassword;

    private int userId;

    /** 表单回显根（只读标签），首屏由 Page_Init 加载。 */
    @BindProperty
    private User currentUser;

    /** 本次按 URL 查询并校验的保存目标；不接受表单或隐藏字段决定主键。 */
    private User storedUser;

    public User getCurrentUser() {
        return currentUser;
    }

    private final UserRepository userRepository;

    public UserChangePasswordModel(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void Page_Init(Object sender, EventArgs e) {
        userId = getQueryInt("id", 0);
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

    public void btnSaveClose_Click(Object sender, EventArgs e) {
        // 保存目标来自本次 Page_Init 的查询和权限校验，不信任表单中的主键。
        User user = storedUser;
        if (user == null) {
            Alert.show("该用户不存在或已被删除！");
            return;
        }
        if ("admin".equals(user.getName()) && !"admin".equals(getIdentityName())) {
            Alert.show("你无权编辑超级管理员！");
            return;
        }
        String newPassword = tbxPassword.getValue().trim();
        if (newPassword.isEmpty()) {
            tbxPassword.markInvalid("新密码不能为空！");
            return;
        }
        user.setPassword(PasswordUtil.createDbPassword(newPassword));
        userRepository.save(user);

        // 关闭本窗体（触发父页窗体的关闭事件以刷新列表）
        ActiveWindow.hidePostBack();
    }
}

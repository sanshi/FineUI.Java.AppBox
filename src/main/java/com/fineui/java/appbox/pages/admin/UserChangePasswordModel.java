package com.fineui.java.appbox.pages.admin;

import com.fineui.java.appbox.business.AdminPageBase;
import com.fineui.java.appbox.business.CheckPower;
import com.fineui.java.appbox.business.PasswordUtil;
import com.fineui.java.appbox.model.User;
import com.fineui.java.appbox.repository.UserRepository;
import com.fineui.java.binding.BindProperty;
import com.fineui.java.binding.HiddenProperty;
import com.fineui.java.core.AbortPageException;
import com.fineui.java.core.ActiveWindow;
import com.fineui.java.core.EventArgs;
import com.fineui.java.core.FineUIPage;
import com.fineui.java.core.controls.TextBox;

/**
 * 修改指定用户的登录密码（路由 {@code admin/user-change-password}，弹窗内打开）。
 * 目标用户主键首屏从 {@code ?id} 取、之后经 {@code @HiddenProperty} 随状态往返；保存时按主键重读实体再改密码。
 */
@FineUIPage("admin/user-change-password")
@CheckPower("CoreUserChangePassword")
public class UserChangePasswordModel extends AdminPageBase {

    TextBox tbxPassword;

    @HiddenProperty
    private int userId;

    /** 表单回显根（只读标签），首屏由 Page_Get 加载。 */
    @BindProperty
    private User currentUser;

    public User getCurrentUser() {
        return currentUser;
    }

    private final UserRepository userRepository;

    public UserChangePasswordModel(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void Page_Get(Object sender, EventArgs e) {
        userId = getQueryInt("id", 0);
        currentUser = userRepository.findById(userId).orElse(null);
        if (currentUser == null) {
            throw new AbortPageException("无效参数！", ActiveWindow.hideReference());
        }
        if ("admin".equals(currentUser.getName()) && !"admin".equals(getIdentityName())) {
            throw new AbortPageException("你无权编辑超级管理员！", ActiveWindow.hideReference());
        }
    }

    public void btnSaveClose_Click(Object sender, EventArgs e) {
        // 主键随状态往返、客户端可篡改：回发路径没有 Page_Get，这里再判一次
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            showAlert("该用户不存在或已被删除！");
            return;
        }
        if ("admin".equals(user.getName()) && !"admin".equals(getIdentityName())) {
            showAlert("你无权编辑超级管理员！");
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

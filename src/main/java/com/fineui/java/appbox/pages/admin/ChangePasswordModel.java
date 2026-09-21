package com.fineui.java.appbox.pages.admin;

import com.fineui.java.appbox.business.AdminPageBase;
import com.fineui.java.appbox.business.PasswordUtil;
import com.fineui.java.appbox.model.User;
import com.fineui.java.appbox.repository.UserRepository;
import com.fineui.java.core.EventArgs;
import com.fineui.java.core.FineUIPage;
import com.fineui.java.core.MessageBoxIcon;
import com.fineui.java.core.controls.TextBox;

/** 修改当前登录用户自己的密码（路由 {@code admin/change-password}）：校验当前密码、两次新密码一致后更新。 */
@FineUIPage("admin/change-password")
public class ChangePasswordModel extends AdminPageBase {

    TextBox tbxOldPassword;
    TextBox tbxNewPassword;
    TextBox tbxConfirmNewPassword;

    private final UserRepository userRepository;

    public ChangePasswordModel(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void btnSave_Click(Object sender, EventArgs e) {
        String oldPass = tbxOldPassword.getValue().trim();
        String newPass = tbxNewPassword.getValue().trim();
        String confirmNewPass = tbxConfirmNewPassword.getValue().trim();
        if (newPass.isEmpty()) {
            tbxNewPassword.markInvalid("新密码不能为空！");
            return;
        }
        if (!newPass.equals(confirmNewPass)) {
            tbxConfirmNewPassword.markInvalid("确认密码和新密码不一致！");
            return;
        }
        Integer id = getIdentityId();
        User user = id == null ? null : userRepository.findById(id).orElse(null);
        if (user == null) {
            return;
        }
        if (!PasswordUtil.comparePasswords(user.getPassword(), oldPass)) {
            tbxOldPassword.markInvalid("当前密码不正确！");
            return;
        }
        user.setPassword(PasswordUtil.createDbPassword(newPass));
        userRepository.save(user);
        showAlertInTop("修改密码成功！", "", MessageBoxIcon.Information);
    }
}

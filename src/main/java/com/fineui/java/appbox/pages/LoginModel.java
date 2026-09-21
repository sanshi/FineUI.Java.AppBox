package com.fineui.java.appbox.pages;

import com.fineui.java.appbox.business.PageBase;
import com.fineui.java.appbox.business.PasswordUtil;
import com.fineui.java.appbox.model.User;
import com.fineui.java.appbox.repository.UserRepository;
import com.fineui.java.core.EventArgs;
import com.fineui.java.core.FineUIPage;
import com.fineui.java.core.controls.TextBox;
import com.fineui.java.core.controls.Window;

import java.util.Optional;

/**
 * 登录页（路由 {@code login}）：校验用户名密码与启用状态，成功后建立会话身份并跳转首页。
 * 已登录用户再访问本页时由拦截器直接跳首页。
 */
@FineUIPage("login")
public class LoginModel extends PageBase {

    Window Window1;
    TextBox tbxUserName;
    TextBox tbxPassword;

    private final UserRepository userRepository;

    public LoginModel(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void Page_Load(Object sender, EventArgs e) {
        if (!isPostBack()) {
            Window1.setTitle("FineUI.Java.AppBox v" + getProductVersion());
        }
    }

    public void btnSubmit_Click(Object sender, EventArgs e) {
        String userName = tbxUserName.getValue().trim();
        String password = tbxPassword.getValue().trim();

        Optional<User> found = userRepository.findByName(userName);
        if (found.isEmpty() || !PasswordUtil.comparePasswords(found.get().getPassword(), password)) {
            showAlert("用户名或密码错误！");
            return;
        }
        User user = found.get();
        if (!user.isEnabled()) {
            showAlert("用户未启用，请联系管理员！");
            return;
        }

        // 登录成功：建立会话身份 + 登记在线记录，然后跳转到登录后首页
        authService.login(user);
        redirect("/");
    }
}

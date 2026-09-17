package com.fineui.java.appbox.config;

import com.fineui.java.appbox.business.AppBoxAdminPageBase;
import com.fineui.java.appbox.business.AppBoxUser;
import com.fineui.java.appbox.business.AuthService;
import com.fineui.java.appbox.business.RouteUtil;
import com.fineui.java.core.PageManager;
import com.fineui.java.core.PageRegistry;
import com.fineui.java.core.enums.Theme;
import com.fineui.java.web.FineUIPageManagerInitializer;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

/**
 * 全局页面级配置初始化，每次首屏渲染前执行（早于 head 输出，改动才来得及生效、无闪烁）：
 * <ul>
 *   <li>读 {@code Theme} cookie：是内置主题名则切内置主题；否则若本应用 {@code static/res/themes/{名}/theme.css} 存在，
 *       按自定义主题输出该样式表；cookie 为空或都不是则用 {@code application.properties} 的全局默认；</li>
 *   <li>后台功能页（继承 {@link AppBoxAdminPageBase}）为已登录用户加页面水印「角色名（用户名）」。</li>
 * </ul>
 */
@Component
public class AppPageManagerInitializer implements FineUIPageManagerInitializer {

    private final PageRegistry registry;
    private final AuthService authService;
    private final String homeRoute;

    public AppPageManagerInitializer(PageRegistry registry, AuthService authService,
                                     @Value("${fineui.home:index}") String homeRoute) {
        this.registry = registry;
        this.authService = authService;
        this.homeRoute = homeRoute;
    }

    @Override
    public void init(PageManager pm, HttpServletRequest request) {
        applyTheme(pm, cookie(request, "Theme"));

        AppBoxUser user = AuthService.currentUser();
        if (user != null && isAdminPage(request)) {
            String watermarkText = user.getName();
            List<String> roleNames = authService.getIdentityRoleNames();
            if (!roleNames.isEmpty()) {
                watermarkText = roleNames.get(0) + "（" + watermarkText + "）";
            }
            pm.set("watermark", true).set("watermarkText", watermarkText).set("watermarkFontSize", 16);
        }
    }

    /** 主题 cookie → 内置主题 / 本应用自带的自定义主题（主题名只认 [A-Za-z0-9_-]，其余忽略）。 */
    private static void applyTheme(PageManager pm, String themeCookie) {
        if (themeCookie == null || themeCookie.isEmpty() || !themeCookie.matches("[A-Za-z0-9_-]+")) {
            return;
        }
        String name = themeCookie.toLowerCase(Locale.ROOT);
        for (Theme theme : Theme.values()) {
            if (theme.getName().equals(name)) {
                pm.theme(name);
                return;
            }
        }
        if (new ClassPathResource("static/res/themes/" + name + "/theme.css").exists()) {
            pm.customTheme(name);
        }
    }

    private boolean isAdminPage(HttpServletRequest request) {
        Class<?> pageClass = registry.pageClass(RouteUtil.routeOf(request, homeRoute));
        return pageClass != null && AppBoxAdminPageBase.class.isAssignableFrom(pageClass);
    }

    private static String cookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie c : cookies) {
            if (name.equals(c.getName())) {
                return c.getValue();
            }
        }
        return null;
    }
}

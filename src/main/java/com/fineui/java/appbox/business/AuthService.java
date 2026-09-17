package com.fineui.java.appbox.business;

import com.fineui.java.appbox.model.Power;
import com.fineui.java.appbox.model.Role;
import com.fineui.java.appbox.model.User;
import com.fineui.java.appbox.repository.PowerRepository;
import com.fineui.java.appbox.repository.RoleRepository;
import com.fineui.java.appbox.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 登录身份与业务权限：登录时把 {@link AppBoxUser} 存入 Spring Security 的会话上下文（相当于签发登录凭据），
 * 登出时清除；权限名列表按「超级管理员全权限 / 其余按角色汇总」解析并缓存在 Session 里，避免每个请求反复查库。
 */
@Service
public class AuthService {

    /** Session 键：当前用户拥有的权限名列表缓存。 */
    private static final String SK_USER_POWER_LIST = "UserPowerList";
    /** Session 键：上述缓存对应的权限版本号（角色权限 / 用户角色一改就升版本，让各会话的缓存失效）。 */
    private static final String SK_USER_POWER_VERSION = "UserPowerVersion";
    /** Session 键：上次复核「用户仍存在且启用」的时间戳。 */
    private static final String SK_ACTIVE_CHECKED_AT = "ActiveCheckedAt";

    /** 复核用户是否仍有效的间隔（秒）。 */
    private static final int ACTIVE_CHECK_INTERVAL_SECONDS = 60;

    /** Spring Security 角色名：超级管理员（用于保护 H2 控制台等仅 admin 可用的入口）。 */
    public static final String ROLE_ADMIN = "ADMIN";
    /** Spring Security 角色名：普通登录用户。 */
    public static final String ROLE_USER = "USER";

    /** 全局权限版本号：角色的权限集合、用户的角色集合发生变化时递增，各会话据此丢弃过期的权限缓存。 */
    private static final AtomicLong PERMISSION_VERSION = new AtomicLong(1);

    private final SecurityContextRepository securityContextRepository;
    private final RoleRepository roleRepository;
    private final PowerRepository powerRepository;
    private final UserRepository userRepository;
    private final OnlineService onlineService;

    public AuthService(SecurityContextRepository securityContextRepository, RoleRepository roleRepository,
                       PowerRepository powerRepository, UserRepository userRepository, OnlineService onlineService) {
        this.securityContextRepository = securityContextRepository;
        this.roleRepository = roleRepository;
        this.powerRepository = powerRepository;
        this.userRepository = userRepository;
        this.onlineService = onlineService;
    }

    /** 登录成功：建立会话身份 + 登记在线记录。须在请求线程内调用（登录页的按钮事件里）。 */
    public void login(User user) {
        HttpServletRequest request = currentRequest();
        HttpServletResponse response = currentResponse();
        List<Integer> roleIds = new ArrayList<>();
        for (Role role : user.getRoles()) {
            roleIds.add(role.getId());
        }
        AppBoxUser principal = new AppBoxUser(user.getId(), user.getName(), roleIds);
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + ROLE_USER));
        if (principal.isAdmin()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + ROLE_ADMIN));
        }
        // 登录成功先换掉会话 ID：登录前的匿名会话可能已被别人预先植入（会话固定攻击）。
        // 没有会话时（登录页本身不建会话）无需也不能轮换，直接新建。
        if (request.getSession(false) != null) {
            request.changeSessionId();
        } else {
            request.getSession(true);
        }
        Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(principal, null, authorities);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);

        HttpSession session = request.getSession(true);
        // changeSessionId 会保留原会话的全部属性：与身份绑定的缓存必须显式清掉，避免换身份后沿用旧值
        session.removeAttribute(SK_USER_POWER_LIST);
        session.removeAttribute(SK_USER_POWER_VERSION);
        session.removeAttribute(SK_ACTIVE_CHECKED_AT);
        onlineService.register(user.getId(), request.getRemoteAddr(), session);
    }

    /** 登出：清除会话身份并作废 Session。 */
    public void logout() {
        HttpServletRequest request = currentRequest();
        HttpServletResponse response = currentResponse();
        new SecurityContextLogoutHandler().logout(request, response,
                SecurityContextHolder.getContext().getAuthentication());
    }

    /** 当前登录用户；未登录返回 {@code null}。 */
    public static AppBoxUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AppBoxUser user) {
            return user;
        }
        return null;
    }

    public boolean isAuthenticated() {
        return currentUser() != null;
    }

    public Integer getIdentityId() {
        AppBoxUser user = currentUser();
        return user == null ? null : user.getId();
    }

    public String getIdentityName() {
        AppBoxUser user = currentUser();
        return user == null ? null : user.getName();
    }

    public List<Integer> getIdentityRoleIds() {
        AppBoxUser user = currentUser();
        return user == null ? List.of() : user.getRoleIds();
    }

    /** 当前用户所属角色的名称列表（超级管理员固定为「超级管理员」）。 */
    public List<String> getIdentityRoleNames() {
        AppBoxUser user = currentUser();
        if (user == null) {
            return List.of();
        }
        if (user.isAdmin()) {
            return List.of("超级管理员");
        }
        List<String> names = new ArrayList<>();
        for (Role role : roleRepository.findAllById(user.getRoleIds())) {
            names.add(role.getName());
        }
        return names;
    }

    /** 当前用户拥有的全部权限名（Session 缓存；超级管理员拥有全部权限）。 */
    @SuppressWarnings("unchecked")
    public List<String> getRolePowerNames() {
        AppBoxUser user = currentUser();
        if (user == null) {
            return List.of();
        }
        HttpSession session = currentRequest().getSession(true);
        Object cached = session.getAttribute(SK_USER_POWER_LIST);
        Object cachedVersion = session.getAttribute(SK_USER_POWER_VERSION);
        if (cached instanceof List<?> list && Long.valueOf(PERMISSION_VERSION.get()).equals(cachedVersion)) {
            return List.copyOf((List<String>) list);
        }
        List<String> powerNames = new ArrayList<>();
        if (user.isAdmin()) {
            for (Power power : powerRepository.findAll()) {
                powerNames.add(power.getName());
            }
        } else {
            // 按用户主键重查角色，而不是用登录快照里的 roleIds——管理员改了某人的角色后，
            // 其在线会话下次解析权限就能拿到新角色集合（配合版本号让缓存失效）
            List<Role> roles = userRepository.findById(user.getId()).map(User::getRoles).orElse(List.of());
            for (Role role : roles) {
                for (Power power : role.getPowers()) {
                    if (!powerNames.contains(power.getName())) {
                        powerNames.add(power.getName());
                    }
                }
            }
        }
        session.setAttribute(SK_USER_POWER_LIST, new ArrayList<>(powerNames));
        session.setAttribute(SK_USER_POWER_VERSION, PERMISSION_VERSION.get());
        return powerNames;
    }

    /**
     * 角色的权限集合、用户的角色集合被修改后调用：升版本号，让本进程内所有会话下次请求重新解析权限。
     *
     * <p>版本号是进程内的静态计数：多实例部署时各实例互不通知，被改动的那台之外仍会用旧缓存到会话结束；
     * 真要做集群，把它换成共享存储（如 Redis）里的一个计数或按用户的失效标记。
     */
    public static void invalidatePermissionCaches() {
        PERMISSION_VERSION.incrementAndGet();
    }

    /**
     * 该会话身份对应的用户是否仍然存在且启用。每请求都要判，故按 {@value #ACTIVE_CHECK_INTERVAL_SECONDS} 秒
     * 在 Session 里缓存一次结果——禁用/删除最迟在这个间隔后生效。
     */
    public boolean isActiveUser(AppBoxUser user) {
        HttpSession session = currentRequest().getSession(true);
        Object checkedAt = session.getAttribute(SK_ACTIVE_CHECKED_AT);
        long now = System.currentTimeMillis();
        if (checkedAt instanceof Long last && now - last < ACTIVE_CHECK_INTERVAL_SECONDS * 1000L) {
            return true;
        }
        boolean active = userRepository.findById(user.getId()).map(User::isEnabled).orElse(false);
        if (active) {
            session.setAttribute(SK_ACTIVE_CHECKED_AT, now);
        }
        return active;
    }

    /** 当前用户是否拥有某权限（权限名为空视为放行）。 */
    public boolean checkPower(String powerName) {
        if (powerName == null || powerName.isEmpty()) {
            return true;
        }
        return getRolePowerNames().contains(powerName);
    }

    static HttpServletRequest currentRequest() {
        RequestAttributes attrs = RequestContextHolder.currentRequestAttributes();
        return ((ServletRequestAttributes) attrs).getRequest();
    }

    static HttpServletResponse currentResponse() {
        RequestAttributes attrs = RequestContextHolder.currentRequestAttributes();
        return ((ServletRequestAttributes) attrs).getResponse();
    }
}

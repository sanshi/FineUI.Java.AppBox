package com.fineui.java.appbox.business;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fineui.java.core.FineUIPageBase;
import com.fineui.java.core.GridCommandEventArgs;
import com.fineui.java.core.controls.Grid;
import com.fineui.java.core.MessageBoxIcon;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * 全站页面基类：登录身份、权限校验、表格分页排序助手、自定义事件参数解析。
 * 页面是 Spring 管理的 prototype Bean，公共服务在此按字段注入，子类只需构造器注入自己的数据访问对象。
 */
public abstract class AppBoxPageBase extends FineUIPageBase {

    @Autowired
    protected AuthService authService;

    @Autowired
    protected ConfigService configService;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${app.version}")
    private String productVersion;

    /** 每页条数的上界（客户端可篡改，服务端封顶）。 */
    private static final int MAX_PAGE_SIZE = 200;

    // —— 当前登录用户 ——

    protected Integer getIdentityId() {
        return authService.getIdentityId();
    }

    protected String getIdentityName() {
        return authService.getIdentityName();
    }

    protected List<Integer> getIdentityRoleIds() {
        return authService.getIdentityRoleIds();
    }

    protected List<String> getIdentityRoleNames() {
        return authService.getIdentityRoleNames();
    }

    protected List<String> getRolePowerNames() {
        return authService.getRolePowerNames();
    }

    // —— 权限校验 ——

    /** 当前用户是否拥有某权限。 */
    protected boolean checkPower(String powerName) {
        return authService.checkPower(powerName);
    }

    /** 回发事件里的操作级校验失败：在顶层窗口弹提示。 */
    protected void checkPowerFailWithAlert() {
        showAlertInTop(AppBoxInterceptor.CHECK_POWER_FAIL_ACTION_MESSAGE, "", MessageBoxIcon.Warning);
    }

    // —— 环境 ——

    protected String getProductVersion() {
        return productVersion;
    }

    protected static HttpServletRequest currentRequest() {
        return AuthService.currentRequest();
    }

    protected static HttpSession currentSession() {
        return AuthService.currentRequest().getSession(true);
    }

    // —— 表格分页 / 排序 ——

    /**
     * 按表格当前的页码、每页条数、排序字段执行分页查询，并把总记录数写回表格。
     * 若请求的页码已越界（如删掉末页最后一行后），自动回退到最后一页重新查询。
     */
    protected <T> Page<T> loadPage(Grid grid, Function<Pageable, Page<T>> query) {
        return loadPage(grid, sortOf(grid), query);
    }

    /**
     * 同上，但排序由调用方给出——列显示的是派生属性（如在线统计的用户名由 user 导航派生）时，
     * 要把它映射成真实的关联属性路径再传进来。
     */
    protected <T> Page<T> loadPage(Grid grid, Sort explicitSort, Function<Pageable, Page<T>> query) {
        // 每页条数随客户端状态往返、可被改大：封顶，避免一次拉回整张表
        int pageSize = Math.min(grid.getPageSize() > 0 ? grid.getPageSize() : 20, MAX_PAGE_SIZE);
        int pageIndex = Math.max(grid.getPageIndex(), 0);
        Sort sort = explicitSort == null ? Sort.unsorted() : explicitSort;
        Page<T> page;
        try {
            page = query.apply(PageRequest.of(pageIndex, pageSize, sort));
        } catch (RuntimeException ex) {
            // 排序字段来自客户端状态：可能不是实体属性（如只读派生属性 deptName、集合属性 roles），
            // 各持久化层抛的异常类型不一，一律退回不排序重试一次，不把实体结构当报错回显出去
            if (sort.isUnsorted()) {
                throw ex;
            }
            sort = Sort.unsorted();
            page = query.apply(PageRequest.of(pageIndex, pageSize));
        }
        if (page.getTotalElements() > 0 && pageIndex >= page.getTotalPages()) {
            pageIndex = page.getTotalPages() - 1;
            grid.setPageIndex(pageIndex);
            page = query.apply(PageRequest.of(pageIndex, pageSize, sort));
        }
        grid.setRecordCount((int) page.getTotalElements());
        return page;
    }

    /** 表格当前排序（未指定排序字段、或字段名不是简单标识符时不排序——排序字段随客户端状态往返，不可信）。 */
    protected Sort sortOf(Grid grid) {
        String sortField = grid.getSortField();
        if (sortField == null || sortField.isEmpty() || !sortField.matches("[A-Za-z][A-Za-z0-9_]*")) {
            return Sort.unsorted();
        }
        Sort.Direction direction = "DESC".equalsIgnoreCase(grid.getSortDirection())
                ? Sort.Direction.DESC : Sort.Direction.ASC;
        return Sort.by(direction, sortField);
    }

    /**
     * 行命令事件里取该行的整型主键（{@code data-id-field} 的值随事件回传）。
     * 不要按行号去 {@code getDataKeys()} 里回查：树形表格的行号是层内序号，与扁平数据源顺序不一致。
     */
    protected Integer getRowId(GridCommandEventArgs e) {
        String rowId = e.getRowID();
        if (rowId == null || rowId.isEmpty()) {
            return null;
        }
        try {
            return Integer.valueOf(rowId);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /**
     * 把控件里取到的字符串当整数读；空、非数字一律返回 {@code null}。
     * 控件值随客户端状态往返、可被改成任意串，直接 {@code Integer.valueOf} 会抛出异常变成 500。
     */
    protected static Integer intOrNull(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return Integer.valueOf(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** 同上，解析不出整数时用给定的默认值（如每页条数）。 */
    protected static int intOrDefault(String value, int defaultValue) {
        Integer parsed = intOrNull(value);
        return parsed == null ? defaultValue : parsed;
    }

    /**
     * 图标地址除了要是安全 URL，还不能含能逃出 CSS {@code url(...)} 的字符——树节点的图标是拼进内联
     * {@code style="background-image:url(…)"} 的，括号/分号/冒号逃出去就能往页面上铺一层覆盖内容。
     */
    protected static boolean isSafeIconUrl(String url) {
        if (!isSafeUrl(url)) {
            return false;
        }
        if (url == null || url.isEmpty()) {
            return true;
        }
        String u = url.trim();
        int schemeEnd = u.indexOf("://");
        String rest = schemeEnd < 0 ? u : u.substring(schemeEnd + 3);
        return rest.indexOf('(') < 0 && rest.indexOf(')') < 0 && rest.indexOf(';') < 0 && rest.indexOf(':') < 0;
    }

    /** 站内相对路径（以 {@code /} 开头）或 http(s) 绝对地址才算安全，拒绝 {@code javascript:} / {@code data:} 等伪协议。 */
    protected static boolean isSafeUrl(String url) {
        if (url == null || url.isEmpty()) {
            return true;
        }
        String u = url.trim();
        // 反斜杠开头的「协议相对」变体（浏览器会把 /\ 规范化成 //）同样是站外地址
        if (u.startsWith("//") || u.startsWith("/\\")) {
            return false;
        }
        if (u.startsWith("/")) {
            return true;
        }
        String lower = u.toLowerCase(java.util.Locale.ROOT);
        return lower.startsWith("http://") || lower.startsWith("https://");
    }

    // —— 自定义事件参数 ——

    /** 把客户端 {@code F.customEvent(name, 参数对象)} 传来的 JSON 参数解析成树。 */
    protected JsonNode parseJson(String json) {
        try {
            return objectMapper.readTree(json == null || json.isEmpty() ? "{}" : json);
        } catch (Exception e) {
            throw new IllegalArgumentException("自定义事件参数不是合法的 JSON：" + json, e);
        }
    }

    /** JSON 数组 → 整型列表（用于 {@code rowIDs} 这类主键数组）。 */
    protected static List<Integer> toIntList(JsonNode array) {
        List<Integer> list = new ArrayList<>();
        if (array != null && array.isArray()) {
            for (JsonNode node : array) {
                list.add(node.asInt());
            }
        }
        return list;
    }
}

package com.fineui.java.appbox.business;

import com.fineui.java.appbox.model.Config;
import com.fineui.java.appbox.repository.ConfigRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 全局配置（Config 表）的进程内缓存：首次访问整表读入，之后直接从内存取；修改先写缓存并记下改动的键，
 * {@link #saveAll()} 再把这些键批量落库并清空缓存。单例 Bean，读写加锁保证一致。
 */
@Service
public class ConfigService {

    private final ConfigRepository configRepository;
    private final ConfigWriter configWriter;

    private List<Config> configs;
    private final Set<String> changedKeys = new HashSet<>();

    public ConfigService(ConfigRepository configRepository, ConfigWriter configWriter) {
        this.configRepository = configRepository;
        this.configWriter = configWriter;
    }

    /** 全部配置项（缓存为空时读库）。 */
    public synchronized List<Config> getConfigs() {
        if (configs == null) {
            configs = new ArrayList<>(configRepository.findAll());
        }
        return configs;
    }

    /** 清空缓存，下次访问重新读库。 */
    public synchronized void reload() {
        configs = null;
    }

    public synchronized String getValue(String key) {
        for (Config c : getConfigs()) {
            if (key.equals(c.getConfigKey())) {
                return c.getConfigValue();
            }
        }
        return null;
    }

    /** 写缓存并记下改动的键（值没变则不记），落库要调 {@link #saveAll()}。 */
    public synchronized void setValue(String key, String value) {
        for (Config c : getConfigs()) {
            if (key.equals(c.getConfigKey())) {
                if (value != null && !value.equals(c.getConfigValue())) {
                    changedKeys.add(key);
                    c.setConfigValue(value);
                }
                return;
            }
        }
    }

    /** 把改动过的键写回数据库（一个事务写完再清缓存），然后清空改动记录。全程持锁，不给并发读留下把旧值缓存回来的窗口。 */
    public synchronized void saveAll() {
        if (changedKeys.isEmpty()) {
            return;
        }
        Map<String, String> values = new LinkedHashMap<>();
        for (String key : changedKeys) {
            values.put(key, getValue(key));
        }
        try {
            configWriter.write(values);   // 返回即已提交
        } finally {
            // 写失败时缓存里可能残留库中不存在的值：一律清掉重新读，宁可多读一次也不让缓存领先于库
            changedKeys.clear();
            reload();
        }
    }

    // —— 强类型访问 ——

    /** 网站标题。 */
    public String getTitle() {
        return getValue("Title");
    }

    public void setTitle(String title) {
        setValue("Title", title);
    }

    /** 表格每页显示的条数（配置缺失或非法时回落 20）。 */
    public int getPageSize() {
        String v = getValue("PageSize");
        try {
            return Integer.parseInt(v.trim());
        } catch (RuntimeException e) {
            return 20;
        }
    }

    public void setPageSize(int pageSize) {
        setValue("PageSize", String.valueOf(pageSize));
    }

    /** 帮助下拉菜单的 JSON 数组字符串。 */
    public String getHelpList() {
        return getValue("HelpList");
    }

    public void setHelpList(String helpList) {
        setValue("HelpList", helpList);
    }

    /** 网站主题。 */
    public String getTheme() {
        return getValue("Theme");
    }

    public void setTheme(String theme) {
        setValue("Theme", theme);
    }
}

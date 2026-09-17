package com.fineui.java.appbox.business;

import com.fineui.java.appbox.repository.ConfigRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * 全局配置的写库动作，单独成一个组件：让事务边界落在方法上、方法返回即已提交，
 * {@link ConfigService#saveAll()} 才能在提交之后再清缓存（同一个类里加事务注解不会生效，且提交发生在方法之外）。
 */
@Component
class ConfigWriter {

    private final ConfigRepository configRepository;

    ConfigWriter(ConfigRepository configRepository) {
        this.configRepository = configRepository;
    }

    /**
     * 把这批「键 → 新值」写回数据库（一个事务，全部成功或全部不写）。
     *
     * <p>必须是 public：事务注解默认只对 public 方法织入，包级私有会静默失去事务、退化成逐条各自提交。
     */
    @Transactional
    public void write(Map<String, String> values) {
        for (Map.Entry<String, String> e : values.entrySet()) {
            configRepository.findByConfigKey(e.getKey()).ifPresent(c -> {
                c.setConfigValue(e.getValue());
                configRepository.save(c);
            });
        }
    }
}

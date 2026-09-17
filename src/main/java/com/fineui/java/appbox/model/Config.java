package com.fineui.java.appbox.model;

import com.fineui.java.binding.Display;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;

/** 全局配置项（键值对）：网站标题、表格每页条数、帮助菜单 JSON 等；由 {@code ConfigService} 缓存读取。 */
@Entity
@Table(name = "configs")
public class Config implements KeyId {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Display(name = "键")
    @NotBlank(message = "键不能为空！")
    @Column(length = 50, nullable = false)
    private String configKey;

    @Display(name = "值")
    @NotBlank(message = "值不能为空！")
    @Column(length = 4000, nullable = false)
    private String configValue;

    @Display(name = "备注")
    @Column(length = 500)
    private String remark;

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    public String getConfigKey() {
        return configKey;
    }

    public void setConfigKey(String configKey) {
        this.configKey = configKey;
    }

    public String getConfigValue() {
        return configValue;
    }

    public void setConfigValue(String configValue) {
        this.configValue = configValue;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}

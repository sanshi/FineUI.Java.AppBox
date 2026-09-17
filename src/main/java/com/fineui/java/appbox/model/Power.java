package com.fineui.java.appbox.model;

import com.fineui.java.binding.Display;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;

import java.util.ArrayList;
import java.util.List;

/** 权限（操作点）。{@code name} 是代码里校验用的权限标识（如 {@code CoreUserView}），{@code groupName} 用于角色权限页按组渲染。 */
@Entity
@Table(name = "powers")
public class Power implements KeyId {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Display(name = "名称")
    @NotBlank(message = "名称不能为空！")
    @Column(length = 50, nullable = false)
    private String name;

    @Display(name = "分组名称")
    @NotBlank(message = "分组名称不能为空！")
    @Column(length = 50, nullable = false)
    private String groupName;

    @Display(name = "标题")
    @NotBlank(message = "标题不能为空！")
    @Column(length = 200, nullable = false)
    private String title;

    @Display(name = "备注")
    @Column(length = 500)
    private String remark;

    @ManyToMany(mappedBy = "powers")
    private List<Role> roles = new ArrayList<>();

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public List<Role> getRoles() {
        return roles;
    }

    public void setRoles(List<Role> roles) {
        this.roles = roles;
    }
}

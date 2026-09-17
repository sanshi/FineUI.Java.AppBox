package com.fineui.java.appbox.model;

import com.fineui.java.binding.Display;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * 部门（自引用树）。{@code parentId} 是真实外键列（表单下拉树直接绑定它、表格树形列按它挂父子），
 * {@code parent} / {@code children} 是只读导航。
 */
@Entity
@Table(name = "depts")
public class Dept implements KeyId {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Display(name = "名称")
    @NotBlank(message = "名称不能为空！")
    @Column(length = 50, nullable = false)
    private String name;

    @Display(name = "排序")
    @NotNull(message = "排序不能为空！")
    @Column(nullable = false)
    private Integer sortIndex;

    @Display(name = "备注")
    @Column(length = 500)
    private String remark;

    @Display(name = "上级部门")
    @Column(name = "parent_id")
    private Integer parentId;

    @ManyToOne
    @JoinColumn(name = "parent_id", insertable = false, updatable = false)
    private Dept parent;

    @OneToMany(mappedBy = "parent")
    @OrderBy("sortIndex ASC")
    private List<Dept> children = new ArrayList<>();

    @OneToMany(mappedBy = "dept")
    private List<User> users = new ArrayList<>();

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

    public Integer getSortIndex() {
        return sortIndex;
    }

    public void setSortIndex(Integer sortIndex) {
        this.sortIndex = sortIndex;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public Integer getParentId() {
        return parentId;
    }

    public void setParentId(Integer parentId) {
        this.parentId = parentId;
    }

    public Dept getParent() {
        return parent;
    }

    public void setParent(Dept parent) {
        this.parent = parent;
    }

    public List<Dept> getChildren() {
        return children;
    }

    public void setChildren(List<Dept> children) {
        this.children = children;
    }

    public List<User> getUsers() {
        return users;
    }

    public void setUsers(List<User> users) {
        this.users = users;
    }
}

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
 * 菜单（自引用树）。主框架页左侧菜单树由本表生成；{@code viewPowerId} 指向浏览该菜单所需的权限，为空表示所有登录用户可见。
 * {@code parentId} / {@code viewPowerId} 是真实外键列（表单直接绑定），{@code parent} / {@code viewPower} / {@code children} 是只读导航。
 */
@Entity
@Table(name = "menus")
public class Menu implements KeyId {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Display(name = "菜单名称")
    @NotBlank(message = "菜单名称不能为空！")
    @Column(length = 50, nullable = false)
    private String name;

    @Display(name = "图标")
    @Column(length = 200)
    private String imageUrl;

    @Display(name = "链接")
    @Column(length = 200)
    private String navigateUrl;

    @Display(name = "备注")
    @Column(length = 500)
    private String remark;

    @Display(name = "排序")
    @NotNull(message = "排序不能为空！")
    @Column(nullable = false)
    private Integer sortIndex;

    @Display(name = "上级菜单")
    @Column(name = "parent_id")
    private Integer parentId;

    @ManyToOne
    @JoinColumn(name = "parent_id", insertable = false, updatable = false)
    private Menu parent;

    @Display(name = "浏览权限")
    @Column(name = "view_power_id")
    private Integer viewPowerId;

    @ManyToOne
    @JoinColumn(name = "view_power_id", insertable = false, updatable = false)
    private Power viewPower;

    @OneToMany(mappedBy = "parent")
    @OrderBy("sortIndex ASC")
    private List<Menu> children = new ArrayList<>();

    /** 浏览权限名称（由只读导航派生，供表格列直接绑定）。 */
    @Display(name = "浏览权限")
    public String getViewPowerName() {
        return viewPower == null ? null : viewPower.getName();
    }

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

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getNavigateUrl() {
        return navigateUrl;
    }

    public void setNavigateUrl(String navigateUrl) {
        this.navigateUrl = navigateUrl;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public Integer getSortIndex() {
        return sortIndex;
    }

    public void setSortIndex(Integer sortIndex) {
        this.sortIndex = sortIndex;
    }

    public Integer getParentId() {
        return parentId;
    }

    public void setParentId(Integer parentId) {
        this.parentId = parentId;
    }

    public Menu getParent() {
        return parent;
    }

    public void setParent(Menu parent) {
        this.parent = parent;
    }

    public Integer getViewPowerId() {
        return viewPowerId;
    }

    public void setViewPowerId(Integer viewPowerId) {
        this.viewPowerId = viewPowerId;
    }

    public Power getViewPower() {
        return viewPower;
    }

    public void setViewPower(Power viewPower) {
        this.viewPower = viewPower;
    }

    public List<Menu> getChildren() {
        return children;
    }

    public void setChildren(List<Menu> children) {
        this.children = children;
    }
}

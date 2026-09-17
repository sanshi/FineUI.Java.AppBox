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

/** 职称。与用户多对多（维护方在 {@link User}）。 */
@Entity
@Table(name = "titles")
public class Title implements KeyId {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Display(name = "名称")
    @NotBlank(message = "名称不能为空！")
    @Column(length = 50, nullable = false)
    private String name;

    @Display(name = "备注")
    @Column(length = 500)
    private String remark;

    @ManyToMany(mappedBy = "titles")
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

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public List<User> getUsers() {
        return users;
    }

    public void setUsers(List<User> users) {
        this.users = users;
    }
}

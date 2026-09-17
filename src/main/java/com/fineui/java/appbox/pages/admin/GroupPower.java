package com.fineui.java.appbox.pages.admin;

import com.fineui.java.binding.Display;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** 角色权限页右侧表格的行模型：一个分组下的全部权限（每项 id / name / title，由客户端渲染成复选框）。 */
public class GroupPower {

    @Display(name = "分组名称")
    private String groupName;

    @Display(name = "权限")
    private List<Map<String, Object>> powers = new ArrayList<>();

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public List<Map<String, Object>> getPowers() {
        return powers;
    }

    public void setPowers(List<Map<String, Object>> powers) {
        this.powers = powers;
    }
}

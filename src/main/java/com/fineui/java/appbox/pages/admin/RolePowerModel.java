package com.fineui.java.appbox.pages.admin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fineui.java.appbox.business.AppBoxAdminPageBase;
import com.fineui.java.appbox.business.AuthService;
import com.fineui.java.appbox.business.CheckPower;
import com.fineui.java.appbox.model.Power;
import com.fineui.java.appbox.model.Role;
import com.fineui.java.appbox.repository.PowerRepository;
import com.fineui.java.appbox.repository.RoleRepository;
import com.fineui.java.core.CustomEventArgs;
import com.fineui.java.core.EventArgs;
import com.fineui.java.core.FineUIPage;
import com.fineui.java.core.MessageBoxIcon;
import com.fineui.java.core.controls.Button;
import com.fineui.java.core.controls.Grid;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 角色权限管理（路由 {@code admin/role-power}）：左侧角色列表，右侧按分组列出全部权限（复选框），
 * 加载角色后由客户端按其已有权限勾选；保存时勾选的权限主键经自定义事件回传，整体替换角色的权限集合。
 */
@FineUIPage("admin/role-power")
@CheckPower("CoreRolePowerView")
public class RolePowerModel extends AppBoxAdminPageBase {

    Grid Grid1;
    Grid Grid2;
    Button btnGroupUpdate;

    private final RoleRepository roleRepository;
    private final PowerRepository powerRepository;

    private List<Role> roles;
    private List<GroupPower> groupPowers;

    public List<Role> getRoles() {
        return roles;
    }

    public List<GroupPower> getGroupPowers() {
        return groupPowers;
    }

    public RolePowerModel(RoleRepository roleRepository, PowerRepository powerRepository) {
        this.roleRepository = roleRepository;
        this.powerRepository = powerRepository;
    }

    public void Page_Load(Object sender, EventArgs e) {
        if (!isPostBack()) {
            btnGroupUpdate.setEnabled(checkPower("CoreRolePowerEdit"));
            List<Role> list = loadGrid1Data();
            if (list.isEmpty()) {
                showAlert("请先添加角色！");
                return;
            }
            Grid1.setSelectedRowIdArray(new String[] { String.valueOf(list.get(0).getId()) });
            loadGrid2Data();
        }
    }

    private List<Role> loadGrid1Data() {
        roles = roleRepository.findAll(sortOf(Grid1));
        Grid1.setDataSource(roles);
        Grid1.dataBind();
        return roles;
    }

    /** 右侧：全部权限按分组聚合（按分组名排序方向随表格），再让客户端按当前角色勾选。 */
    private void loadGrid2Data() {
        Integer roleId = selectedRoleId();
        if (roleId == null) {
            Grid2.setDataSource(null);
            Grid2.dataBind();
            return;
        }
        boolean desc = "DESC".equalsIgnoreCase(Grid2.getSortDirection());
        Map<String, List<Power>> grouped = new TreeMap<>(desc ? Comparator.<String>reverseOrder() : Comparator.<String>naturalOrder());
        for (Power power : powerRepository.findAllByOrderByGroupNameAscNameAsc()) {
            grouped.computeIfAbsent(power.getGroupName(), k -> new ArrayList<>()).add(power);
        }
        groupPowers = new ArrayList<>();
        for (Map.Entry<String, List<Power>> entry : grouped.entrySet()) {
            GroupPower group = new GroupPower();
            group.setGroupName(entry.getKey());
            for (Power power : entry.getValue()) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", power.getId());
                item.put("name", power.getName());
                item.put("title", power.getTitle());
                group.getPowers().add(item);
            }
            groupPowers.add(group);
        }
        Grid2.setDataSource(groupPowers);
        Grid2.dataBind();

        // 让客户端按当前角色已有的权限勾选复选框
        List<Integer> powerIds = new ArrayList<>();
        roleRepository.findById(roleId).ifPresent(role -> {
            for (Power power : role.getPowers()) {
                powerIds.add(power.getId());
            }
        });
        invokeClientFunction("updateRolePowers", powerIds);
    }

    private Integer selectedRoleId() {
        String selected = Grid1.getSelectedRowId();
        return intOrNull(selected);
    }

    public void Grid1_Sort(Object sender, EventArgs e) {
        List<Role> list = loadGrid1Data();
        if (!list.isEmpty()) {
            Grid1.setSelectedRowIdArray(new String[] { String.valueOf(list.get(0).getId()) });
        }
        loadGrid2Data();
    }

    public void Grid1_RowClick(Object sender, EventArgs e) {
        loadGrid2Data();
    }

    public void Grid2_Sort(Object sender, EventArgs e) {
        loadGrid2Data();
    }

    public void Page_CustomEvent(Object sender, CustomEventArgs e) {
        if (!"Grid2_SavePowers".equals(e.getEventName())) {
            return;
        }
        if (!checkPower("CoreRolePowerEdit")) {
            checkPowerFailWithAlert();
            return;
        }
        Integer roleId = selectedRoleId();
        if (roleId == null) {
            return;
        }
        JsonNode args = parseJson(e.getArgument());
        List<Integer> powerIds = toIntList(args.get("powerIDs"));
        Role role = roleRepository.findById(roleId).orElse(null);
        if (role == null) {
            showAlert("该角色不存在或已被删除！");
            return;
        }
        // 角色是权限关系的维护方：整体替换角色的权限集合
        role.getPowers().clear();
        role.getPowers().addAll(powerRepository.findAllById(powerIds));
        roleRepository.save(role);
        AuthService.invalidatePermissionCaches();   // 该角色下的在线用户下次请求即按新权限判定
        showAlertInTop("当前角色的权限更新成功！", "", MessageBoxIcon.Information);
    }
}

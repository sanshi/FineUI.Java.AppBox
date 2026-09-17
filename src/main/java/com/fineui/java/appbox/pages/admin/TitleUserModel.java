package com.fineui.java.appbox.pages.admin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fineui.java.appbox.business.AppBoxAdminPageBase;
import com.fineui.java.appbox.business.CheckPower;
import com.fineui.java.appbox.model.Title;
import com.fineui.java.appbox.model.User;
import com.fineui.java.appbox.repository.TitleRepository;
import com.fineui.java.appbox.repository.UserRepository;
import com.fineui.java.core.CustomEventArgs;
import com.fineui.java.core.EventArgs;
import com.fineui.java.core.FineUIPage;
import com.fineui.java.core.GridCommandEventArgs;
import com.fineui.java.core.controls.Button;
import com.fineui.java.core.controls.DropDownList;
import com.fineui.java.core.controls.Grid;
import com.fineui.java.core.controls.TwinTriggerBox;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * 职称用户管理（路由 {@code admin/title-user}）：左侧职称列表，右侧当前职称下的用户（搜索 + 分页排序），
 * 可从职称移除选中用户、或弹窗添加用户到当前职称。
 */
@FineUIPage("admin/title-user")
@CheckPower("CoreTitleUserView")
public class TitleUserModel extends AppBoxAdminPageBase {

    Grid Grid1;
    Grid Grid2;
    TwinTriggerBox ttbSearchMessage;
    DropDownList ddlGridPageSize;
    Button btnNew;
    Button btnDeleteSelected;

    private final TitleRepository titleRepository;
    private final UserRepository userRepository;

    private List<Title> titles;
    private List<User> users;

    public List<Title> getTitles() {
        return titles;
    }

    public List<User> getUsers() {
        return users;
    }

    public TitleUserModel(TitleRepository titleRepository, UserRepository userRepository) {
        this.titleRepository = titleRepository;
        this.userRepository = userRepository;
    }

    public void Page_Load(Object sender, EventArgs e) {
        if (!isPostBack()) {
            btnNew.setEnabled(checkPower("CoreTitleUserNew"));
            boolean powerDelete = checkPower("CoreTitleUserDelete");
            btnDeleteSelected.setEnabled(powerDelete);
            Grid2.findCommand("Delete").setEnabled(powerDelete);

            List<Title> list = loadGrid1Data();
            if (list.isEmpty()) {
                showAlert("请先添加职称！");
                return;
            }
            Grid1.setSelectedRowIdArray(new String[] { String.valueOf(list.get(0).getId()) });

            ddlGridPageSize.setSelectedValue(String.valueOf(configService.getPageSize()));
            Grid2.setPageSize(configService.getPageSize());
            loadGrid2Data();
        }
    }

    /** 左侧：全部职称（按表格排序）。 */
    private List<Title> loadGrid1Data() {
        titles = titleRepository.findAll(sortOf(Grid1));
        Grid1.setDataSource(titles);
        Grid1.dataBind();
        return titles;
    }

    /** 右侧：当前选中职称下的用户（非 admin），可按名称搜索。 */
    private void loadGrid2Data() {
        Integer titleId = selectedTitleId();
        if (titleId == null) {
            Grid2.setDataSource(null);
            Grid2.dataBind();
            return;
        }
        Title title = titleRepository.getReferenceById(titleId);
        String searchText = ttbSearchMessage.getValue().trim();
        Specification<User> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (!searchText.isEmpty()) {
                String pattern = "%" + searchText + "%";
                predicates.add(cb.or(cb.like(root.get("name"), pattern), cb.like(root.get("chineseName"), pattern),
                        cb.like(root.get("englishName"), pattern)));
            }
            predicates.add(cb.notEqual(root.get("name"), "admin"));
            predicates.add(cb.isMember(title, root.get("titles")));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Page<User> page = loadPage(Grid2, pageable -> userRepository.findAll(spec, pageable));
        users = page.getContent();
        Grid2.setDataSource(users);
        Grid2.dataBind();
    }

    private Integer selectedTitleId() {
        String selected = Grid1.getSelectedRowId();
        return intOrNull(selected);
    }

    public void Grid1_Sort(Object sender, EventArgs e) {
        List<Title> list = loadGrid1Data();
        if (!list.isEmpty()) {
            Grid1.setSelectedRowIdArray(new String[] { String.valueOf(list.get(0).getId()) });
        }
        loadGrid2Data();
    }

    public void Grid1_RowClick(Object sender, EventArgs e) {
        loadGrid2Data();
    }

    public void Window1_Close(Object sender, EventArgs e) {
        loadGrid2Data();
    }

    public void Grid2_Sort(Object sender, EventArgs e) {
        loadGrid2Data();
    }

    public void Grid2_PageIndexChanged(Object sender, EventArgs e) {
        loadGrid2Data();
    }

    public void ddlGridPageSize_SelectedIndexChanged(Object sender, EventArgs e) {
        Grid2.setPageSize(intOrDefault(ddlGridPageSize.getSelectedValue(), configService.getPageSize()));
        loadGrid2Data();
    }

    public void ttbSearchMessage_Trigger1Click(Object sender, EventArgs e) {
        ttbSearchMessage.setValue("");
        ttbSearchMessage.setShowTrigger1(false);
        loadGrid2Data();
    }

    public void ttbSearchMessage_Trigger2Click(Object sender, EventArgs e) {
        ttbSearchMessage.setShowTrigger1(true);
        loadGrid2Data();
    }

    public void Page_CustomEvent(Object sender, CustomEventArgs e) {
        if ("Grid2_DeleteRows".equals(e.getEventName())) {
            JsonNode args = parseJson(e.getArgument());
            deleteRows(toIntList(args.get("rowIDs")));
        }
    }

    /** 把这些用户从当前职称移除（用户是职称关系的维护方，改用户的职称集合）。 */
    private void deleteRows(List<Integer> userIds) {
        Integer titleId = selectedTitleId();
        if (titleId == null) {
            return;
        }
        if (!checkPower("CoreTitleUserDelete")) {
            checkPowerFailWithAlert();
            return;
        }
        List<User> selected = userRepository.findAllById(userIds);
        for (User user : selected) {
            user.getTitles().removeIf(r -> titleId.equals(r.getId()));
        }
        userRepository.saveAll(selected);
        loadGrid2Data();
    }

    public void Grid2_RowCommand(Object sender, GridCommandEventArgs e) {
        if ("Delete".equals(e.getCommandName())) {
            Integer rowId = getRowId(e);
            if (rowId != null) {
                deleteRows(List.of(rowId));
            }
        }
    }
}

package com.fineui.java.appbox.pages.admin;

import com.fineui.java.appbox.business.AdminPageBase;
import com.fineui.java.appbox.business.CheckPower;
import com.fineui.java.appbox.model.Title;
import com.fineui.java.appbox.model.User;
import com.fineui.java.appbox.repository.TitleRepository;
import com.fineui.java.appbox.repository.UserRepository;
import com.fineui.java.binding.HiddenProperty;
import com.fineui.java.core.AbortPageException;
import com.fineui.java.core.ActiveWindow;
import com.fineui.java.core.EventArgs;
import com.fineui.java.core.FineUIPage;
import com.fineui.java.core.controls.DropDownList;
import com.fineui.java.core.controls.Grid;
import com.fineui.java.core.controls.TwinTriggerBox;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * 添加用户到当前职称（路由 {@code admin/title-user-new}，弹窗内打开）：列出尚不属于该职称的用户（可搜索、跨页保持勾选），
 * 勾选后一次加入。职称主键首屏从 {@code ?titleID} 取、之后随状态往返。
 */
@FineUIPage("admin/title-user-new")
@CheckPower("CoreTitleUserNew")
public class TitleUserNewModel extends AdminPageBase {

    Grid Grid1;
    TwinTriggerBox ttbSearchMessage;
    DropDownList ddlGridPageSize;

    @HiddenProperty
    private int titleId;

    private final TitleRepository titleRepository;
    private final UserRepository userRepository;

    private List<User> users;

    public List<User> getUsers() {
        return users;
    }

    public TitleUserNewModel(TitleRepository titleRepository, UserRepository userRepository) {
        this.titleRepository = titleRepository;
        this.userRepository = userRepository;
    }

    public void Page_Get(Object sender, EventArgs e) {
        titleId = getQueryInt("titleID", 0);
        if (!titleRepository.existsById(titleId)) {
            throw new AbortPageException("参数错误！", ActiveWindow.hideReference());
        }
    }

    public void Page_Load(Object sender, EventArgs e) {
        if (!isPostBack()) {
            ddlGridPageSize.setSelectedValue(String.valueOf(configService.getPageSize()));
            Grid1.setPageSize(configService.getPageSize());
            loadData();
        }
    }

    /** 尚不属于当前职称的用户（非 admin），可按名称搜索。 */
    private void loadData() {
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
            predicates.add(cb.isNotMember(title, root.get("titles")));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Page<User> page = loadPage(Grid1, pageable -> userRepository.findAll(spec, pageable));
        users = page.getContent();
        Grid1.setDataSource(users);
        Grid1.dataBind();
    }

    public void btnSaveClose_Click(Object sender, EventArgs e) {
        String[] selectedRowIds = Grid1.getSelectedRowIdArray();
        if (selectedRowIds == null || selectedRowIds.length == 0) {
            showAlert("请至少选择一项！");
            return;
        }
        Title title = titleRepository.findById(titleId).orElse(null);
        if (title == null) {
            showAlert("该职称不存在或已被删除！");
            return;
        }
        List<Integer> userIds = new ArrayList<>();
        for (String id : selectedRowIds) {
            Integer userId = intOrNull(id);
            if (userId != null) {
                userIds.add(userId);
            }
        }
        // 用户是职称关系的维护方：把职称加进每个用户的职称集合
        // 列表里排除了 admin，但回传的主键不可信：保存时再挡一次
        List<User> selected = new ArrayList<>(userRepository.findAllById(userIds));
        selected.removeIf(user -> "admin".equals(user.getName()));
        for (User user : selected) {
            boolean has = user.getTitles().stream().anyMatch(r -> r.getId().equals(title.getId()));
            if (!has) {
                user.getTitles().add(title);
            }
        }
        userRepository.saveAll(selected);
        ActiveWindow.hidePostBack();
    }

    public void Grid1_Sort(Object sender, EventArgs e) {
        loadData();
    }

    public void Grid1_PageIndexChanged(Object sender, EventArgs e) {
        loadData();
    }

    public void ddlGridPageSize_SelectedIndexChanged(Object sender, EventArgs e) {
        Grid1.setPageSize(intOrDefault(ddlGridPageSize.getSelectedValue(), configService.getPageSize()));
        loadData();
    }

    public void ttbSearchMessage_Trigger1Click(Object sender, EventArgs e) {
        ttbSearchMessage.setValue("");
        ttbSearchMessage.setShowTrigger1(false);
        loadData();
    }

    public void ttbSearchMessage_Trigger2Click(Object sender, EventArgs e) {
        ttbSearchMessage.setShowTrigger1(true);
        loadData();
    }
}

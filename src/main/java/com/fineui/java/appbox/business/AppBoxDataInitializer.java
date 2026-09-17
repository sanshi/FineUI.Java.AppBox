package com.fineui.java.appbox.business;

import com.fineui.java.appbox.model.Dept;
import com.fineui.java.appbox.model.Menu;
import com.fineui.java.appbox.model.Power;
import com.fineui.java.appbox.repository.ConfigRepository;
import com.fineui.java.appbox.repository.DeptRepository;
import com.fineui.java.appbox.repository.MenuRepository;
import com.fineui.java.appbox.repository.PowerRepository;
import com.fineui.java.appbox.repository.RoleRepository;
import com.fineui.java.appbox.repository.TitleRepository;
import com.fineui.java.appbox.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据库初始化：应用启动、Hibernate 按实体建好表之后执行。库里已有用户即视为已初始化，直接返回（幂等哨兵）；
 * 否则按依赖顺序灌入种子数据：配置 → 部门树 → 用户 → 角色 → 权限 → 职称 → 菜单树（菜单引用已入库的权限主键）。
 * 整个过程在一个事务里，失败即启动失败，不留半套数据。
 */
@Component
public class AppBoxDataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AppBoxDataInitializer.class);

    private final ConfigRepository configRepository;
    private final DeptRepository deptRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PowerRepository powerRepository;
    private final TitleRepository titleRepository;
    private final MenuRepository menuRepository;

    public AppBoxDataInitializer(ConfigRepository configRepository, DeptRepository deptRepository,
                                 UserRepository userRepository, RoleRepository roleRepository,
                                 PowerRepository powerRepository, TitleRepository titleRepository,
                                 MenuRepository menuRepository) {
        this.configRepository = configRepository;
        this.deptRepository = deptRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.powerRepository = powerRepository;
        this.titleRepository = titleRepository;
        this.menuRepository = menuRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) {
            return;
        }
        log.info("数据库为空，开始写入种子数据……");

        configRepository.saveAll(SeedData.configs());
        saveDeptTree(SeedData.depts(), null);
        userRepository.saveAll(SeedData.users());
        roleRepository.saveAll(SeedData.roles());
        List<Power> powers = powerRepository.saveAll(SeedPowers.powers());
        titleRepository.saveAll(SeedData.titles());

        Map<String, Power> powersByName = new HashMap<>();
        for (Power p : powers) {
            powersByName.put(p.getName(), p);
        }
        saveMenuTree(SeedMenus.menus(powersByName), null);

        log.info("种子数据写入完成：用户 {} 个、权限 {} 个、菜单 {} 个。", userRepository.count(), powers.size(), menuRepository.count());
    }

    /** 逐层入库部门树：先存本节点拿到主键，再把主键作为子节点的 parentId 递归下去。 */
    private void saveDeptTree(List<Dept> nodes, Integer parentId) {
        for (Dept node : nodes) {
            node.setParentId(parentId);
            Dept saved = deptRepository.save(node);
            saveDeptTree(node.getChildren(), saved.getId());
        }
    }

    /** 逐层入库菜单树（同部门树）。 */
    private void saveMenuTree(List<Menu> nodes, Integer parentId) {
        for (Menu node : nodes) {
            node.setParentId(parentId);
            Menu saved = menuRepository.save(node);
            saveMenuTree(node.getChildren(), saved.getId());
        }
    }
}

package com.fineui.java.appbox.repository;

import com.fineui.java.appbox.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

/** 用户数据访问：列表页多条件组合查询走 {@link JpaSpecificationExecutor}（搜索词 + 启用状态可叠加）。 */
public interface UserRepository extends JpaRepository<User, Integer>, JpaSpecificationExecutor<User> {

    Optional<User> findByName(String name);

    boolean existsByName(String name);

    /** 属于某角色的用户数（删除角色前校验）。 */
    long countByRolesId(Integer roleId);

    /** 拥有某职称的用户数（删除职称前校验）。 */
    long countByTitlesId(Integer titleId);

    /** 属于某部门的用户数（删除部门前校验）。 */
    long countByDeptId(Integer deptId);
}

package com.fineui.java.appbox.repository;

import com.fineui.java.appbox.model.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/** 用户数据访问：列表页多条件组合查询走 {@link JpaSpecificationExecutor}（搜索词 + 启用状态可叠加）。 */
public interface UserRepository extends JpaRepository<User, Integer>, JpaSpecificationExecutor<User> {

    Optional<User> findByName(String name);

    /** 同一用户的并发登录先锁定用户行，避免同时写入多条在线记录。 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id = :id")
    Optional<User> findByIdForUpdate(@Param("id") Integer id);

    boolean existsByName(String name);

    /** 属于某角色的用户数（删除角色前校验）。 */
    long countByRolesId(Integer roleId);

    /** 拥有某职称的用户数（删除职称前校验）。 */
    long countByTitlesId(Integer titleId);

    /** 属于某部门的用户数（删除部门前校验）。 */
    long countByDeptId(Integer deptId);
}

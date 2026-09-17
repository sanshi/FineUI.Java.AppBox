package com.fineui.java.appbox.repository;

import com.fineui.java.appbox.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface RoleRepository extends JpaRepository<Role, Integer>, JpaSpecificationExecutor<Role> {

    List<Role> findAllByOrderByNameAsc();

    /** 使用某权限的角色数（删除权限前校验）。 */
    long countByPowersId(Integer powerId);
}

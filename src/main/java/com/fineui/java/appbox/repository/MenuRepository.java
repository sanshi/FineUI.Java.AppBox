package com.fineui.java.appbox.repository;

import com.fineui.java.appbox.model.Menu;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MenuRepository extends JpaRepository<Menu, Integer> {

    List<Menu> findAllByOrderBySortIndexAsc();

    List<Menu> findByParentIdOrderBySortIndexAsc(Integer parentId);

    List<Menu> findByParentIdIsNullOrderBySortIndexAsc();

    /** 以某权限作浏览权限的菜单数（删除权限前校验）。 */
    long countByViewPowerId(Integer viewPowerId);
}

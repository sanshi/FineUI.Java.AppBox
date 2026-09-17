package com.fineui.java.appbox.repository;

import com.fineui.java.appbox.model.Dept;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeptRepository extends JpaRepository<Dept, Integer> {

    List<Dept> findAllByOrderBySortIndexAsc();

    List<Dept> findByParentIdOrderBySortIndexAsc(Integer parentId);

    List<Dept> findByParentIdIsNullOrderBySortIndexAsc();
}

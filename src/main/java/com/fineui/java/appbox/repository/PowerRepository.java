package com.fineui.java.appbox.repository;

import com.fineui.java.appbox.model.Power;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface PowerRepository extends JpaRepository<Power, Integer>, JpaSpecificationExecutor<Power> {

    Optional<Power> findByName(String name);

    List<Power> findAllByOrderByGroupNameAscNameAsc();
}

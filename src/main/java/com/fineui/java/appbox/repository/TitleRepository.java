package com.fineui.java.appbox.repository;

import com.fineui.java.appbox.model.Title;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface TitleRepository extends JpaRepository<Title, Integer>, JpaSpecificationExecutor<Title> {

    List<Title> findAllByOrderByNameAsc();
}

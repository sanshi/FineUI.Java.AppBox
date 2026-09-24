package com.fineui.java.appbox.repository;

import com.fineui.java.appbox.model.Online;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDateTime;
import java.util.List;

public interface OnlineRepository extends JpaRepository<Online, Integer>, JpaSpecificationExecutor<Online> {

    List<Online> findAllByUserIdOrderByIdAsc(Integer userId);

    long countByUpdateTimeAfter(LocalDateTime time);
}

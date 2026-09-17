package com.fineui.java.appbox.repository;

import com.fineui.java.appbox.model.Online;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDateTime;
import java.util.Optional;

public interface OnlineRepository extends JpaRepository<Online, Integer>, JpaSpecificationExecutor<Online> {

    Optional<Online> findByUserId(Integer userId);

    long countByUpdateTimeAfter(LocalDateTime time);
}

package com.fineui.java.appbox.repository;

import com.fineui.java.appbox.model.Config;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConfigRepository extends JpaRepository<Config, Integer> {

    Optional<Config> findByConfigKey(String configKey);
}

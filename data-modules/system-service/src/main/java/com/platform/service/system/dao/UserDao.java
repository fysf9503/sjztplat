package com.platform.service.system.dao;

import com.platform.service.system.api.entity.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserDao extends JpaRepository<UserEntity, String> {

    UserEntity findByUsername(String username);

    Page<UserEntity> findByUsernameContaining(String username, Pageable pageable);
}

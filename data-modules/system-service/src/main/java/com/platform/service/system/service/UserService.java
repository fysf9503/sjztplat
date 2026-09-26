package com.platform.service.system.service;

import com.platform.service.system.api.entity.UserEntity;
import org.springframework.data.domain.Page;

import java.util.List;

public interface UserService {

    UserEntity findByUsername(String username);

    Page<UserEntity> findPage(int pageNum, int pageSize, String username);

    List<UserEntity> list();

    UserEntity getById(String id);

    UserEntity save(UserEntity entity);

    void updateById(UserEntity entity);

    void deleteById(String id);
}

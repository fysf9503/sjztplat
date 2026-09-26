package com.platform.service.system.service.impl;

import com.platform.service.system.api.entity.UserEntity;
import com.platform.service.system.dao.UserDao;
import com.platform.service.system.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserDao userDao;

    @Override
    public UserEntity findByUsername(String username) {
        return userDao.findByUsername(username);
    }

    @Override
    public Page<UserEntity> findPage(int pageNum, int pageSize, String username) {
        Sort sort = Sort.by(Sort.Direction.DESC, "createTime");
        PageRequest pageable = PageRequest.of(pageNum - 1, pageSize, sort);
        if (username != null && !username.isBlank()) {
            return userDao.findByUsernameContaining(username, pageable);
        }
        return userDao.findAll(pageable);
    }

    @Override
    public List<UserEntity> list() {
        return userDao.findAll();
    }

    @Override
    public UserEntity getById(String id) {
        return userDao.findById(id).orElse(null);
    }

    @Override
    @Transactional
    public UserEntity save(UserEntity entity) {
        return userDao.save(entity);
    }

    @Override
    @Transactional
    public void updateById(UserEntity entity) {
        userDao.save(entity);
    }

    @Override
    @Transactional
    public void deleteById(String id) {
        userDao.deleteById(id);
    }
}

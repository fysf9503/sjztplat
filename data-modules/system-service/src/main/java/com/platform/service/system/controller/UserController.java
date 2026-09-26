package com.platform.service.system.controller;

import com.platform.common.core.R;
import com.platform.service.system.api.entity.UserEntity;
import com.platform.service.system.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@Tag(name = "用户管理")
@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "分页查询用户")
    @GetMapping("/page")
    public R<Page<UserEntity>> page(@RequestParam(value = "pageNum", defaultValue = "1") int pageNum,
                                   @RequestParam(value = "pageSize", defaultValue = "10") int pageSize,
                                   @RequestParam(value = "username", required = false) String username) {
        return R.ok(userService.findPage(pageNum, pageSize, username));
    }

    @Operation(summary = "查询单个用户")
    @GetMapping("/{id}")
    public R<UserEntity> getById(@PathVariable(value = "id") String id) {
        return R.ok(userService.getById(id));
    }

    @Operation(summary = "新增用户")
    @PostMapping
    public R<Void> save(@RequestBody UserEntity user) {
        userService.save(user);
        return R.ok();
    }

    @Operation(summary = "修改用户")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable(value = "id") String id, @RequestBody UserEntity user) {
        user.setId(id);
        userService.updateById(user);
        return R.ok();
    }

    @Operation(summary = "删除用户")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable(value = "id") String id) {
        userService.deleteById(id);
        return R.ok();
    }

    @Operation(summary = "根据用户名查询")
    @GetMapping("/username/{username}")
    public R<UserEntity> getByUsername(@PathVariable(value = "username") String username) {
        return R.ok(userService.findByUsername(username));
    }
}

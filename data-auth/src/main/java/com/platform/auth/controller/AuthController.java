package com.platform.auth.controller;

import com.platform.common.core.R;
import com.platform.common.security.JwtTokenProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "认证管理")
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final JwtTokenProvider jwtTokenProvider;

    public AuthController(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public R<Map<String, String>> login(@RequestBody Map<String, String> loginForm) {
        String username = loginForm.get("username");
        String password = loginForm.get("password");
        // TODO: 对接 system-service 验证用户
        // 此处简化，实际应通过 Feign 调用 system-service 验证密码
        if (!"admin".equals(username) || !"123456".equals(password)) {
            return R.fail("用户名或密码错误");
        }
        String token = jwtTokenProvider.createToken("1", username);
        return R.ok(Map.of("token", token, "username", username));
    }

    @Operation(summary = "用户登出")
    @PostMapping("/logout")
    public R<Void> logout() {
        return R.ok();
    }
}

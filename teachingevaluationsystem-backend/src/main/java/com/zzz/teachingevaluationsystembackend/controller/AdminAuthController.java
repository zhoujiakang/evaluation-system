package com.zzz.teachingevaluationsystembackend.controller;

import com.zzz.teachingevaluationsystembackend.auth.AuthContext;
import com.zzz.teachingevaluationsystembackend.common.ApiResponse;
import com.zzz.teachingevaluationsystembackend.model.dto.request.LoginRequest;
import com.zzz.teachingevaluationsystembackend.model.vo.LoginVO;
import com.zzz.teachingevaluationsystembackend.model.vo.MeVO;
import com.zzz.teachingevaluationsystembackend.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/auth")
public class AdminAuthController {
    private final AuthService authService;

    /**
     * 管理端登录。
     * 实现方式：Controller 读取用户名和密码后调用 AuthService；
     * AuthService 查询用户、校验 BCrypt 密码和管理员权限，通过后使用 Sa-Token 的 admin 逻辑登录并返回 token。
     */
    @PostMapping("/login")
    public ApiResponse<LoginVO> login(@RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.adminLogin(request.getUsername(), request.getPassword()));
    }

    /**
     * 管理端退出登录。
     * 实现方式：Controller 调用 AuthService；AuthService 使用 Sa-Token admin 登录逻辑注销当前会话，
     * 之后统一返回空成功响应。
     */
    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        authService.logoutAdmin();
        return ApiResponse.ok(null);
    }

    /**
     * 校验管理端登录状态。
     * 实现方式：Controller 通过 AuthContext 从 Sa-Token admin 会话中读取当前用户 id，
     * 读取成功说明登录有效，并返回用户 id 与 admin 身份标识。
     */
    @GetMapping("/check")
    public ApiResponse<MeVO> check() {
        return ApiResponse.ok(new MeVO(AuthContext.currentAdminUserId(), "admin"));
    }

}

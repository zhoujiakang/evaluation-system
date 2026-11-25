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
@RequestMapping("/api/student/auth")
public class StudentAuthController {
    private final AuthService authService;

    /**
     * 学生端登录。
     * 实现方式：Controller 读取用户名和密码后调用 AuthService；
     * AuthService 查询用户、校验 BCrypt 密码并确认角色为学生，通过后使用 Sa-Token student 逻辑登录并返回 token。
     */
    @PostMapping("/login")
    public ApiResponse<LoginVO> login(@RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.studentLogin(request.getUsername(), request.getPassword()));
    }

    /**
     * 学生端退出登录。
     * 实现方式：Controller 调用 AuthService；AuthService 使用 Sa-Token student 登录逻辑注销当前学生会话。
     */
    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        authService.logoutStudent();
        return ApiResponse.ok(null);
    }

    /**
     * 校验学生端登录状态。
     * 实现方式：Controller 通过 AuthContext 从 Sa-Token student 会话中读取当前用户 id，
     * 成功读取后返回用户 id 和 student 身份标识。
     */
    @GetMapping("/check")
    public ApiResponse<MeVO> check() {
        return ApiResponse.ok(new MeVO(AuthContext.currentStudentUserId(), "student"));
    }

}

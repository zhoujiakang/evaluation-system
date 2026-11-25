package com.zzz.teachingevaluationsystembackend.controller;

import com.zzz.teachingevaluationsystembackend.auth.AuthContext;
import com.zzz.teachingevaluationsystembackend.common.ApiResponse;
import com.zzz.teachingevaluationsystembackend.model.vo.MeVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/student/me")
public class StudentMeController {
    /**
     * 获取当前学生端用户信息。
     * 实现方式：接口从 Sa-Token student 会话中读取当前登录用户 id，
     * 并用固定的 student 角色标识组装 MeVO 返回给前端。
     */
    @GetMapping
    public ApiResponse<MeVO> me() {
        return ApiResponse.ok(new MeVO(AuthContext.currentStudentUserId(), "student"));
    }
}

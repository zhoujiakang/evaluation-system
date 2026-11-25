package com.zzz.teachingevaluationsystembackend.controller;

import com.zzz.teachingevaluationsystembackend.auth.AuthContext;
import com.zzz.teachingevaluationsystembackend.common.ApiResponse;
import com.zzz.teachingevaluationsystembackend.model.vo.MeVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/me")
public class AdminMeController {
    /**
     * 获取当前管理端用户信息。
     * 实现方式：接口不再查库，直接从 Sa-Token admin 会话中读取当前登录用户 id，
     * 并用固定的 admin 角色标识组装 MeVO 返回。
     */
    @GetMapping
    public ApiResponse<MeVO> me() {
        return ApiResponse.ok(new MeVO(AuthContext.currentAdminUserId(), "admin"));
    }
}

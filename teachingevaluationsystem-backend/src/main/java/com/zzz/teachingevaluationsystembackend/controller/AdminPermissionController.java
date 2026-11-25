package com.zzz.teachingevaluationsystembackend.controller;

import com.zzz.teachingevaluationsystembackend.common.ApiResponse;
import com.zzz.teachingevaluationsystembackend.service.AdminPermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminPermissionController {
    private final AdminPermissionService service;

    /**
     * 授予用户管理员权限。
     * 实现方式：Controller 接收目标用户 id；AdminPermissionService 校验用户存在后，
     * 读取当前管理员作为授权人，并在管理员权限表中写入授权记录。
     */
    @PostMapping("/users/{userId}/admin-permission")
    public ApiResponse<Void> grantAdmin(@PathVariable Long userId) {
        service.grantAdmin(userId);
        return ApiResponse.ok(null);
    }

    /**
     * 撤销用户管理员权限。
     * 实现方式：Controller 接收目标用户 id；AdminPermissionService 先阻止当前管理员撤销自己的权限，
     * 再按用户 id 删除管理员权限记录，删除后该用户不再能通过管理端权限校验。
     */
    @DeleteMapping("/users/{userId}/admin-permission")
    public ApiResponse<Void> revokeAdmin(@PathVariable Long userId) {
        service.revokeAdmin(userId);
        return ApiResponse.ok(null);
    }
}

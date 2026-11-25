package com.zzz.teachingevaluationsystembackend.service;

import com.zzz.teachingevaluationsystembackend.auth.AuthContext;
import com.zzz.teachingevaluationsystembackend.common.BizException;
import com.zzz.teachingevaluationsystembackend.entity.AdminPermission;
import com.zzz.teachingevaluationsystembackend.mapper.AdminPermissionMapper;
import com.zzz.teachingevaluationsystembackend.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminPermissionService {
    private final UserMapper userMapper;
    private final AdminPermissionMapper adminPermissionMapper;

    public void grantAdmin(Long userId) {
        if (userMapper.selectById(userId) == null) throw new BizException(404, "用户不存在");
        if (adminPermissionMapper.selectById(userId) != null) return;
        AdminPermission permission = new AdminPermission();
        permission.setUserId(userId);
        permission.setGrantedBy(AuthContext.currentAdminUserId());
        adminPermissionMapper.insert(permission);
    }

    public void revokeAdmin(Long userId) {
        if (AuthContext.currentAdminUserId().equals(userId)) {
            throw new BizException(400, "不能取消自己的管理员权限");
        }
        adminPermissionMapper.deleteById(userId);
    }
}

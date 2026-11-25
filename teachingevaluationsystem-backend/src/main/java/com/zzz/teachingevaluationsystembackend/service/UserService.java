package com.zzz.teachingevaluationsystembackend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zzz.teachingevaluationsystembackend.model.dto.PageResult;
import com.zzz.teachingevaluationsystembackend.model.vo.AdminUserVO;
import com.zzz.teachingevaluationsystembackend.entity.AdminPermission;
import com.zzz.teachingevaluationsystembackend.entity.User;
import com.zzz.teachingevaluationsystembackend.mapper.AdminPermissionMapper;
import com.zzz.teachingevaluationsystembackend.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserMapper userMapper;
    private final AdminPermissionMapper adminPermissionMapper;

    public PageResult<AdminUserVO> adminUsers(int page, int size, String role, String keyword, Integer isAdmin) {
        List<AdminPermission> permissions = adminPermissionMapper.selectList(new LambdaQueryWrapper<>());
        Map<Long, AdminPermission> permissionMap = new HashMap<>();
        for (AdminPermission permission : permissions) {
            permissionMap.put(permission.getUserId(), permission);
        }

        LambdaQueryWrapper<User> query = new LambdaQueryWrapper<User>()
                .in(User::getRole, List.of("student", "teacher"));
        if (StringUtils.hasText(role)) query.eq(User::getRole, role);
        if (StringUtils.hasText(keyword)) {
            query.and(q -> q.like(User::getUsername, keyword.trim()).or().like(User::getName, keyword.trim()));
        }
        if (isAdmin != null) {
            List<Long> adminUserIds = new ArrayList<>(permissionMap.keySet());
            if (isAdmin == 1) {
                if (adminUserIds.isEmpty()) return new PageResult<>(page, 0, 0, List.of());
                query.in(User::getId, adminUserIds);
            } else {
                if (!adminUserIds.isEmpty()) query.notIn(User::getId, adminUserIds);
            }
        }

        IPage<User> result = userMapper.selectPage(Page.of(page, size), query.orderByDesc(User::getId));
        List<AdminUserVO> records = result.getRecords().stream().map(user -> {
            AdminPermission permission = permissionMap.get(user.getId());
            return new AdminUserVO(
                    user.getId(),
                    user.getUsername(),
                    user.getName(),
                    user.getRole(),
                    permission != null,
                    permission == null ? null : permission.getGrantedBy(),
                    permission == null ? null : permission.getGrantedAt()
            );
        }).toList();
        return new PageResult<>(result.getCurrent(), result.getPages(), result.getTotal(), records);
    }
}

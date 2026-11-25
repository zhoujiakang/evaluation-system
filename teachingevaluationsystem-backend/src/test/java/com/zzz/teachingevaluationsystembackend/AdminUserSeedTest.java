package com.zzz.teachingevaluationsystembackend;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zzz.teachingevaluationsystembackend.entity.AdminPermission;
import com.zzz.teachingevaluationsystembackend.entity.User;
import com.zzz.teachingevaluationsystembackend.mapper.AdminPermissionMapper;
import com.zzz.teachingevaluationsystembackend.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.beans.factory.annotation.Autowired;

@SpringBootTest
class AdminUserSeedTest {
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private AdminPermissionMapper adminPermissionMapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Test
    void createAdminUser() {
        String username = "admin";
        String rawPassword = "admin123456";
        String name = "系统管理员";

        User existing = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username)
                .last("limit 1"));
        if (existing != null) {
            System.out.println("管理员已存在，userId=" + existing.getId() + ", username=" + username);
            return;
        }

        User user = new User();
        user.setUsername(username);
        user.setName(name);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setRole("teacher");
        user.setIsActive(1);
        userMapper.insert(user);

        AdminPermission permission = new AdminPermission();
        permission.setUserId(user.getId());
        permission.setGrantedBy(null);
        adminPermissionMapper.insert(permission);

        System.out.println("管理员创建成功，userId=" + user.getId() + ", username=" + username);
    }
}

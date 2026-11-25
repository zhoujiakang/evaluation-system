package com.zzz.teachingevaluationsystembackend.service;

import cn.dev33.satoken.stp.StpLogic;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zzz.teachingevaluationsystembackend.auth.StpKit;
import com.zzz.teachingevaluationsystembackend.common.BizException;
import com.zzz.teachingevaluationsystembackend.model.vo.LoginVO;
import com.zzz.teachingevaluationsystembackend.entity.AdminPermission;
import com.zzz.teachingevaluationsystembackend.entity.Student;
import com.zzz.teachingevaluationsystembackend.entity.User;
import com.zzz.teachingevaluationsystembackend.mapper.AdminPermissionMapper;
import com.zzz.teachingevaluationsystembackend.mapper.StudentMapper;
import com.zzz.teachingevaluationsystembackend.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserMapper userMapper;
    private final StudentMapper studentMapper;
    private final AdminPermissionMapper adminPermissionMapper;
    private final BCryptPasswordEncoder passwordEncoder;

    public LoginVO adminLogin(String username, String password) {
        User user = checkPassword(username, password);
        AdminPermission permission = adminPermissionMapper.selectById(user.getId());
        if (permission == null) {
            throw new BizException(403, "当前账号没有管理员权限");
        }
        return loginBy(StpKit.ADMIN, user, "admin");
    }

    public LoginVO studentLogin(String username, String password) {
        User user = checkPassword(username, password);
        if (!"student".equals(user.getRole())) {
            throw new BizException(403, "当前账号不是学生账号");
        }
        Student student = studentMapper.selectOne(new LambdaQueryWrapper<Student>()
                .eq(Student::getUserId, user.getId())
                .last("limit 1"));
        if (student == null) {
            throw new BizException(403, "当前账号未绑定学生信息");
        }
        return loginBy(StpKit.STUDENT, user, "student");
    }

    public void logoutAdmin() {
        StpKit.ADMIN.logout();
    }

    public void logoutStudent() {
        StpKit.STUDENT.logout();
    }

    private User checkPassword(String username, String password) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username)
                .last("limit 1"));
        if (user == null || user.getIsActive() == null || user.getIsActive() == 0) {
            throw new BizException(401, "账号不存在或已停用");
        }
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BizException(401, "账号或密码错误");
        }
        return user;
    }

    private LoginVO loginBy(StpLogic stpLogic, User user, String side) {
        stpLogic.login(user.getId());
        return new LoginVO(
                stpLogic.getTokenValue(),
                user.getId(),
                user.getUsername(),
                user.getName(),
                side
        );
    }
}

package com.zzz.teachingevaluationsystembackend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zzz.teachingevaluationsystembackend.common.BizException;
import com.zzz.teachingevaluationsystembackend.entity.Teacher;
import com.zzz.teachingevaluationsystembackend.entity.User;
import com.zzz.teachingevaluationsystembackend.mapper.TeacherMapper;
import com.zzz.teachingevaluationsystembackend.mapper.UserMapper;
import com.zzz.teachingevaluationsystembackend.model.dto.PageResult;
import com.zzz.teachingevaluationsystembackend.model.dto.request.ResetPasswordRequest;
import com.zzz.teachingevaluationsystembackend.model.dto.request.TeacherCreateRequest;
import com.zzz.teachingevaluationsystembackend.model.dto.request.TeacherUpdateRequest;
import com.zzz.teachingevaluationsystembackend.model.vo.TeacherVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TeacherService {
    private final UserMapper userMapper;
    private final TeacherMapper teacherMapper;
    private final BCryptPasswordEncoder passwordEncoder;

    public PageResult<TeacherVO> teachers(int page, int size, String teacherType, String name) {
        LambdaQueryWrapper<Teacher> query = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(teacherType)) query.eq(Teacher::getTeacherType, teacherType);
        if (StringUtils.hasText(name)) {
            List<Long> userIds = userMapper.selectList(new LambdaQueryWrapper<User>()
                            .like(User::getName, name.trim()))
                    .stream().map(User::getId).toList();
            if (userIds.isEmpty()) return new PageResult<>(page, 0, 0, List.of());
            query.in(Teacher::getUserId, userIds);
        }
        IPage<Teacher> result = teacherMapper.selectPage(Page.of(page, size), query.orderByDesc(Teacher::getId));
        Set<Long> userIds = result.getRecords().stream()
                .map(Teacher::getUserId)
                .filter(id -> id != null)
                .collect(HashSet::new, HashSet::add, HashSet::addAll);
        Map<Long, User> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            for (User it : userMapper.selectBatchIds(userIds)) userMap.put(it.getId(), it);
        }
        return new PageResult<>(result.getCurrent(), result.getPages(), result.getTotal(), result.getRecords().stream().map(it -> teacherMap(it, userMap)).toList());
    }

    @Transactional
    public Long createTeacher(TeacherCreateRequest request) {
        User user = createUser(request.getUsername(), request.getName(), request.getPassword(), "teacher");
        Teacher teacher = new Teacher();
        teacher.setUserId(user.getId());
        teacher.setTeacherType(request.getTeacherType());
        teacherMapper.insert(teacher);
        return teacher.getId();
    }

    @Transactional
    public void updateTeacher(Long id, TeacherUpdateRequest request) {
        Teacher teacher = teacherMapper.selectById(id);
        if (teacher == null) throw new BizException(404, "教师不存在");
        User user = userMapper.selectById(teacher.getUserId());
        if (user != null) {
            user.setUsername(request.getUsername());
            user.setName(request.getName());
            userMapper.updateById(user);
        }
        teacher.setTeacherType(request.getTeacherType());
        teacherMapper.updateById(teacher);
    }

    @Transactional
    public void deleteTeacher(Long id) {
        Teacher teacher = teacherMapper.selectById(id);
        teacherMapper.deleteById(id);
        if (teacher != null && teacher.getUserId() != null) userMapper.deleteById(teacher.getUserId());
    }

    public void resetTeacherPassword(Long id, ResetPasswordRequest request) {
        Teacher teacher = teacherMapper.selectById(id);
        if (teacher == null) throw new BizException(404, "教师不存在");
        resetPassword(teacher.getUserId(), request.getNewPassword());
    }

    private TeacherVO teacherMap(Teacher teacher, Map<Long, User> userMap) {
        User user = userMap.get(teacher.getUserId());
        return new TeacherVO(
                teacher.getId(),
                teacher.getUserId(),
                user == null ? null : user.getUsername(),
                user == null ? null : user.getName(),
                teacher.getTeacherType()
        );
    }

    private User createUser(String username, String name, String password, String role) {
        User user = new User();
        user.setUsername(username);
        user.setName(name);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(role);
        user.setIsActive(1);
        userMapper.insert(user);
        return user;
    }

    private void resetPassword(Long userId, String newPassword) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new BizException(404, "用户不存在");
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);
    }
}

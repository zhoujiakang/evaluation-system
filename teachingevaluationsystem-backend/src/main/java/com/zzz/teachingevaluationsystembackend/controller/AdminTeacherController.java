package com.zzz.teachingevaluationsystembackend.controller;

import com.zzz.teachingevaluationsystembackend.common.ApiResponse;
import com.zzz.teachingevaluationsystembackend.model.dto.PageResult;
import com.zzz.teachingevaluationsystembackend.model.dto.request.ResetPasswordRequest;
import com.zzz.teachingevaluationsystembackend.model.dto.request.TeacherCreateRequest;
import com.zzz.teachingevaluationsystembackend.model.dto.request.TeacherUpdateRequest;
import com.zzz.teachingevaluationsystembackend.model.vo.TeacherVO;
import com.zzz.teachingevaluationsystembackend.service.TeacherService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminTeacherController {
    private final TeacherService service;

    /**
     * 分页查询教师列表。
     * 实现方式：Controller 接收分页、教师类型和姓名筛选条件；TeacherService 查询教师表，
     * 批量关联用户表中的账号和姓名信息，封装为 TeacherVO 分页返回。
     */
    @GetMapping("/teachers")
    public ApiResponse<PageResult<TeacherVO>> teachers(@RequestParam(defaultValue = "1") int page,
                                                       @RequestParam(defaultValue = "10") int size,
                                                       @RequestParam(required = false) String teacherType,
                                                       @RequestParam(required = false) String name) {
        return ApiResponse.ok(service.teachers(page, size, teacherType, name));
    }

    /**
     * 新增教师。
     * 实现方式：Controller 接收教师账号、姓名、密码和教师类型；TeacherService 先创建教师登录用户，
     * 使用 BCrypt 加密密码，再写入教师表并返回教师 id。
     */
    @PostMapping("/teachers")
    public ApiResponse<Long> createTeacher(@RequestBody TeacherCreateRequest request) {
        return ApiResponse.ok(service.createTeacher(request));
    }

    /**
     * 修改教师。
     * 实现方式：Controller 传入教师 id 和修改内容；TeacherService 校验教师存在后，
     * 同步更新用户表的账号、姓名以及教师表中的教师类型。
     */
    @PutMapping("/teachers/{id}")
    public ApiResponse<Void> updateTeacher(@PathVariable Long id, @RequestBody TeacherUpdateRequest request) {
        service.updateTeacher(id, request);
        return ApiResponse.ok(null);
    }

    /**
     * 删除教师。
     * 实现方式：Controller 接收教师 id；TeacherService 查出教师关联的用户 id，
     * 删除教师记录并同步删除对应登录用户。
     */
    @DeleteMapping("/teachers/{id}")
    public ApiResponse<Void> deleteTeacher(@PathVariable Long id) {
        service.deleteTeacher(id);
        return ApiResponse.ok(null);
    }

    /**
     * 重置教师密码。
     * 实现方式：Controller 接收教师 id 和新密码；TeacherService 根据教师找到用户，
     * 对新密码进行 BCrypt 加密后更新用户密码字段。
     */
    @PutMapping("/teachers/{id}/password")
    public ApiResponse<Void> resetTeacherPassword(@PathVariable Long id, @RequestBody ResetPasswordRequest request) {
        service.resetTeacherPassword(id, request);
        return ApiResponse.ok(null);
    }
}

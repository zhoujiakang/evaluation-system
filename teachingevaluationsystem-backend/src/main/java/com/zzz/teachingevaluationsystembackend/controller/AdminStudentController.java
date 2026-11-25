package com.zzz.teachingevaluationsystembackend.controller;

import com.zzz.teachingevaluationsystembackend.common.ApiResponse;
import com.zzz.teachingevaluationsystembackend.model.dto.PageResult;
import com.zzz.teachingevaluationsystembackend.model.dto.request.ResetPasswordRequest;
import com.zzz.teachingevaluationsystembackend.model.dto.request.StudentCreateRequest;
import com.zzz.teachingevaluationsystembackend.model.dto.request.StudentUpdateRequest;
import com.zzz.teachingevaluationsystembackend.model.vo.StudentImportResultVO;
import com.zzz.teachingevaluationsystembackend.model.vo.StudentVO;
import com.zzz.teachingevaluationsystembackend.service.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminStudentController {
    private final StudentService service;

    /**
     * 分页查询学生列表。
     * 实现方式：Controller 接收分页、班级、入学年份、学号和姓名筛选条件；
     * StudentService 查询学生表并批量关联用户、班级、年级信息，组装 StudentVO 分页返回。
     */
    @GetMapping("/students")
    public ApiResponse<PageResult<StudentVO>> students(@RequestParam(defaultValue = "1") int page,
                                                       @RequestParam(defaultValue = "10") int size,
                                                       @RequestParam(required = false) String className,
                                                       @RequestParam(required = false) Integer enrollYear,
                                                       @RequestParam(required = false) String studentNo,
                                                       @RequestParam(required = false) String name) {
        return ApiResponse.ok(service.students(page, size, className, enrollYear, studentNo, name));
    }

    /**
     * 新增学生。
     * 实现方式：Controller 接收学生账号和学籍信息；StudentService 先创建登录用户并加密密码，
     * 再查找或创建年级、班级，最后写入学生表并返回学生 id。
     */
    @PostMapping("/students")
    public ApiResponse<Long> createStudent(@RequestBody StudentCreateRequest request) {
        return ApiResponse.ok(service.createStudent(request));
    }

    /**
     * 批量导入学生。
     * 实现方式：Controller 接收上传的 Excel 文件；StudentService 使用 ExcelSheetReader 读取表头和行数据，
     * 按学号新增或更新学生，同时自动创建缺失的用户、年级和班级，并返回导入统计。
     */
    @PostMapping("/students/import")
    public ApiResponse<StudentImportResultVO> importStudents(@RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(service.importStudents(file));
    }

    /**
     * 修改学生。
     * 实现方式：Controller 传入学生 id 和修改内容；StudentService 校验学生存在，
     * 同步更新用户姓名、学生学号，并重新匹配或创建对应班级、年级。
     */
    @PutMapping("/students/{id}")
    public ApiResponse<Void> updateStudent(@PathVariable Long id, @RequestBody StudentUpdateRequest request) {
        service.updateStudent(id, request);
        return ApiResponse.ok(null);
    }

    /**
     * 删除学生。
     * 实现方式：Controller 接收学生 id；StudentService 先查出学生关联的用户 id，
     * 再删除学生记录和对应登录用户，保证账号与学籍数据一起移除。
     */
    @DeleteMapping("/students/{id}")
    public ApiResponse<Void> deleteStudent(@PathVariable Long id) {
        service.deleteStudent(id);
        return ApiResponse.ok(null);
    }

    /**
     * 重置学生密码。
     * 实现方式：Controller 接收学生 id 和新密码；StudentService 查找学生对应用户，
     * 使用 BCryptPasswordEncoder 加密新密码后更新用户表。
     */
    @PutMapping("/students/{id}/password")
    public ApiResponse<Void> resetStudentPassword(@PathVariable Long id, @RequestBody ResetPasswordRequest request) {
        service.resetStudentPassword(id, request);
        return ApiResponse.ok(null);
    }
}

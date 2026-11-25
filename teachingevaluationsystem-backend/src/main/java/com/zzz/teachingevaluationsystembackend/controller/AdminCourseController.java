package com.zzz.teachingevaluationsystembackend.controller;

import com.zzz.teachingevaluationsystembackend.common.ApiResponse;
import com.zzz.teachingevaluationsystembackend.model.dto.PageResult;
import com.zzz.teachingevaluationsystembackend.model.dto.request.CourseUpsertRequest;
import com.zzz.teachingevaluationsystembackend.model.vo.CourseVO;
import com.zzz.teachingevaluationsystembackend.service.CourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminCourseController {
    private final CourseService service;

    /**
     * 分页查询课程列表。
     * 实现方式：Controller 接收分页、评教批次和年级筛选条件；CourseService 查询课程表，
     * 并批量查询课程-班级、课程-教师关系，组装课程基本信息、关联班级 id 和教师 id 后返回。
     */
    @GetMapping("/courses")
    public ApiResponse<PageResult<CourseVO>> courses(@RequestParam(defaultValue = "1") int page,
                                                     @RequestParam(defaultValue = "10") int size,
                                                     @RequestParam(required = false) Long evaluationId,
                                                     @RequestParam(required = false) String grade) {
        return ApiResponse.ok(service.courses(page, size, evaluationId, grade));
    }

    /**
     * 新增课程。
     * 实现方式：Controller 接收课程表单；CourseService 先确保年级存在，再写入课程所属评教、名称和专业，
     * 最后保存课程与班级、教师的关联关系。
     */
    @PostMapping("/courses")
    public ApiResponse<Long> createCourse(@RequestBody CourseUpsertRequest request) {
        return ApiResponse.ok(service.createCourse(request));
    }

    /**
     * 修改课程。
     * 实现方式：Controller 传入课程 id 和请求体；CourseService 校验课程存在后更新课程字段，
     * 并先清理旧的班级/教师关联，再按请求重新插入关联记录。
     */
    @PutMapping("/courses/{id}")
    public ApiResponse<Void> updateCourse(@PathVariable Long id, @RequestBody CourseUpsertRequest request) {
        service.updateCourse(id, request);
        return ApiResponse.ok(null);
    }

    /**
     * 删除课程。
     * 实现方式：Controller 接收课程 id；CourseService 删除课程本身，
     * 同时清理课程-班级和课程-教师两张关系表中的关联数据。
     */
    @DeleteMapping("/courses/{id}")
    public ApiResponse<Void> deleteCourse(@PathVariable Long id) {
        service.deleteCourse(id);
        return ApiResponse.ok(null);
    }
}

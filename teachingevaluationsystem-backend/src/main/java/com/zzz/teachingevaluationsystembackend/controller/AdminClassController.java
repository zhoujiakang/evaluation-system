package com.zzz.teachingevaluationsystembackend.controller;

import com.zzz.teachingevaluationsystembackend.common.ApiResponse;
import com.zzz.teachingevaluationsystembackend.model.dto.PageResult;
import com.zzz.teachingevaluationsystembackend.model.dto.request.ClassUpsertRequest;
import com.zzz.teachingevaluationsystembackend.model.vo.ClassVO;
import com.zzz.teachingevaluationsystembackend.service.ClassService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminClassController {
    private final ClassService service;

    /**
     * 分页查询班级列表。
     * 实现方式：Controller 接收页码、页大小以及班级名、专业、年级筛选条件，交给 ClassService；
     * Service 使用 MyBatis-Plus 按条件拼接查询班级表，并关联年级表补全年级名称，最后包装成 PageResult 返回。
     */
    @GetMapping("/classes")
    public ApiResponse<PageResult<ClassVO>> classes(@RequestParam(defaultValue = "1") int page,
                                                    @RequestParam(defaultValue = "10") int size,
                                                    @RequestParam(required = false) String className,
                                                    @RequestParam(required = false) String major,
                                                    @RequestParam(required = false) String grade) {
        return ApiResponse.ok(service.classes(page, size, className, major, grade));
    }

    /**
     * 新增班级。
     * 实现方式：Controller 接收班级表单后调用 ClassService；Service 会先按年级名称查找或创建年级记录，
     * 再写入班级名称、专业和年级 id，插入成功后返回新班级 id。
     */
    @PostMapping("/classes")
    public ApiResponse<Long> createClass(@RequestBody ClassUpsertRequest request) {
        return ApiResponse.ok(service.createClass(request));
    }

    /**
     * 修改班级。
     * 实现方式：Controller 将路径 id 和请求体传给 ClassService；Service 先确认班级存在，
     * 再保证目标年级存在，最后更新班级名称、专业和年级关联。
     */
    @PutMapping("/classes/{id}")
    public ApiResponse<Void> updateClass(@PathVariable Long id, @RequestBody ClassUpsertRequest request) {
        service.updateClass(id, request);
        return ApiResponse.ok(null);
    }

    /**
     * 删除班级。
     * 实现方式：Controller 只负责接收路径 id；ClassService 直接按主键删除班级记录，
     * 删除结果由统一 ApiResponse 返回给前端。
     */
    @DeleteMapping("/classes/{id}")
    public ApiResponse<Void> deleteClass(@PathVariable Long id) {
        service.deleteClass(id);
        return ApiResponse.ok(null);
    }
}

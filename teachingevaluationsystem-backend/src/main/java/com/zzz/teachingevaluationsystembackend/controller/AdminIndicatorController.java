package com.zzz.teachingevaluationsystembackend.controller;

import com.zzz.teachingevaluationsystembackend.common.ApiResponse;
import com.zzz.teachingevaluationsystembackend.model.dto.PageResult;
import com.zzz.teachingevaluationsystembackend.model.dto.request.IndicatorUpsertRequest;
import com.zzz.teachingevaluationsystembackend.model.vo.IndicatorVO;
import com.zzz.teachingevaluationsystembackend.service.IndicatorService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminIndicatorController {
    private final IndicatorService service;

    /**
     * 分页查询评教指标。
     * 实现方式：Controller 接收教师类型、层级和父级指标筛选条件；
     * IndicatorService 按条件查询指标表，按排序值升序和 id 倒序排列，再转换为 IndicatorVO 返回。
     */
    @GetMapping("/indicators")
    public ApiResponse<PageResult<IndicatorVO>> indicators(@RequestParam(defaultValue = "1") int page,
                                                           @RequestParam(defaultValue = "10") int size,
                                                           @RequestParam(required = false) String teacherType,
                                                           @RequestParam(required = false) Integer level,
                                                           @RequestParam(required = false) Long parentId) {
        return ApiResponse.ok(service.indicators(page, size, teacherType, level, parentId));
    }

    /**
     * 新增评教指标。
     * 实现方式：Controller 接收指标字段；IndicatorService 将教师类型、层级、父级、名称、权重等写入指标表，
     * 对排序值和启用状态提供默认值，插入后返回指标 id。
     */
    @PostMapping("/indicators")
    public ApiResponse<Long> createIndicator(@RequestBody IndicatorUpsertRequest request) {
        return ApiResponse.ok(service.createIndicator(request));
    }

    /**
     * 修改评教指标。
     * 实现方式：Controller 传入指标 id 和请求体；IndicatorService 先校验指标存在，
     * 再覆盖教师类型、层级、父级、名称、权重、排序和启用状态。
     */
    @PutMapping("/indicators/{id}")
    public ApiResponse<Void> updateIndicator(@PathVariable Long id, @RequestBody IndicatorUpsertRequest request) {
        service.updateIndicator(id, request);
        return ApiResponse.ok(null);
    }

    /**
     * 删除评教指标。
     * 实现方式：Controller 接收指标 id；IndicatorService 直接按主键删除指标记录，
     * 由数据库层完成对应删除操作。
     */
    @DeleteMapping("/indicators/{id}")
    public ApiResponse<Void> deleteIndicator(@PathVariable Long id) {
        service.deleteIndicator(id);
        return ApiResponse.ok(null);
    }
}

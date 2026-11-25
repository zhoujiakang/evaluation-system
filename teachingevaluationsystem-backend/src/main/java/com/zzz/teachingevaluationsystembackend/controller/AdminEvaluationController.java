package com.zzz.teachingevaluationsystembackend.controller;

import com.zzz.teachingevaluationsystembackend.common.ApiResponse;
import com.zzz.teachingevaluationsystembackend.model.dto.PageResult;
import com.zzz.teachingevaluationsystembackend.model.dto.request.EvaluationUpsertRequest;
import com.zzz.teachingevaluationsystembackend.model.vo.EvaluationCourseImportResultVO;
import com.zzz.teachingevaluationsystembackend.model.vo.EvaluationCreateImportResultVO;
import com.zzz.teachingevaluationsystembackend.model.vo.EvaluationVO;
import com.zzz.teachingevaluationsystembackend.service.EvaluationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminEvaluationController {
    private final EvaluationService service;

    /**
     * 分页查询评教批次。
     * 实现方式：Controller 接收分页和状态筛选；EvaluationService 按状态查询评教表，
     * 将数据库实体转换为 EvaluationVO 后放入 PageResult 返回。
     */
    @GetMapping("/evaluations")
    public ApiResponse<PageResult<EvaluationVO>> evaluations(@RequestParam(defaultValue = "1") int page,
                                                             @RequestParam(defaultValue = "10") int size,
                                                             @RequestParam(required = false) String status) {
        return ApiResponse.ok(service.evaluations(page, size, status));
    }

    /**
     * 新建评教批次。
     * 实现方式：Controller 接收评教基础信息；EvaluationService 写入名称、学年、学期和状态，
     * 如果前端未传状态则使用草稿状态，插入后返回评教 id。
     */
    @PostMapping("/evaluations")
    public ApiResponse<Long> createEvaluation(@RequestBody EvaluationUpsertRequest request) {
        return ApiResponse.ok(service.createEvaluation(request));
    }

    /**
     * 新建评教批次并导入课程任务。
     * 实现方式：Controller 从 multipart 表单中拆出评教字段和 Excel 文件，先构造 EvaluationUpsertRequest；
     * EvaluationService 创建评教批次后解析 Excel，自动创建/匹配年级、课程、教师和班级，并生成课程与教师、班级的关系。
     */
    @PostMapping(value = "/evaluations/create-with-courses", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<EvaluationCreateImportResultVO> createEvaluationWithCourses(
            @RequestParam String name,
            @RequestParam String academicYear,
            @RequestParam String term,
            @RequestParam(required = false) String status,
            @RequestParam("file") MultipartFile file
    ) {
        EvaluationUpsertRequest request = new EvaluationUpsertRequest();
        request.setName(name);
        request.setAcademicYear(academicYear);
        request.setTerm(term);
        request.setStatus(status);
        return ApiResponse.ok(service.createEvaluationWithCourses(request, file));
    }

    /**
     * 修改评教批次。
     * 实现方式：Controller 传入评教 id 和请求体；EvaluationService 先校验评教存在，
     * 再覆盖名称、学年、学期和状态等基础字段。
     */
    @PutMapping("/evaluations/{id}")
    public ApiResponse<Void> updateEvaluation(@PathVariable Long id, @RequestBody EvaluationUpsertRequest request) {
        service.updateEvaluation(id, request);
        return ApiResponse.ok(null);
    }

    /**
     * 删除评教批次。
     * 实现方式：Controller 接收评教 id；EvaluationService 删除评教记录，
     * 并清理该评教下的评分、提交记录和评教任务等关联数据。
     */
    @DeleteMapping("/evaluations/{id}")
    public ApiResponse<Void> deleteEvaluation(@PathVariable Long id) {
        service.deleteEvaluation(id);
        return ApiResponse.ok(null);
    }

    /**
     * 发布评教批次。
     * 实现方式：Controller 调用 EvaluationService；Service 校验评教存在后，
     * 根据该批次下课程关联的班级学生和授课教师生成待评任务，并把评教状态改为已发布。
     */
    @PostMapping("/evaluations/{id}/publish")
    public ApiResponse<Void> publishEvaluation(@PathVariable Long id) {
        service.publishEvaluation(id);
        return ApiResponse.ok(null);
    }

    /**
     * 向已有评教批次导入课程。
     * 实现方式：Controller 接收评教 id 和 Excel 文件；EvaluationService 先确认评教存在，
     * 然后逐行读取课程、专业、年级、教师等信息，创建缺失数据并维护课程、教师、班级关系。
     */
    @PostMapping("/evaluations/{id}/courses/import")
    public ApiResponse<EvaluationCourseImportResultVO> importEvaluationCourses(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(service.importEvaluationCourses(id, file));
    }

    /**
     * 关闭评教批次。
     * 实现方式：Controller 接收评教 id；EvaluationService 校验评教存在后，
     * 将状态更新为已关闭，使学生端不再继续提交该批次任务。
     */
    @PostMapping("/evaluations/{id}/close")
    public ApiResponse<Void> closeEvaluation(@PathVariable Long id) {
        service.closeEvaluation(id);
        return ApiResponse.ok(null);
    }
}

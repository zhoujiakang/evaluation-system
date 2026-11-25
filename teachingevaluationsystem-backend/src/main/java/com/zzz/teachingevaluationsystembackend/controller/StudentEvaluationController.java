package com.zzz.teachingevaluationsystembackend.controller;

import com.zzz.teachingevaluationsystembackend.common.ApiResponse;
import com.zzz.teachingevaluationsystembackend.model.dto.request.StudentSubmitRequest;
import com.zzz.teachingevaluationsystembackend.model.vo.StudentCourseTaskVO;
import com.zzz.teachingevaluationsystembackend.model.vo.StudentEvaluationOptionVO;
import com.zzz.teachingevaluationsystembackend.model.vo.StudentEvaluationProgressVO;
import com.zzz.teachingevaluationsystembackend.model.vo.StudentTaskDetailVO;
import com.zzz.teachingevaluationsystembackend.service.StudentEvaluationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/student/evaluations")
public class StudentEvaluationController {
    private final StudentEvaluationService service;

    /**
     * 获取学生可参与的评教批次选项。
     * 实现方式：Controller 调用 StudentEvaluationService；Service 根据当前登录学生找到其班级和课程任务，
     * 只返回已发布且与该学生有关联任务的评教批次。
     */
    @GetMapping("/options")
    public ApiResponse<List<StudentEvaluationOptionVO>> evaluationOptions() {
        return ApiResponse.ok(service.evaluationOptions());
    }

    /**
     * 查询学生在指定评教批次中的完成进度。
     * 实现方式：Controller 接收评教 id；StudentEvaluationService 校验评教为已发布，
     * 再统计当前学生该批次任务总数和已提交数量，计算完成率后返回。
     */
    @GetMapping("/{evaluationId}/progress")
    public ApiResponse<StudentEvaluationProgressVO> evaluationProgress(@PathVariable Long evaluationId) {
        return ApiResponse.ok(service.evaluationProgress(evaluationId));
    }

    /**
     * 查询学生课程维度的待评任务。
     * 实现方式：Controller 接收可选评教 id；StudentEvaluationService 先定位当前学生，
     * 再按评教和学生查询任务，批量加载课程、教师和评教信息后按课程分组返回。
     */
    @GetMapping("/course-tasks")
    public ApiResponse<List<StudentCourseTaskVO>> courseTasks(@RequestParam(required = false) Long evaluationId) {
        return ApiResponse.ok(service.courseTasks(evaluationId));
    }

    /**
     * 开始某个评教任务并获取评分详情。
     * 实现方式：Controller 接收任务 id；StudentEvaluationService 校验任务属于当前学生，
     * 然后加载课程、教师和匹配该教师类型的启用指标列表，组装成任务详情返回。
     */
    @PostMapping("/tasks/{taskId}/start")
    public ApiResponse<StudentTaskDetailVO> start(@PathVariable Long taskId) {
        return ApiResponse.ok(service.start(taskId));
    }

    /**
     * 提交某个评教任务。
     * 实现方式：Controller 接收任务 id 和各指标评分；StudentEvaluationService 校验任务归属和评教状态，
     * 若任务已经提交则拒绝重复提交；校验通过后写入提交记录和评分明细，并把任务状态更新为已提交。
     */
    @PostMapping("/tasks/{taskId}/submit")
    public ApiResponse<Void> submit(@PathVariable Long taskId, @RequestBody StudentSubmitRequest request) {
        service.submit(taskId, request);
        return ApiResponse.ok(null);
    }
}

package com.zzz.teachingevaluationsystembackend.controller;

import com.zzz.teachingevaluationsystembackend.common.ApiResponse;
import com.zzz.teachingevaluationsystembackend.model.dto.PageResult;
import com.zzz.teachingevaluationsystembackend.model.vo.AdminScoreVO;
import com.zzz.teachingevaluationsystembackend.model.vo.AdminUserVO;
import com.zzz.teachingevaluationsystembackend.model.vo.StudentEvaluationStatusVO;
import com.zzz.teachingevaluationsystembackend.model.vo.TeacherScoreStatVO;
import com.zzz.teachingevaluationsystembackend.service.ScoreService;
import com.zzz.teachingevaluationsystembackend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminAnalyticsController {
    private final ScoreService service;
    private final UserService userService;

    /**
     * 分页查询后台用户和管理员授权状态。
     * 实现方式：Controller 接收角色、关键字和是否管理员筛选条件；
     * UserService 查询用户表并关联管理员授权表，计算 isAdmin、授权人和授权时间后返回分页数据。
     */
    @GetMapping("/admin-users")
    public ApiResponse<PageResult<AdminUserVO>> adminUsers(@RequestParam(defaultValue = "1") int page,
                                                           @RequestParam(defaultValue = "10") int size,
                                                           @RequestParam(required = false) String role,
                                                           @RequestParam(required = false) String keyword,
                                                           @RequestParam(required = false) Integer isAdmin) {
        return ApiResponse.ok(userService.adminUsers(page, size, role, keyword, isAdmin));
    }

    /**
     * 分页查询学生评教成绩明细。
     * 实现方式：Controller 接收评教批次、学号、学生姓名和任务状态筛选条件；
     * ScoreService 查询评教任务并批量关联学生、课程、教师、提交记录和分数，组装每条任务的得分明细。
     */
    @GetMapping("/scores")
    public ApiResponse<PageResult<AdminScoreVO>> scores(@RequestParam(defaultValue = "1") int page,
                                                        @RequestParam(defaultValue = "10") int size,
                                                        @RequestParam(required = false) Long evaluationId,
                                                        @RequestParam(required = false) String studentNo,
                                                        @RequestParam(required = false) String studentName,
                                                        @RequestParam(required = false) String taskStatus) {
        return ApiResponse.ok(service.scores(page, size, evaluationId, studentNo, studentName, taskStatus));
    }

    /**
     * 分页查询某个评教批次下的教师得分统计。
     * 实现方式：Controller 从路径取得评教 id，并接收教师类型、教师姓名筛选；
     * ScoreService 汇总该批次提交记录和评分表，按教师计算提交次数、总分和平均分后分页返回。
     */
    @GetMapping("/evaluations/{id}/teacher-score-stats")
    public ApiResponse<PageResult<TeacherScoreStatVO>> teacherScoreStats(@PathVariable Long id,
                                                                         @RequestParam(defaultValue = "1") int page,
                                                                         @RequestParam(defaultValue = "10") int size,
                                                                         @RequestParam(required = false) String teacherType,
                                                                         @RequestParam(required = false) String teacherName) {
        return ApiResponse.ok(service.teacherScoreStats(page, size, id, teacherType, teacherName));
    }

    /**
     * 分页查询某个评教批次下学生完成情况。
     * 实现方式：Controller 接收评教 id、学号、姓名和班级筛选；
     * ScoreService 按学生聚合同批次任务数量和已提交数量，计算未完成数量与完成率后分页返回。
     */
    @GetMapping("/evaluations/{id}/student-evaluation-status")
    public ApiResponse<PageResult<StudentEvaluationStatusVO>> studentEvaluationStatus(@PathVariable Long id,
                                                                                      @RequestParam(defaultValue = "1") int page,
                                                                                      @RequestParam(defaultValue = "10") int size,
                                                                                      @RequestParam(required = false) String studentNo,
                                                                                      @RequestParam(required = false) String studentName,
                                                                                      @RequestParam(required = false) String className) {
        return ApiResponse.ok(service.studentEvaluationStatus(page, size, id, studentNo, studentName, className));
    }

    /**
     * 导出未完成评教的学生名单。
     * 实现方式：Controller 调用 ScoreService 生成 Excel 字节数组；
     * Service 会筛出 pendingTasks 大于 0 的学生并写入工作簿，Controller 再设置 xlsx 下载响应头返回文件流。
     */
    @GetMapping("/evaluations/{id}/student-evaluation-status/export-incomplete")
    public ResponseEntity<byte[]> exportIncompleteStudents(@PathVariable Long id) {
        byte[] bytes = service.exportIncompleteStudents(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=incomplete_students_" + id + ".xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }

    /**
     * 导出教师得分统计表。
     * 实现方式：Controller 调用 ScoreService 按评教 id 生成教师统计 Excel；
     * Service 将教师姓名、类型、提交次数、总分和均分写入工作簿，Controller 设置下载文件名和 MIME 类型后返回。
     */
    @GetMapping("/evaluations/{id}/teacher-score-stats/export")
    public ResponseEntity<byte[]> exportTeacherScoreStats(@PathVariable Long id) {
        byte[] bytes = service.exportTeacherScoreStats(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=teacher_score_stats_" + id + ".xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }
}

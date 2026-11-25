package com.zzz.teachingevaluationsystembackend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.MybatisBatchUtils;
import com.zzz.teachingevaluationsystembackend.auth.AuthContext;
import com.zzz.teachingevaluationsystembackend.common.BizException;
import com.zzz.teachingevaluationsystembackend.constant.EvaluationConstant;
import com.zzz.teachingevaluationsystembackend.model.dto.request.StudentSubmitRequest;
import com.zzz.teachingevaluationsystembackend.model.vo.StudentCourseTaskVO;
import com.zzz.teachingevaluationsystembackend.model.vo.StudentCourseTeacherTaskVO;
import com.zzz.teachingevaluationsystembackend.model.vo.IndicatorVO;
import com.zzz.teachingevaluationsystembackend.model.vo.StudentEvaluationOptionVO;
import com.zzz.teachingevaluationsystembackend.model.vo.StudentEvaluationProgressVO;
import com.zzz.teachingevaluationsystembackend.model.vo.StudentTaskDetailVO;
import com.zzz.teachingevaluationsystembackend.model.vo.StudentTaskVO;
import com.zzz.teachingevaluationsystembackend.entity.*;
import com.zzz.teachingevaluationsystembackend.mapper.*;
import lombok.RequiredArgsConstructor;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class StudentEvaluationService {
    private static final long INDICATOR_CACHE_TTL_MILLIS = 60_000L;
    private final Map<String, CachedIndicators> indicatorCache = new ConcurrentHashMap<>();

    private final UserMapper userMapper;
    private final StudentMapper studentMapper;
    private final TeacherMapper teacherMapper;
    private final CourseMapper courseMapper;
    private final EvaluationMapper evaluationMapper;
    private final IndicatorMapper indicatorMapper;
    private final EvalTaskMapper evalTaskMapper;
    private final ScoreSubmissionMapper scoreSubmissionMapper;
    private final SqlSessionFactory sqlSessionFactory;

    public List<StudentEvaluationOptionVO> evaluationOptions() {
        Student student = currentStudent();
        List<EvalTask> tasks = evalTaskMapper.selectList(new LambdaQueryWrapper<EvalTask>()
                .select(EvalTask::getEvaluationId)
                .eq(EvalTask::getStudentId, student.getId()));
        if (tasks.isEmpty()) return List.of();

        List<Long> evaluationIds = tasks.stream()
                .map(EvalTask::getEvaluationId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (evaluationIds.isEmpty()) return List.of();

        Map<Long, Evaluation> evaluationMap = new HashMap<>();
        for (Evaluation evaluation : evaluationMapper.selectBatchIds(evaluationIds)) {
            if (EvaluationConstant.STATUS_PUBLISHED.equals(evaluation.getStatus())) {
                evaluationMap.put(evaluation.getId(), evaluation);
            }
        }
        if (evaluationMap.isEmpty()) return List.of();

        List<StudentEvaluationOptionVO> result = new ArrayList<>();
        for (Long evaluationId : evaluationIds) {
            Evaluation evaluation = evaluationMap.get(evaluationId);
            if (evaluation == null) continue;
            result.add(new StudentEvaluationOptionVO(
                    evaluationId,
                    evaluation.getName()
            ));
        }
        return result;
    }

    public StudentEvaluationProgressVO evaluationProgress(Long evaluationId) {
        Student student = currentStudent();
        Evaluation evaluation = validatePublishedEvaluation(evaluationId);
        List<EvalTask> tasks = evalTaskMapper.selectList(new LambdaQueryWrapper<EvalTask>()
                .select(EvalTask::getStatus)
                .eq(EvalTask::getStudentId, student.getId())
                .eq(EvalTask::getEvaluationId, evaluation.getId()));
        int total = tasks.size();
        int completed = (int) tasks.stream().filter(task -> "completed".equals(task.getStatus())).count();
        return new StudentEvaluationProgressVO(
                evaluation.getId(),
                evaluation.getName(),
                total,
                completed
        );
    }

    public List<StudentCourseTaskVO> courseTasks(Long evaluationId) {
        Student student = currentStudent();
        Evaluation evaluation = validatePublishedEvaluation(evaluationId);
        List<EvalTask> tasks = evalTaskMapper.selectList(new LambdaQueryWrapper<EvalTask>()
                .eq(EvalTask::getStudentId, student.getId())
                .eq(EvalTask::getEvaluationId, evaluation.getId())
                .orderByDesc(EvalTask::getId));
        if (tasks.isEmpty()) return List.of();

        Map<Long, Course> courseMap = new HashMap<>();
        List<Long> courseIds = tasks.stream().map(EvalTask::getCourseId).filter(Objects::nonNull).distinct().toList();
        if (!courseIds.isEmpty()) {
            for (Course course : courseMapper.selectBatchIds(courseIds)) {
                courseMap.put(course.getId(), course);
            }
        }

        Map<Long, Teacher> teacherMap = new HashMap<>();
        List<Long> teacherIds = tasks.stream().map(EvalTask::getTeacherId).filter(Objects::nonNull).distinct().toList();
        if (!teacherIds.isEmpty()) {
            for (Teacher teacher : teacherMapper.selectBatchIds(teacherIds)) {
                teacherMap.put(teacher.getId(), teacher);
            }
        }

        Map<Long, User> userMap = new HashMap<>();
        List<Long> userIds = teacherMap.values().stream().map(Teacher::getUserId).filter(Objects::nonNull).distinct().toList();
        if (!userIds.isEmpty()) {
            for (User user : userMapper.selectBatchIds(userIds)) {
                userMap.put(user.getId(), user);
            }
        }

        Map<Long, StudentCourseTaskVO> grouped = new LinkedHashMap<>();
        for (EvalTask task : tasks) {
            if (task.getCourseId() == null) continue;
            Course course = courseMap.get(task.getCourseId());
            Teacher teacher = teacherMap.get(task.getTeacherId());
            User teacherUser = teacher == null ? null : userMap.get(teacher.getUserId());

            StudentCourseTaskVO courseNode = grouped.computeIfAbsent(task.getCourseId(),
                    id -> new StudentCourseTaskVO(
                            id,
                            course == null ? null : course.getCourseNameCn(),
                            course == null ? null : course.getCourseNameEn(),
                            new ArrayList<>()
                    ));
            courseNode.getTeachers().add(new StudentCourseTeacherTaskVO(
                    task.getId(),
                    task.getTeacherId(),
                    teacherUser == null ? null : teacherUser.getName(),
                    teacher == null ? null : teacher.getTeacherType(),
                    task.getStatus(),
                    task.getCompletedAt()
            ));
        }
        return new ArrayList<>(grouped.values());
    }

    public StudentTaskDetailVO start(Long taskId) {
        EvalTask task = ownTask(taskId);
        Teacher teacher = teacherMapper.selectById(task.getTeacherId());
        if (teacher == null) throw new BizException(404, "教师不存在");
        List<IndicatorVO> indicators = findIndicatorsByTeacherType(teacher.getTeacherType());
        return new StudentTaskDetailVO(taskMap(task), indicators);
    }

    @Transactional
    public void submit(Long taskId, StudentSubmitRequest request) {
        Long userId = AuthContext.currentStudentUserId();
        EvalTask task = ownTask(taskId);
        Evaluation evaluation = evaluationMapper.selectById(task.getEvaluationId());
        if (evaluation == null || !"published".equals(evaluation.getStatus())) {
            throw new BizException(400, "测评未发布或已关闭");
        }
        boolean submitted = scoreSubmissionMapper.exists(new LambdaQueryWrapper<ScoreSubmission>()
                .eq(ScoreSubmission::getTaskId, taskId));
        if (submitted) throw new BizException(400, "该任务已提交");

        ScoreSubmission submission = new ScoreSubmission();
        submission.setEvaluationId(task.getEvaluationId());
        submission.setTaskId(taskId);
        submission.setSubmitterUserId(userId);
        submission.setSubmitMode("student");
        submission.setRemark(request.getRemark());
        scoreSubmissionMapper.insert(submission);

        if (request.getScores() == null || request.getScores().isEmpty()) {
            throw new BizException(400, "评分不能为空");
        }
        List<Score> scoreRows = request.getScores().stream().map(item -> {
            Score score = new Score();
            score.setSubmissionId(submission.getId());
            score.setEvaluationId(task.getEvaluationId());
            score.setTaskId(taskId);
            score.setIndicatorId(item.getIndicatorId());
            score.setScore(item.getScore());
            return score;
        }).toList();
        MybatisBatchUtils.execute(sqlSessionFactory, scoreRows, ScoreMapper.class.getName() + ".insert", 500);

        task.setStatus("completed");
        task.setCompletedAt(LocalDateTime.now());
        evalTaskMapper.updateById(task);
    }

    private Student currentStudent() {
        Long userId = AuthContext.currentStudentUserId();
        Student student = studentMapper.selectOne(new LambdaQueryWrapper<Student>()
                .eq(Student::getUserId, userId)
                .last("limit 1"));
        if (student == null) throw new BizException(403, "当前账号未绑定学生信息");
        return student;
    }

    private EvalTask ownTask(Long taskId) {
        Student student = currentStudent();
        EvalTask task = evalTaskMapper.selectById(taskId);
        if (task == null || !student.getId().equals(task.getStudentId())) {
            throw new BizException(404, "任务不存在");
        }
        return task;
    }

    private Evaluation validatePublishedEvaluation(Long evaluationId) {
        if (evaluationId == null) throw new BizException(400, "请选择测评");
        Evaluation evaluation = evaluationMapper.selectById(evaluationId);
        if (evaluation == null) throw new BizException(404, "测评不存在");
        if (!EvaluationConstant.STATUS_PUBLISHED.equals(evaluation.getStatus())) throw new BizException(400, "测评未发布");
        return evaluation;
    }

    private StudentTaskVO taskMap(EvalTask task) {
        Evaluation evaluation = evaluationMapper.selectById(task.getEvaluationId());
        Course course = courseMapper.selectById(task.getCourseId());
        Teacher teacher = teacherMapper.selectById(task.getTeacherId());
        User teacherUser = teacher == null ? null : userMapper.selectById(teacher.getUserId());
        return taskMapBuild(task, evaluation, course, teacher, teacherUser);
    }

    private StudentTaskVO taskMapBuild(EvalTask task, Evaluation evaluation, Course course, Teacher teacher, User teacherUser) {
        return new StudentTaskVO(
                task.getId(),
                task.getEvaluationId(),
                evaluation == null ? null : evaluation.getName(),
                task.getCourseId(),
                course == null ? null : course.getCourseNameCn(),
                course == null ? null : course.getCourseNameEn(),
                task.getTeacherId(),
                teacherUser == null ? null : teacherUser.getName(),
                teacher == null ? null : teacher.getTeacherType(),
                task.getStatus(),
                task.getCompletedAt()
        );
    }

    private IndicatorVO indicatorVO(Indicator indicator) {
        return new IndicatorVO(
                indicator.getId(),
                indicator.getTeacherType(),
                indicator.getLevel(),
                indicator.getParentId(),
                indicator.getName(),
                indicator.getNameEn(),
                indicator.getWeight(),
                indicator.getSortOrder(),
                indicator.getIsActive(),
                indicator.getLockedAt()
        );
    }

    private List<IndicatorVO> findIndicatorsByTeacherType(String teacherType) {
        String key = teacherType == null ? "" : teacherType.trim();
        long now = System.currentTimeMillis();
        CachedIndicators cached = indicatorCache.get(key);
        if (cached != null && now - cached.cachedAt <= INDICATOR_CACHE_TTL_MILLIS) {
            return cached.indicators;
        }
        List<IndicatorVO> indicators = indicatorMapper.selectList(new LambdaQueryWrapper<Indicator>()
                        .eq(Indicator::getTeacherType, key)
                        .eq(Indicator::getIsActive, 1)
                        .orderByAsc(Indicator::getLevel)
                        .orderByAsc(Indicator::getSortOrder)
                        .orderByAsc(Indicator::getId))
                .stream()
                .map(this::indicatorVO)
                .toList();
        indicatorCache.put(key, new CachedIndicators(indicators, now));
        return indicators;
    }

    private record CachedIndicators(List<IndicatorVO> indicators, long cachedAt) {
    }
}

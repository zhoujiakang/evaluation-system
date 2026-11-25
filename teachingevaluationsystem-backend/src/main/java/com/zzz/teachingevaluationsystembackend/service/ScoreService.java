package com.zzz.teachingevaluationsystembackend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zzz.teachingevaluationsystembackend.common.BizException;
import com.zzz.teachingevaluationsystembackend.model.dto.PageResult;
import com.zzz.teachingevaluationsystembackend.model.vo.AdminScoreVO;
import com.zzz.teachingevaluationsystembackend.model.vo.StudentEvaluationStatusVO;
import com.zzz.teachingevaluationsystembackend.model.vo.TeacherScoreStatVO;
import com.zzz.teachingevaluationsystembackend.entity.*;
import com.zzz.teachingevaluationsystembackend.mapper.*;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ScoreService {
    private static final int AUTO_SIZE_MAX_ROWS = 2000;
    private final UserMapper userMapper;
    private final StudentMapper studentMapper;
    private final TeacherMapper teacherMapper;
    private final GradeMapper gradeMapper;
    private final ClassMapper classMapper;
    private final CourseMapper courseMapper;
    private final EvaluationMapper evaluationMapper;
    private final IndicatorMapper indicatorMapper;
    private final EvalTaskMapper evalTaskMapper;
    private final ScoreSubmissionMapper scoreSubmissionMapper;
    private final ScoreMapper scoreMapper;

    public PageResult<AdminScoreVO> scores(int page, int size, Long evaluationId, String studentNo, String studentName, String taskStatus) {
        LambdaQueryWrapper<Student> studentQuery = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(studentNo)) studentQuery.like(Student::getStudentNo, studentNo.trim());
        if (StringUtils.hasText(studentName)) {
            List<Long> matchedUserIds = userMapper.selectList(new LambdaQueryWrapper<User>()
                            .like(User::getName, studentName.trim()))
                    .stream().map(User::getId).toList();
            if (matchedUserIds.isEmpty()) return new PageResult<>(page, 0, 0, List.of());
            studentQuery.in(Student::getUserId, matchedUserIds);
        }
        List<Long> studentIds = null;
        if (StringUtils.hasText(studentNo) || StringUtils.hasText(studentName)) {
            studentIds = studentMapper.selectList(studentQuery).stream().map(Student::getId).toList();
            if (studentIds.isEmpty()) return new PageResult<>(page, 0, 0, List.of());
        }

        LambdaQueryWrapper<EvalTask> taskQuery = new LambdaQueryWrapper<>();
        if (evaluationId != null) taskQuery.eq(EvalTask::getEvaluationId, evaluationId);
        if (studentIds != null) taskQuery.in(EvalTask::getStudentId, studentIds);
        if (StringUtils.hasText(taskStatus)) taskQuery.eq(EvalTask::getStatus, taskStatus);
        IPage<EvalTask> taskPage = evalTaskMapper.selectPage(Page.of(page, size), taskQuery.orderByDesc(EvalTask::getId));
        List<EvalTask> tasks = taskPage.getRecords();
        if (tasks.isEmpty()) {
            return new PageResult<>(taskPage.getCurrent(), taskPage.getPages(), taskPage.getTotal(), List.of());
        }

        Set<Long> evaluationIds = tasks.stream().map(EvalTask::getEvaluationId).filter(Objects::nonNull).collect(HashSet::new, HashSet::add, HashSet::addAll);
        Set<Long> taskStudentIds = tasks.stream().map(EvalTask::getStudentId).filter(Objects::nonNull).collect(HashSet::new, HashSet::add, HashSet::addAll);
        Set<Long> courseIds = tasks.stream().map(EvalTask::getCourseId).filter(Objects::nonNull).collect(HashSet::new, HashSet::add, HashSet::addAll);
        Set<Long> teacherIds = tasks.stream().map(EvalTask::getTeacherId).filter(Objects::nonNull).collect(HashSet::new, HashSet::add, HashSet::addAll);
        Set<Long> taskIds = tasks.stream().map(EvalTask::getId).filter(Objects::nonNull).collect(HashSet::new, HashSet::add, HashSet::addAll);

        Map<Long, Evaluation> evaluationMap = new HashMap<>();
        if (!evaluationIds.isEmpty()) {
            for (Evaluation it : evaluationMapper.selectBatchIds(evaluationIds)) evaluationMap.put(it.getId(), it);
        }
        Map<Long, Student> studentMap = new HashMap<>();
        if (!taskStudentIds.isEmpty()) {
            for (Student it : studentMapper.selectBatchIds(taskStudentIds)) studentMap.put(it.getId(), it);
        }
        Map<Long, Course> courseMap = new HashMap<>();
        if (!courseIds.isEmpty()) {
            for (Course it : courseMapper.selectBatchIds(courseIds)) courseMap.put(it.getId(), it);
        }
        Map<Long, Teacher> teacherMap = new HashMap<>();
        if (!teacherIds.isEmpty()) {
            for (Teacher it : teacherMapper.selectBatchIds(teacherIds)) teacherMap.put(it.getId(), it);
        }

        Set<Long> userIds = new HashSet<>();
        for (Student student : studentMap.values()) if (student != null && student.getUserId() != null) userIds.add(student.getUserId());
        for (Teacher teacher : teacherMap.values()) if (teacher != null && teacher.getUserId() != null) userIds.add(teacher.getUserId());
        Map<Long, User> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            for (User it : userMapper.selectBatchIds(userIds)) userMap.put(it.getId(), it);
        }

        Map<Long, ScoreSubmission> submissionByTaskId = new HashMap<>();
        if (!taskIds.isEmpty()) {
            List<ScoreSubmission> submissions = scoreSubmissionMapper.selectList(new LambdaQueryWrapper<ScoreSubmission>()
                    .in(ScoreSubmission::getTaskId, taskIds)
                    .orderByDesc(ScoreSubmission::getId));
            for (ScoreSubmission submission : submissions) {
                submissionByTaskId.putIfAbsent(submission.getTaskId(), submission);
            }
        }

        Set<Long> submissionIds = submissionByTaskId.values().stream()
                .map(ScoreSubmission::getId)
                .filter(Objects::nonNull)
                .collect(HashSet::new, HashSet::add, HashSet::addAll);
        Map<Long, BigDecimal> totalScoreBySubmissionId = new HashMap<>();
        if (!submissionIds.isEmpty()) {
            List<Score> scores = scoreMapper.selectList(new LambdaQueryWrapper<Score>().in(Score::getSubmissionId, submissionIds));
            for (Score score : scores) {
                if (score.getSubmissionId() == null || score.getScore() == null) continue;
                totalScoreBySubmissionId.put(
                        score.getSubmissionId(),
                        totalScoreBySubmissionId.getOrDefault(score.getSubmissionId(), BigDecimal.ZERO).add(score.getScore())
                );
            }
        }

        List<AdminScoreVO> records = tasks.stream().map(task -> {
            Evaluation evaluation = evaluationMap.get(task.getEvaluationId());
            Student student = studentMap.get(task.getStudentId());
            Course course = courseMap.get(task.getCourseId());
            Teacher teacher = teacherMap.get(task.getTeacherId());
            User studentUser = student == null ? null : userMap.get(student.getUserId());
            User teacherUser = teacher == null ? null : userMap.get(teacher.getUserId());
            ScoreSubmission submission = submissionByTaskId.get(task.getId());
            BigDecimal totalScore = submission == null ? null : totalScoreBySubmissionId.getOrDefault(submission.getId(), BigDecimal.ZERO);
            return new AdminScoreVO(
                    task.getId(),
                    task.getEvaluationId(),
                    evaluation == null ? null : evaluation.getName(),
                    task.getStudentId(),
                    student == null ? null : student.getStudentNo(),
                    studentUser == null ? null : studentUser.getName(),
                    task.getCourseId(),
                    course == null ? null : course.getCourseNameCn(),
                    task.getTeacherId(),
                    teacherUser == null ? null : teacherUser.getName(),
                    teacher == null ? null : teacher.getTeacherType(),
                    task.getStatus(),
                    totalScore,
                    submission == null ? null : submission.getSubmittedAt(),
                    submission == null ? null : submission.getSubmitMode()
            );
        }).toList();
        return new PageResult<>(taskPage.getCurrent(), taskPage.getPages(), taskPage.getTotal(), records);
    }

    public List<TeacherScoreStatVO> teacherScoreStats(Long evaluationId) {
        if (evaluationMapper.selectById(evaluationId) == null) throw new BizException(404, "测评不存在");
        List<EvalTask> tasks = evalTaskMapper.selectList(new LambdaQueryWrapper<EvalTask>()
                .eq(EvalTask::getEvaluationId, evaluationId));
        if (tasks.isEmpty()) return List.of();

        Map<Long, EvalTask> taskMap = new HashMap<>();
        Set<Long> teacherIds = new HashSet<>();
        for (EvalTask task : tasks) {
            taskMap.put(task.getId(), task);
            teacherIds.add(task.getTeacherId());
        }

        Map<Long, Teacher> teacherMap = new HashMap<>();
        for (Teacher teacher : teacherMapper.selectBatchIds(teacherIds)) teacherMap.put(teacher.getId(), teacher);

        List<ScoreSubmission> submissions = scoreSubmissionMapper.selectList(new LambdaQueryWrapper<ScoreSubmission>()
                .eq(ScoreSubmission::getEvaluationId, evaluationId));
        if (submissions.isEmpty()) return List.of();

        List<Long> submissionIds = submissions.stream().map(ScoreSubmission::getId).toList();
        Map<Long, List<Score>> scoresBySubmissionId = new HashMap<>();
        for (Score score : scoreMapper.selectList(new LambdaQueryWrapper<Score>().in(Score::getSubmissionId, submissionIds))) {
            scoresBySubmissionId.computeIfAbsent(score.getSubmissionId(), k -> new ArrayList<>()).add(score);
        }

        List<Indicator> indicators = indicatorMapper.selectList(new LambdaQueryWrapper<Indicator>().eq(Indicator::getIsActive, 1));
        Map<String, Map<Long, BigDecimal>> parentWeightByType = new HashMap<>();
        Map<String, Map<Long, Long>> level2ToParentByType = new HashMap<>();
        Map<String, Map<Long, Integer>> level2CountByParentByType = new HashMap<>();
        for (Indicator indicator : indicators) {
            String teacherType = indicator.getTeacherType();
            if (!StringUtils.hasText(teacherType)) continue;
            if (indicator.getLevel() != null && indicator.getLevel() == 1) {
                parentWeightByType.computeIfAbsent(teacherType, k -> new HashMap<>())
                        .put(indicator.getId(), Optional.ofNullable(indicator.getWeight()).orElse(BigDecimal.ZERO));
            } else if (indicator.getLevel() != null && indicator.getLevel() == 2 && indicator.getParentId() != null) {
                level2ToParentByType.computeIfAbsent(teacherType, k -> new HashMap<>()).put(indicator.getId(), indicator.getParentId());
                Map<Long, Integer> cnt = level2CountByParentByType.computeIfAbsent(teacherType, k -> new HashMap<>());
                cnt.put(indicator.getParentId(), cnt.getOrDefault(indicator.getParentId(), 0) + 1);
            }
        }

        Map<Long, BigDecimal> totalByTeacher = new HashMap<>();
        Map<Long, Integer> countByTeacher = new HashMap<>();
        for (ScoreSubmission submission : submissions) {
            EvalTask task = taskMap.get(submission.getTaskId());
            if (task == null) continue;
            Teacher teacher = teacherMap.get(task.getTeacherId());
            if (teacher == null || !StringUtils.hasText(teacher.getTeacherType())) continue;

            String teacherType = teacher.getTeacherType();
            Map<Long, Long> level2ToParent = level2ToParentByType.getOrDefault(teacherType, Map.of());
            Map<Long, BigDecimal> parentWeight = parentWeightByType.getOrDefault(teacherType, Map.of());
            Map<Long, Integer> level2CountByParent = level2CountByParentByType.getOrDefault(teacherType, Map.of());
            if (level2ToParent.isEmpty() || parentWeight.isEmpty()) continue;

            Map<Long, BigDecimal> sumByParent = new HashMap<>();
            for (Score score : scoresBySubmissionId.getOrDefault(submission.getId(), List.of())) {
                Long parentId = level2ToParent.get(score.getIndicatorId());
                if (parentId == null || score.getScore() == null) continue;
                sumByParent.put(parentId, sumByParent.getOrDefault(parentId, BigDecimal.ZERO).add(score.getScore()));
            }

            BigDecimal submissionWeightedScore = BigDecimal.ZERO;
            for (Map.Entry<Long, BigDecimal> parentEntry : parentWeight.entrySet()) {
                Long parentId = parentEntry.getKey();
                BigDecimal weight = Optional.ofNullable(parentEntry.getValue()).orElse(BigDecimal.ZERO);
                int childCount = Math.max(1, level2CountByParent.getOrDefault(parentId, 0));
                BigDecimal parentSum = sumByParent.getOrDefault(parentId, BigDecimal.ZERO);
                BigDecimal avg = parentSum.divide(BigDecimal.valueOf(childCount), 6, RoundingMode.HALF_UP);
                submissionWeightedScore = submissionWeightedScore.add(avg.multiply(weight));
            }

            Long teacherId = teacher.getId();
            totalByTeacher.put(teacherId, totalByTeacher.getOrDefault(teacherId, BigDecimal.ZERO).add(submissionWeightedScore));
            countByTeacher.put(teacherId, countByTeacher.getOrDefault(teacherId, 0) + 1);
        }

        Set<Long> teacherUserIds = teacherMap.values().stream()
                .map(Teacher::getUserId)
                .filter(Objects::nonNull)
                .collect(HashSet::new, HashSet::add, HashSet::addAll);
        Map<Long, User> teacherUserMap = new HashMap<>();
        if (!teacherUserIds.isEmpty()) {
            for (User user : userMapper.selectBatchIds(teacherUserIds)) teacherUserMap.put(user.getId(), user);
        }

        List<TeacherScoreStatVO> result = new ArrayList<>();
        for (Long teacherId : totalByTeacher.keySet()) {
            Teacher teacher = teacherMap.get(teacherId);
            if (teacher == null) continue;
            User teacherUser = teacher.getUserId() == null ? null : teacherUserMap.get(teacher.getUserId());
            BigDecimal total = totalByTeacher.getOrDefault(teacherId, BigDecimal.ZERO);
            int cnt = Math.max(1, countByTeacher.getOrDefault(teacherId, 0));
            BigDecimal avg = total.divide(BigDecimal.valueOf(cnt), 4, RoundingMode.HALF_UP);
            result.add(new TeacherScoreStatVO(
                    teacherId,
                    teacherUser == null ? null : teacherUser.getName(),
                    teacher.getTeacherType(),
                    countByTeacher.getOrDefault(teacherId, 0),
                    total.setScale(4, RoundingMode.HALF_UP),
                    avg
            ));
        }
        result.sort((a, b) -> Optional.ofNullable(b.getAverageScore()).orElse(BigDecimal.ZERO)
                .compareTo(Optional.ofNullable(a.getAverageScore()).orElse(BigDecimal.ZERO)));
        return result;
    }

    public PageResult<TeacherScoreStatVO> teacherScoreStats(int page, int size, Long evaluationId, String teacherType, String teacherName) {
        int normalizedPage = Math.max(page, 1);
        int normalizedSize = Math.max(size, 1);
        List<TeacherScoreStatVO> rows = teacherScoreStats(evaluationId).stream()
                .filter(it -> !StringUtils.hasText(teacherType) || teacherType.trim().equals(it.getTeacherType()))
                .filter(it -> !StringUtils.hasText(teacherName)
                        || StringUtils.hasText(it.getTeacherName()) && it.getTeacherName().contains(teacherName.trim()))
                .toList();
        return pageList(rows, normalizedPage, normalizedSize);
    }

    public List<StudentEvaluationStatusVO> studentEvaluationStatus(Long evaluationId, String studentNo, String studentName, String className) {
        if (evaluationMapper.selectById(evaluationId) == null) throw new BizException(404, "测评不存在");
        LambdaQueryWrapper<Student> studentQuery = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(studentNo)) studentQuery.like(Student::getStudentNo, studentNo.trim());
        if (StringUtils.hasText(studentName)) {
            List<Long> userIds = userMapper.selectList(new LambdaQueryWrapper<User>()
                            .like(User::getName, studentName.trim()))
                    .stream().map(User::getId).toList();
            if (userIds.isEmpty()) return List.of();
            studentQuery.in(Student::getUserId, userIds);
        }
        if (StringUtils.hasText(className)) {
            List<Long> classIds = classMapper.selectList(new LambdaQueryWrapper<ClassEntity>()
                            .like(ClassEntity::getClassName, className.trim()))
                    .stream().map(ClassEntity::getId).toList();
            if (classIds.isEmpty()) return List.of();
            studentQuery.in(Student::getClassId, classIds);
        }

        List<Student> students = studentMapper.selectList(studentQuery);
        if (students.isEmpty()) return List.of();
        List<EvalTask> tasks = evalTaskMapper.selectList(new LambdaQueryWrapper<EvalTask>()
                .eq(EvalTask::getEvaluationId, evaluationId));
        if (tasks.isEmpty()) return List.of();

        Map<Long, Integer> totalByStudent = new HashMap<>();
        Map<Long, Integer> completedByStudent = new HashMap<>();
        for (EvalTask task : tasks) {
            Long sid = task.getStudentId();
            totalByStudent.put(sid, totalByStudent.getOrDefault(sid, 0) + 1);
            if ("completed".equals(task.getStatus())) completedByStudent.put(sid, completedByStudent.getOrDefault(sid, 0) + 1);
        }

        Set<Long> userIdsInStudents = students.stream()
                .map(Student::getUserId)
                .filter(Objects::nonNull)
                .collect(HashSet::new, HashSet::add, HashSet::addAll);
        Set<Long> classIdsInStudents = students.stream()
                .map(Student::getClassId)
                .filter(Objects::nonNull)
                .collect(HashSet::new, HashSet::add, HashSet::addAll);
        Map<Long, User> studentUserMap = new HashMap<>();
        if (!userIdsInStudents.isEmpty()) {
            for (User user : userMapper.selectBatchIds(userIdsInStudents)) studentUserMap.put(user.getId(), user);
        }
        Map<Long, ClassEntity> classEntityMap = new HashMap<>();
        if (!classIdsInStudents.isEmpty()) {
            for (ClassEntity clazz : classMapper.selectBatchIds(classIdsInStudents)) classEntityMap.put(clazz.getId(), clazz);
        }
        Set<Long> gradeIds = classEntityMap.values().stream()
                .map(ClassEntity::getGradeId)
                .filter(Objects::nonNull)
                .collect(HashSet::new, HashSet::add, HashSet::addAll);
        Map<Long, Grade> gradeMap = new HashMap<>();
        if (!gradeIds.isEmpty()) {
            for (Grade grade : gradeMapper.selectBatchIds(gradeIds)) gradeMap.put(grade.getId(), grade);
        }

        List<StudentEvaluationStatusVO> result = new ArrayList<>();
        for (Student student : students) {
            int total = totalByStudent.getOrDefault(student.getId(), 0);
            if (total <= 0) continue;
            int completed = completedByStudent.getOrDefault(student.getId(), 0);
            int pending = Math.max(0, total - completed);
            BigDecimal rate = BigDecimal.valueOf(completed).multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
            User user = student.getUserId() == null ? null : studentUserMap.get(student.getUserId());
            ClassEntity clazz = student.getClassId() == null ? null : classEntityMap.get(student.getClassId());
            Grade grade = clazz == null ? null : gradeMap.get(clazz.getGradeId());
            result.add(new StudentEvaluationStatusVO(
                    student.getId(), student.getStudentNo(), user == null ? null : user.getName(),
                    clazz == null ? null : clazz.getClassName(), clazz == null ? null : clazz.getMajor(),
                    grade == null ? null : grade.getGradeName(), total, completed, pending, rate
            ));
        }
        result.sort((a, b) -> Integer.compare(Optional.ofNullable(b.getCompletedTasks()).orElse(0), Optional.ofNullable(a.getCompletedTasks()).orElse(0)));
        return result;
    }

    public PageResult<StudentEvaluationStatusVO> studentEvaluationStatus(int page, int size, Long evaluationId, String studentNo, String studentName, String className) {
        int normalizedPage = Math.max(page, 1);
        int normalizedSize = Math.max(size, 1);
        return pageList(studentEvaluationStatus(evaluationId, studentNo, studentName, className), normalizedPage, normalizedSize);
    }

    private <T> PageResult<T> pageList(List<T> rows, int page, int size) {
        long total = rows.size();
        long pages = total == 0 ? 0 : (total + size - 1) / size;
        long offset = (long) (page - 1) * size;
        int fromIndex = offset >= rows.size() ? rows.size() : (int) offset;
        int toIndex = Math.min(fromIndex + size, rows.size());
        return new PageResult<>(page, pages, total, rows.subList(fromIndex, toIndex));
    }

    public byte[] exportIncompleteStudents(Long evaluationId) {
        List<StudentEvaluationStatusVO> rows = studentEvaluationStatus(evaluationId, null, null, null)
                .stream().filter(it -> Optional.ofNullable(it.getPendingTasks()).orElse(0) > 0).toList();
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("未完成学生名单");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("学号");
            header.createCell(1).setCellValue("姓名");
            header.createCell(2).setCellValue("年级");
            header.createCell(3).setCellValue("班级");
            header.createCell(4).setCellValue("专业");
            header.createCell(5).setCellValue("总任务数");
            header.createCell(6).setCellValue("已完成");
            header.createCell(7).setCellValue("待完成");
            header.createCell(8).setCellValue("完成率(%)");

            int rowIdx = 1;
            for (StudentEvaluationStatusVO item : rows) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(Optional.ofNullable(item.getStudentNo()).orElse(""));
                row.createCell(1).setCellValue(Optional.ofNullable(item.getStudentName()).orElse(""));
                row.createCell(2).setCellValue(Optional.ofNullable(item.getGrade()).orElse(""));
                row.createCell(3).setCellValue(Optional.ofNullable(item.getClassName()).orElse(""));
                row.createCell(4).setCellValue(Optional.ofNullable(item.getMajor()).orElse(""));
                row.createCell(5).setCellValue(Optional.ofNullable(item.getTotalTasks()).orElse(0));
                row.createCell(6).setCellValue(Optional.ofNullable(item.getCompletedTasks()).orElse(0));
                row.createCell(7).setCellValue(Optional.ofNullable(item.getPendingTasks()).orElse(0));
                row.createCell(8).setCellValue(Optional.ofNullable(item.getCompletionRate()).orElse(BigDecimal.ZERO).doubleValue());
            }
            if (rows.size() <= AUTO_SIZE_MAX_ROWS) {
                for (int i = 0; i <= 8; i++) sheet.autoSizeColumn(i);
            }
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new BizException(500, "导出未完成学生名单失败");
        }
    }

    public byte[] exportTeacherScoreStats(Long evaluationId) {
        List<TeacherScoreStatVO> rows = teacherScoreStats(evaluationId);
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("老师分数汇总");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("教师姓名");
            header.createCell(1).setCellValue("教师类型");
            header.createCell(2).setCellValue("平均分");
            int rowIdx = 1;
            for (TeacherScoreStatVO item : rows) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(Optional.ofNullable(item.getTeacherName()).orElse(""));
                row.createCell(1).setCellValue(Optional.ofNullable(item.getTeacherType()).orElse(""));
                row.createCell(2).setCellValue(Optional.ofNullable(item.getAverageScore()).orElse(BigDecimal.ZERO).doubleValue());
            }
            if (rows.size() <= AUTO_SIZE_MAX_ROWS) {
                for (int i = 0; i <= 2; i++) sheet.autoSizeColumn(i);
            }
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new BizException(500, "导出老师分数汇总失败");
        }
    }
}

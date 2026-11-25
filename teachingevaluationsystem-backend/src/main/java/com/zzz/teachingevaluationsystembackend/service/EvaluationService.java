package com.zzz.teachingevaluationsystembackend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.MybatisBatchUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zzz.teachingevaluationsystembackend.auth.AuthContext;
import com.zzz.teachingevaluationsystembackend.common.BizException;
import com.zzz.teachingevaluationsystembackend.entity.*;
import com.zzz.teachingevaluationsystembackend.mapper.*;
import com.zzz.teachingevaluationsystembackend.model.dto.PageResult;
import com.zzz.teachingevaluationsystembackend.model.dto.request.EvaluationUpsertRequest;
import com.zzz.teachingevaluationsystembackend.model.vo.EvaluationCourseImportResultVO;
import com.zzz.teachingevaluationsystembackend.model.vo.EvaluationCreateImportResultVO;
import com.zzz.teachingevaluationsystembackend.model.vo.EvaluationVO;
import com.zzz.teachingevaluationsystembackend.parser.excel.ExcelSheetReader;
import lombok.RequiredArgsConstructor;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class EvaluationService {
    private final UserMapper userMapper;
    private final StudentMapper studentMapper;
    private final TeacherMapper teacherMapper;
    private final GradeMapper gradeMapper;
    private final ClassMapper classMapper;
    private final CourseMapper courseMapper;
    private final EvaluationMapper evaluationMapper;
    private final CourseClassMapper courseClassMapper;
    private final CourseTeacherMapper courseTeacherMapper;
    private final IndicatorMapper indicatorMapper;
    private final EvalTaskMapper evalTaskMapper;
    private final ScoreSubmissionMapper scoreSubmissionMapper;
    private final ScoreMapper scoreMapper;
    private final BCryptPasswordEncoder passwordEncoder;
    private final SqlSessionFactory sqlSessionFactory;

    public PageResult<EvaluationVO> evaluations(int page, int size, String status) {
        LambdaQueryWrapper<Evaluation> query = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(status)) query.eq(Evaluation::getStatus, status);
        IPage<Evaluation> result = evaluationMapper.selectPage(Page.of(page, size), query.orderByDesc(Evaluation::getId));
        return new PageResult<>(result.getCurrent(), result.getPages(), result.getTotal(), result.getRecords().stream().map(this::evaluationVO).toList());
    }

    public Long createEvaluation(EvaluationUpsertRequest request) {
        Evaluation evaluation = new Evaluation();
        evaluation.setName(request.getName());
        evaluation.setAcademicYear(request.getAcademicYear());
        evaluation.setTerm(request.getTerm());
        evaluation.setStatus(Optional.ofNullable(request.getStatus()).orElse("draft"));
        evaluation.setCreatedBy(AuthContext.currentAdminUserId());
        evaluationMapper.insert(evaluation);
        return evaluation.getId();
    }

    @Transactional
    public EvaluationCreateImportResultVO createEvaluationWithCourses(EvaluationUpsertRequest request, MultipartFile file) {
        Long evaluationId = createEvaluation(request);
        EvaluationCourseImportResultVO importResult = importEvaluationCourses(evaluationId, file);
        return new EvaluationCreateImportResultVO(evaluationId, importResult);
    }

    public void updateEvaluation(Long id, EvaluationUpsertRequest request) {
        Evaluation evaluation = evaluationMapper.selectById(id);
        if (evaluation == null) throw new BizException(404, "测评不存在");
        evaluation.setName(request.getName());
        evaluation.setAcademicYear(request.getAcademicYear());
        evaluation.setTerm(request.getTerm());
        evaluation.setStatus(request.getStatus());
        evaluationMapper.updateById(evaluation);
    }

    @Transactional
    public void deleteEvaluation(Long id) {
        List<Course> courses = courseMapper.selectList(
                new LambdaQueryWrapper<Course>().eq(Course::getEvaluationId, id)
        );
        List<Long> courseIds = courses.stream()
                .map(Course::getId)
                .filter(Objects::nonNull)
                .toList();
        if (!courseIds.isEmpty()) {
            courseClassMapper.delete(new LambdaQueryWrapper<CourseClass>().in(CourseClass::getCourseId, courseIds));
            courseTeacherMapper.delete(new LambdaQueryWrapper<CourseTeacher>().in(CourseTeacher::getCourseId, courseIds));
            courseMapper.delete(new LambdaQueryWrapper<Course>().in(Course::getId, courseIds));
        }

        scoreMapper.delete(new LambdaQueryWrapper<Score>().eq(Score::getEvaluationId, id));
        scoreSubmissionMapper.delete(new LambdaQueryWrapper<ScoreSubmission>().eq(ScoreSubmission::getEvaluationId, id));
        evalTaskMapper.delete(new LambdaQueryWrapper<EvalTask>().eq(EvalTask::getEvaluationId, id));
        evaluationMapper.deleteById(id);
    }

    @Transactional
    public void publishEvaluation(Long id) {
        Evaluation evaluation = evaluationMapper.selectById(id);
        if (evaluation == null) throw new BizException(404, "测评不存在");
        List<Course> courses = courseMapper.selectList(new LambdaQueryWrapper<Course>().eq(Course::getEvaluationId, id));
        List<Long> courseIds = courses.stream().map(Course::getId).toList();
        Map<Long, List<Long>> classIdsByCourseId = new HashMap<>();
        Map<Long, List<Long>> teacherIdsByCourseId = new HashMap<>();
        Set<Long> allClassIds = new HashSet<>();
        if (!courseIds.isEmpty()) {
            List<CourseClass> courseClassLinks = courseClassMapper.selectList(new LambdaQueryWrapper<CourseClass>()
                    .in(CourseClass::getCourseId, courseIds));
            for (CourseClass link : courseClassLinks) {
                if (link.getCourseId() == null || link.getClassId() == null) continue;
                classIdsByCourseId.computeIfAbsent(link.getCourseId(), k -> new ArrayList<>()).add(link.getClassId());
                allClassIds.add(link.getClassId());
            }
            List<CourseTeacher> courseTeacherLinks = courseTeacherMapper.selectList(new LambdaQueryWrapper<CourseTeacher>()
                    .in(CourseTeacher::getCourseId, courseIds));
            for (CourseTeacher link : courseTeacherLinks) {
                if (link.getCourseId() == null || link.getTeacherId() == null) continue;
                teacherIdsByCourseId.computeIfAbsent(link.getCourseId(), k -> new ArrayList<>()).add(link.getTeacherId());
            }
        }

        Map<Long, List<Student>> studentsByClassId = new HashMap<>();
        if (!allClassIds.isEmpty()) {
            List<Student> students = studentMapper.selectList(new LambdaQueryWrapper<Student>().in(Student::getClassId, allClassIds));
            for (Student student : students) {
                if (student.getClassId() == null) continue;
                studentsByClassId.computeIfAbsent(student.getClassId(), k -> new ArrayList<>()).add(student);
            }
        }

        Set<String> existingTaskKeys = evalTaskMapper.selectList(new LambdaQueryWrapper<EvalTask>()
                        .eq(EvalTask::getEvaluationId, id))
                .stream().map(task -> taskKey(task.getStudentId(), task.getCourseId(), task.getTeacherId()))
                .collect(HashSet::new, HashSet::add, HashSet::addAll);

        boolean hasAnyTask = false;
        List<EvalTask> pendingTasksToInsert = new ArrayList<>();
        for (Course course : courses) {
            Long courseId = course.getId();
            List<Long> classIds = classIdsByCourseId.getOrDefault(courseId, List.of());
            List<Long> teacherIds = teacherIdsByCourseId.getOrDefault(courseId, List.of());
            for (Long classId : classIds) {
                List<Student> students = studentsByClassId.getOrDefault(classId, List.of());
                for (Student student : students) {
                    for (Long teacherId : teacherIds) {
                        hasAnyTask = true;
                        String key = taskKey(student.getId(), courseId, teacherId);
                        if (existingTaskKeys.contains(key)) continue;
                        EvalTask task = new EvalTask();
                        task.setEvaluationId(id);
                        task.setStudentId(student.getId());
                        task.setCourseId(courseId);
                        task.setTeacherId(teacherId);
                        task.setStatus("pending");
                        pendingTasksToInsert.add(task);
                        existingTaskKeys.add(key);
                    }
                }
            }
        }
        MybatisBatchUtils.execute(sqlSessionFactory, pendingTasksToInsert, EvalTaskMapper.class.getName() + ".insert", 500);
        if (!hasAnyTask) {
            throw new BizException(400, "未生成任何测评任务，请检查课程是否关联班级、课程是否关联教师、班级下是否有学生");
        }
        evaluation.setStatus("published");
        evaluation.setPublishedAt(LocalDateTime.now());
        evaluationMapper.updateById(evaluation);
        Indicator indicatorPatch = new Indicator();
        indicatorPatch.setLockedAt(LocalDateTime.now());
        indicatorMapper.update(indicatorPatch, new LambdaQueryWrapper<Indicator>().eq(Indicator::getIsActive, 1));
    }

    public EvaluationCourseImportResultVO importEvaluationCourses(Long id, MultipartFile file) {
        Evaluation evaluation = evaluationMapper.selectById(id);
        if (evaluation == null) throw new BizException(404, "测评不存在");
        if (file == null || file.isEmpty()) throw new BizException(400, "请上传Excel文件");
        String filename = Optional.ofNullable(file.getOriginalFilename()).orElse("");
        if (!(filename.endsWith(".xlsx") || filename.endsWith(".xls"))) {
            throw new BizException(400, "仅支持 .xlsx 或 .xls 文件");
        }

        int totalRows = 0;
        int successRows = 0;
        int skippedRows = 0;
        int createdCourses = 0;
        int linkedCourses = 0;
        int createdTeachers = 0;
        int linkedTeacherRelations = 0;
        int linkedClassRelations = 0;

        Map<String, Long> teacherCache = new HashMap<>();
        String lastGradeRaw = "";
        String lastMajor = "";
        Map<String, Course> courseCache = new HashMap<>();
        Set<String> courseClassRelationKeys = new HashSet<>();
        Set<String> courseTeacherRelationKeys = new HashSet<>();
        List<Course> existingCourses = courseMapper.selectList(new LambdaQueryWrapper<Course>().eq(Course::getEvaluationId, id));
        for (Course existingCourse : existingCourses) {
            courseCache.put(courseKey(existingCourse.getGradeId(), existingCourse.getCourseNameCn(), existingCourse.getCourseNameEn()), existingCourse);
        }
        if (!existingCourses.isEmpty()) {
            Set<Long> existingCourseIds = existingCourses.stream().map(Course::getId).filter(Objects::nonNull).collect(HashSet::new, HashSet::add, HashSet::addAll);
            List<CourseClass> existingCourseClasses = courseClassMapper.selectList(new LambdaQueryWrapper<CourseClass>().in(CourseClass::getCourseId, existingCourseIds));
            for (CourseClass relation : existingCourseClasses) {
                if (relation.getCourseId() == null || relation.getClassId() == null) continue;
                courseClassRelationKeys.add(relation.getCourseId() + "_" + relation.getClassId());
            }
            List<CourseTeacher> existingCourseTeachers = courseTeacherMapper.selectList(new LambdaQueryWrapper<CourseTeacher>().in(CourseTeacher::getCourseId, existingCourseIds));
            for (CourseTeacher relation : existingCourseTeachers) {
                if (relation.getCourseId() == null || relation.getTeacherId() == null) continue;
                courseTeacherRelationKeys.add(relation.getCourseId() + "_" + relation.getTeacherId());
            }
        }

        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            if (workbook.getNumberOfSheets() <= 0) throw new BizException(400, "Excel没有工作表");
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null || sheet.getPhysicalNumberOfRows() <= 1) throw new BizException(400, "Excel没有可导入的数据");
            ExcelSheetReader reader = new ExcelSheetReader(sheet.getRow(sheet.getFirstRowNum()));
            int gradeCol = reader.optionalCol("Grade", "年级");
            int majorCol = reader.optionalCol("Major", "专业", "专业名称");
            int courseCnCol = reader.requiredCol("Course Chinese Name", "课程中文名", "课程名称");
            int courseEnCol = reader.requiredCol("Course English Name", "课程英文名");
            int hduTeacherCol = reader.optionalCol("HDU Teacher", "中方教师");
            int itmoTeacher1Col = reader.optionalCol("ITMO Teacher 1(Surname, First name)", "ITMO Teacher 1");
            int itmoTeacher2Col = reader.optionalCol("ITMO Teacher 2(Surname, First name)", "ITMO Teacher 2");
            int itmoTeacher3Col = reader.optionalCol("ITMO Teacher 3(Surname, First name)", "ITMO Teacher 3");

            for (int i = sheet.getFirstRowNum() + 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                totalRows++;

                String gradeRaw = gradeCol >= 0 ? reader.cell(row, gradeCol) : "";
                String major = majorCol >= 0 ? reader.cell(row, majorCol) : "";
                String courseCn = reader.cell(row, courseCnCol);
                String courseEn = reader.cell(row, courseEnCol);

                if (StringUtils.hasText(gradeRaw)) lastGradeRaw = gradeRaw;
                if (StringUtils.hasText(major)) lastMajor = major;
                if (!StringUtils.hasText(gradeRaw)) gradeRaw = lastGradeRaw;
                if (!StringUtils.hasText(major)) major = lastMajor;

                if (!StringUtils.hasText(gradeRaw) || !StringUtils.hasText(courseCn) || !StringUtils.hasText(courseEn)) {
                    skippedRows++;
                    continue;
                }

                String gradeName = normalizeGrade(gradeRaw);
                Grade grade = ensureGrade(gradeName);

                List<Long> classIds = findClassesForCourse(grade.getId(), major);
                if (classIds.isEmpty()) {
                    skippedRows++;
                    continue;
                }

                Course course = courseCache.get(courseKey(grade.getId(), courseCn, courseEn));
                if (course == null) {
                    course = new Course();
                    course.setGradeId(grade.getId());
                    course.setEvaluationId(id);
                    course.setCourseNameCn(courseCn);
                    course.setCourseNameEn(courseEn);
                    courseMapper.insert(course);
                    courseCache.put(courseKey(grade.getId(), courseCn, courseEn), course);
                    createdCourses++;
                    linkedCourses++;
                }

                for (Long classId : classIds) {
                    String relationKey = course.getId() + "_" + classId;
                    if (!courseClassRelationKeys.contains(relationKey)) {
                        CourseClass cc = new CourseClass();
                        cc.setCourseId(course.getId());
                        cc.setClassId(classId);
                        courseClassMapper.insert(cc);
                        courseClassRelationKeys.add(relationKey);
                        linkedClassRelations++;
                    }
                }

                List<TeacherRef> teacherRefs = new ArrayList<>();
                if (hduTeacherCol >= 0) {
                    String cnTeacher = reader.cell(row, hduTeacherCol);
                    if (StringUtils.hasText(cnTeacher)) teacherRefs.add(new TeacherRef(cnTeacher, "CN"));
                }
                if (itmoTeacher1Col >= 0) {
                    String intlTeacher1 = reader.cell(row, itmoTeacher1Col);
                    if (StringUtils.hasText(intlTeacher1)) teacherRefs.add(new TeacherRef(intlTeacher1, "INTL"));
                }
                if (itmoTeacher2Col >= 0) {
                    String intlTeacher2 = reader.cell(row, itmoTeacher2Col);
                    if (StringUtils.hasText(intlTeacher2)) teacherRefs.add(new TeacherRef(intlTeacher2, "INTL"));
                }
                if (itmoTeacher3Col >= 0) {
                    String intlTeacher3 = reader.cell(row, itmoTeacher3Col);
                    if (StringUtils.hasText(intlTeacher3)) teacherRefs.add(new TeacherRef(intlTeacher3, "INTL"));
                }

                for (TeacherRef teacherRef : teacherRefs) {
                    String teacherName = normalizeTeacherName(teacherRef.name);
                    if (!StringUtils.hasText(teacherName)) continue;
                    String cacheKey = teacherRef.type + ":" + teacherName;
                    Long teacherId = teacherCache.get(cacheKey);
                    if (teacherId == null) {
                        TeacherCreateOutcome outcome = findOrCreateTeacher(teacherName, teacherRef.type);
                        teacherId = outcome.teacherId;
                        teacherCache.put(cacheKey, teacherId);
                        if (outcome.created) createdTeachers++;
                    }

                    String relationKey = course.getId() + "_" + teacherId;
                    if (!courseTeacherRelationKeys.contains(relationKey)) {
                        CourseTeacher ct = new CourseTeacher();
                        ct.setCourseId(course.getId());
                        ct.setTeacherId(teacherId);
                        courseTeacherMapper.insert(ct);
                        courseTeacherRelationKeys.add(relationKey);
                        linkedTeacherRelations++;
                    }
                }

                successRows++;
            }
        } catch (IOException e) {
            throw new BizException(400, "读取Excel失败");
        }

        return new EvaluationCourseImportResultVO(
                totalRows,
                successRows,
                skippedRows,
                createdCourses,
                linkedCourses,
                createdTeachers,
                linkedTeacherRelations,
                linkedClassRelations
        );
    }

    public void closeEvaluation(Long id) {
        Evaluation evaluation = evaluationMapper.selectById(id);
        if (evaluation == null) throw new BizException(404, "测评不存在");
        evaluation.setStatus("closed");
        evaluation.setClosedAt(LocalDateTime.now());
        evaluationMapper.updateById(evaluation);
    }

    private EvaluationVO evaluationVO(Evaluation evaluation) {
        return new EvaluationVO(
                evaluation.getId(),
                evaluation.getName(),
                evaluation.getAcademicYear(),
                evaluation.getTerm(),
                evaluation.getStatus(),
                evaluation.getPublishedAt(),
                evaluation.getClosedAt(),
                evaluation.getCreatedBy(),
                evaluation.getCreatedAt(),
                evaluation.getUpdatedAt()
        );
    }

    private String taskKey(Long studentId, Long courseId, Long teacherId) {
        return studentId + "_" + courseId + "_" + teacherId;
    }

    private Grade findGrade(String gradeName) {
        return gradeMapper.selectOne(new LambdaQueryWrapper<Grade>().eq(Grade::getGradeName, gradeName).last("limit 1"));
    }

    private Grade ensureGrade(String gradeName) {
        if (!StringUtils.hasText(gradeName)) throw new BizException(400, "年级不能为空");
        Grade grade = findGrade(gradeName);
        if (grade != null) return grade;
        grade = new Grade();
        grade.setGradeName(gradeName);
        gradeMapper.insert(grade);
        return grade;
    }

    private String normalizeGrade(String gradeRaw) {
        String grade = gradeRaw.trim();
        if (grade.endsWith("级")) return grade;
        return grade + "级";
    }

    private Course findCourseByEvaluationAndName(Long evaluationId, Long gradeId, String courseCn, String courseEn) {
        return courseMapper.selectOne(new LambdaQueryWrapper<Course>()
                .eq(Course::getEvaluationId, evaluationId)
                .eq(Course::getGradeId, gradeId)
                .eq(Course::getCourseNameCn, courseCn)
                .eq(Course::getCourseNameEn, courseEn)
                .last("limit 1"));
    }

    private String courseKey(Long gradeId, String courseCn, String courseEn) {
        return gradeId + "|" + courseCn + "|" + courseEn;
    }

    private List<Long> findClassesForCourse(Long gradeId, String major) {
        List<ClassEntity> classesInGrade = classMapper.selectList(new LambdaQueryWrapper<ClassEntity>().eq(ClassEntity::getGradeId, gradeId));
        if (classesInGrade.isEmpty()) return List.of();
        if (!StringUtils.hasText(major)) return classesInGrade.stream().map(ClassEntity::getId).toList();

        String normalizedMajor = normalizeMajorForMatch(major);
        if (!StringUtils.hasText(normalizedMajor)) return classesInGrade.stream().map(ClassEntity::getId).toList();

        List<Long> exactMatched = classesInGrade.stream()
                .filter(clazz -> majorMatches(clazz.getMajor(), major))
                .map(ClassEntity::getId)
                .toList();
        if (!exactMatched.isEmpty()) return exactMatched;

        return classesInGrade.stream()
                .filter(clazz -> {
                    String clazzMajor = normalizeMajorForMatch(clazz.getMajor());
                    return clazzMajor.contains(normalizedMajor) || normalizedMajor.contains(clazzMajor);
                })
                .map(ClassEntity::getId)
                .toList();
    }

    private boolean majorMatches(String classMajor, String courseMajor) {
        String left = normalizeMajorForMatch(classMajor);
        String right = normalizeMajorForMatch(courseMajor);
        if (!StringUtils.hasText(left) || !StringUtils.hasText(right)) return false;
        if (left.equals(right)) return true;
        return left.contains(right) || right.contains(left);
    }

    private String normalizeMajorForMatch(String rawMajor) {
        if (!StringUtils.hasText(rawMajor)) return "";
        String value = rawMajor.trim()
                .replace('（', '(')
                .replace('）', ')')
                .replaceAll("\\s+", "");
        value = value.replaceAll("\\([^)]*\\)$", "");
        value = value.replaceAll("[-_/·•]+", "");
        return value.toLowerCase(Locale.ROOT);
    }

    private TeacherCreateOutcome findOrCreateTeacher(String teacherName, String teacherType) {
        List<Teacher> teachers = teacherMapper.selectList(new LambdaQueryWrapper<Teacher>().eq(Teacher::getTeacherType, teacherType));
        Set<Long> userIds = teachers.stream()
                .map(Teacher::getUserId)
                .filter(Objects::nonNull)
                .collect(HashSet::new, HashSet::add, HashSet::addAll);
        Map<Long, User> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            for (User user : userMapper.selectBatchIds(userIds)) userMap.put(user.getId(), user);
        }

        Teacher existingTeacher = teachers.stream()
                .filter(t -> {
                    if (t.getUserId() == null) return false;
                    User u = userMap.get(t.getUserId());
                    return u != null && teacherName.equals(u.getName());
                })
                .findFirst()
                .orElse(null);
        if (existingTeacher != null) {
            return new TeacherCreateOutcome(existingTeacher.getId(), false);
        }

        String username = generateTeacherUsername(teacherName, teacherType);
        User user = createUser(username, teacherName, username, "teacher");
        Teacher teacher = new Teacher();
        teacher.setUserId(user.getId());
        teacher.setTeacherType(teacherType);
        teacherMapper.insert(teacher);
        return new TeacherCreateOutcome(teacher.getId(), true);
    }

    private String generateTeacherUsername(String teacherName, String teacherType) {
        String base = teacherName.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "").trim();
        if (!StringUtils.hasText(base)) base = "teacher";
        base = base + "_" + teacherType.toLowerCase(Locale.ROOT);
        String username = base;
        int suffix = 1;
        while (userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username).last("limit 1")) != null) {
            username = base + "_" + suffix++;
        }
        return username;
    }

    private User createUser(String username, String name, String password, String role) {
        User user = new User();
        user.setUsername(username);
        user.setName(name);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(role);
        user.setIsActive(1);
        userMapper.insert(user);
        return user;
    }

    private String normalizeTeacherName(String rawName) {
        String value = rawName == null ? "" : rawName.trim();
        return value.replaceAll("\\s+", " ");
    }

    private record TeacherRef(String name, String type) {
    }

    private record TeacherCreateOutcome(Long teacherId, boolean created) {
    }
}

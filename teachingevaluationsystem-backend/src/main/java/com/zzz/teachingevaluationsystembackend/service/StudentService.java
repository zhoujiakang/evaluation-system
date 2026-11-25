package com.zzz.teachingevaluationsystembackend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zzz.teachingevaluationsystembackend.common.BizException;
import com.zzz.teachingevaluationsystembackend.entity.ClassEntity;
import com.zzz.teachingevaluationsystembackend.entity.Grade;
import com.zzz.teachingevaluationsystembackend.entity.Student;
import com.zzz.teachingevaluationsystembackend.entity.User;
import com.zzz.teachingevaluationsystembackend.mapper.ClassMapper;
import com.zzz.teachingevaluationsystembackend.mapper.GradeMapper;
import com.zzz.teachingevaluationsystembackend.mapper.StudentMapper;
import com.zzz.teachingevaluationsystembackend.mapper.UserMapper;
import com.zzz.teachingevaluationsystembackend.model.dto.PageResult;
import com.zzz.teachingevaluationsystembackend.model.dto.request.ResetPasswordRequest;
import com.zzz.teachingevaluationsystembackend.model.dto.request.StudentCreateRequest;
import com.zzz.teachingevaluationsystembackend.model.dto.request.StudentUpdateRequest;
import com.zzz.teachingevaluationsystembackend.model.vo.StudentImportResultVO;
import com.zzz.teachingevaluationsystembackend.model.vo.StudentVO;
import com.zzz.teachingevaluationsystembackend.parser.excel.ExcelSheetReader;
import lombok.RequiredArgsConstructor;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class StudentService {
    private final UserMapper userMapper;
    private final StudentMapper studentMapper;
    private final GradeMapper gradeMapper;
    private final ClassMapper classMapper;
    private final BCryptPasswordEncoder passwordEncoder;

    public PageResult<StudentVO> students(int page, int size, String className, Integer enrollYear, String studentNo, String name) {
        List<ClassEntity> classes = classMapper.selectList(new LambdaQueryWrapper<ClassEntity>()
                .like(StringUtils.hasText(className), ClassEntity::getClassName, className));
        Set<Long> classIds = new HashSet<>(classes.stream().map(ClassEntity::getId).toList());
        LambdaQueryWrapper<Student> query = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(className)) {
            if (classIds.isEmpty()) return new PageResult<>(page, 0, 0, List.of());
            query.in(Student::getClassId, classIds);
        }
        if (enrollYear != null) {
            Grade grade = findGrade(enrollYear + "级");
            if (grade == null) return new PageResult<>(page, 0, 0, List.of());
            List<Long> ids = classMapper.selectList(new LambdaQueryWrapper<ClassEntity>().eq(ClassEntity::getGradeId, grade.getId()))
                    .stream().map(ClassEntity::getId).toList();
            if (ids.isEmpty()) return new PageResult<>(page, 0, 0, List.of());
            query.in(Student::getClassId, ids);
        }
        if (StringUtils.hasText(studentNo)) {
            query.like(Student::getStudentNo, studentNo.trim());
        }
        if (StringUtils.hasText(name)) {
            List<Long> matchedUserIds = userMapper.selectList(new LambdaQueryWrapper<User>()
                            .like(User::getName, name.trim()))
                    .stream()
                    .map(User::getId)
                    .toList();
            if (matchedUserIds.isEmpty()) return new PageResult<>(page, 0, 0, List.of());
            query.in(Student::getUserId, matchedUserIds);
        }
        IPage<Student> result = studentMapper.selectPage(Page.of(page, size), query.orderByDesc(Student::getId));
        List<Student> records = result.getRecords();
        if (records.isEmpty()) {
            return new PageResult<>(result.getCurrent(), result.getPages(), result.getTotal(), List.of());
        }

        Set<Long> userIds = records.stream().map(Student::getUserId).filter(Objects::nonNull).collect(HashSet::new, HashSet::add, HashSet::addAll);
        Set<Long> classIdsInPage = records.stream().map(Student::getClassId).filter(Objects::nonNull).collect(HashSet::new, HashSet::add, HashSet::addAll);
        Map<Long, User> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            for (User user : userMapper.selectBatchIds(userIds)) userMap.put(user.getId(), user);
        }
        Map<Long, ClassEntity> classMap = new HashMap<>();
        if (!classIdsInPage.isEmpty()) {
            for (ClassEntity clazz : classMapper.selectBatchIds(classIdsInPage)) classMap.put(clazz.getId(), clazz);
        }
        Set<Long> gradeIds = classMap.values().stream().map(ClassEntity::getGradeId).filter(Objects::nonNull).collect(HashSet::new, HashSet::add, HashSet::addAll);
        Map<Long, Grade> gradeMap = new HashMap<>();
        if (!gradeIds.isEmpty()) {
            for (Grade gradeEntity : gradeMapper.selectBatchIds(gradeIds)) gradeMap.put(gradeEntity.getId(), gradeEntity);
        }

        return new PageResult<>(result.getCurrent(), result.getPages(), result.getTotal(), records.stream()
                .map(it -> studentMap(it, userMap, classMap, gradeMap))
                .toList());
    }

    @Transactional
    public Long createStudent(StudentCreateRequest request) {
        User user = createUser(request.getUsername(), request.getName(), request.getPassword(), "student");
        Student student = new Student();
        student.setUserId(user.getId());
        student.setStudentNo(request.getStudentNo());
        student.setClassId(ensureClass(request.getClassName(), request.getMajor(), gradeName(request.getGrade(), request.getEnrollYear())).getId());
        studentMapper.insert(student);
        return student.getId();
    }

    @Transactional
    public StudentImportResultVO importStudents(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new BizException(400, "请上传Excel文件");
        String filename = Optional.ofNullable(file.getOriginalFilename()).orElse("");
        if (!(filename.endsWith(".xlsx") || filename.endsWith(".xls"))) {
            throw new BizException(400, "仅支持 .xlsx 或 .xls 文件");
        }

        Set<Long> createdGradeIds = new HashSet<>();
        Set<Long> createdClassIds = new HashSet<>();
        int totalRows = 0;
        int successRows = 0;
        int skippedRows = 0;
        int createdUsers = 0;
        int createdStudents = 0;
        int updatedStudents = 0;

        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            if (workbook.getNumberOfSheets() <= 0) throw new BizException(400, "Excel没有工作表");
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null || sheet.getPhysicalNumberOfRows() <= 1) throw new BizException(400, "Excel没有可导入的数据");
            ExcelSheetReader reader = new ExcelSheetReader(sheet.getRow(sheet.getFirstRowNum()));
            int studentNoCol = reader.requiredCol("学号");
            int nameCol = reader.requiredCol("姓名");
            int gradeCol = reader.requiredCol("年级");
            int classCol = reader.requiredCol("班级");
            int majorCol = reader.optionalCol("专业名称", "招生专业", "专业号");

            for (int i = sheet.getFirstRowNum() + 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                totalRows++;
                String studentNo = reader.cell(row, studentNoCol);
                String name = reader.cell(row, nameCol);
                String gradeRaw = reader.cell(row, gradeCol);
                String className = reader.cell(row, classCol);
                String major = majorCol >= 0 ? reader.cell(row, majorCol) : "";
                if (!StringUtils.hasText(studentNo) || !StringUtils.hasText(name) || !StringUtils.hasText(gradeRaw) || !StringUtils.hasText(className)) {
                    skippedRows++;
                    continue;
                }

                String gradeName = normalizeGrade(gradeRaw);
                Grade existingGrade = findGrade(gradeName);
                ClassEntity existingClass = findClass(className, gradeName);
                ImportUpsertOutcome outcome = upsertStudentByNo(studentNo, name, className, major, gradeName);

                Grade ensuredGrade = existingGrade == null ? findGrade(gradeName) : existingGrade;
                if (existingGrade == null && ensuredGrade != null) createdGradeIds.add(ensuredGrade.getId());
                ClassEntity ensuredClass = existingClass == null ? findClass(className, gradeName) : existingClass;
                if (existingClass == null && ensuredClass != null) createdClassIds.add(ensuredClass.getId());
                if (outcome.createdUser) createdUsers++;
                if (outcome.createdStudent) createdStudents++;
                if (outcome.updatedStudent) updatedStudents++;
                successRows++;
            }
        } catch (IOException e) {
            throw new BizException(400, "读取Excel失败");
        }
        return new StudentImportResultVO(
                totalRows,
                successRows,
                skippedRows,
                Math.max(createdUsers, 0),
                Math.max(createdStudents, 0),
                updatedStudents,
                createdGradeIds.size(),
                createdClassIds.size()
        );
    }

    @Transactional
    public void updateStudent(Long id, StudentUpdateRequest request) {
        Student student = studentMapper.selectById(id);
        if (student == null) throw new BizException(404, "学生不存在");
        User user = userMapper.selectById(student.getUserId());
        if (user != null) {
            user.setUsername(request.getUsername());
            user.setName(request.getName());
            userMapper.updateById(user);
        }
        student.setStudentNo(request.getStudentNo());
        student.setClassId(ensureClass(request.getClassName(), request.getMajor(), gradeName(request.getGrade(), request.getEnrollYear())).getId());
        studentMapper.updateById(student);
    }

    @Transactional
    public void deleteStudent(Long id) {
        Student student = studentMapper.selectById(id);
        studentMapper.deleteById(id);
        if (student != null && student.getUserId() != null) userMapper.deleteById(student.getUserId());
    }

    public void resetStudentPassword(Long id, ResetPasswordRequest request) {
        Student student = studentMapper.selectById(id);
        if (student == null) throw new BizException(404, "学生不存在");
        resetPassword(student.getUserId(), request.getNewPassword());
    }

    private StudentVO studentMap(Student student) {
        User user = userMapper.selectById(student.getUserId());
        ClassEntity clazz = classMapper.selectById(student.getClassId());
        Grade grade = clazz == null ? null : gradeMapper.selectById(clazz.getGradeId());
        return studentMapBuild(student, user, clazz, grade);
    }

    private StudentVO studentMap(Student student, Map<Long, User> userMap, Map<Long, ClassEntity> classMap, Map<Long, Grade> gradeMap) {
        User user = userMap.get(student.getUserId());
        ClassEntity clazz = classMap.get(student.getClassId());
        Grade grade = clazz == null ? null : gradeMap.get(clazz.getGradeId());
        return studentMapBuild(student, user, clazz, grade);
    }

    private StudentVO studentMapBuild(Student student, User user, ClassEntity clazz, Grade grade) {
        return new StudentVO(
                student.getId(),
                student.getUserId(),
                user == null ? null : user.getUsername(),
                user == null ? null : user.getName(),
                student.getStudentNo(),
                clazz == null ? null : clazz.getClassName(),
                clazz == null ? null : clazz.getMajor(),
                grade == null ? null : grade.getGradeName()
        );
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

    private void resetPassword(Long userId, String newPassword) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new BizException(404, "用户不存在");
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);
    }

    private Grade findGrade(String gradeName) {
        return gradeMapper.selectOne(new LambdaQueryWrapper<Grade>().eq(Grade::getGradeName, gradeName).last("limit 1"));
    }

    private ClassEntity findClass(String className, String gradeName) {
        Grade grade = findGrade(gradeName);
        if (grade == null) return null;
        return classMapper.selectOne(new LambdaQueryWrapper<ClassEntity>()
                .eq(ClassEntity::getClassName, className)
                .eq(ClassEntity::getGradeId, grade.getId())
                .last("limit 1"));
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

    private ClassEntity ensureClass(String className, String major, String gradeName) {
        ClassEntity entity = classMapper.selectOne(new LambdaQueryWrapper<ClassEntity>()
                .eq(ClassEntity::getClassName, className)
                .eq(ClassEntity::getGradeId, ensureGrade(gradeName).getId())
                .last("limit 1"));
        if (entity != null) return entity;
        entity = new ClassEntity();
        entity.setClassName(className);
        entity.setMajor(major);
        entity.setGradeId(ensureGrade(gradeName).getId());
        classMapper.insert(entity);
        return entity;
    }

    private String gradeName(String grade, Integer enrollYear) {
        if (StringUtils.hasText(grade)) return grade;
        return enrollYear == null ? null : enrollYear + "级";
    }

    private String normalizeGrade(String gradeRaw) {
        String grade = gradeRaw.trim();
        if (grade.endsWith("级")) return grade;
        return grade + "级";
    }

    private ImportUpsertOutcome upsertStudentByNo(String studentNo, String name, String className, String major, String gradeName) {
        ImportUpsertOutcome outcome = new ImportUpsertOutcome();
        Student existing = studentMapper.selectOne(new LambdaQueryWrapper<Student>()
                .eq(Student::getStudentNo, studentNo)
                .last("limit 1"));
        ClassEntity clazz = ensureClass(className, major, gradeName);
        if (existing == null) {
            User user = createUser(studentNo, name, studentNo, "student");
            Student student = new Student();
            student.setUserId(user.getId());
            student.setStudentNo(studentNo);
            student.setClassId(clazz.getId());
            studentMapper.insert(student);
            outcome.createdUser = true;
            outcome.createdStudent = true;
            return outcome;
        }
        existing.setClassId(clazz.getId());
        studentMapper.updateById(existing);
        outcome.updatedStudent = true;
        User user = existing.getUserId() == null ? null : userMapper.selectById(existing.getUserId());
        if (user == null) {
            User created = createUser(studentNo, name, studentNo, "student");
            existing.setUserId(created.getId());
            studentMapper.updateById(existing);
            outcome.createdUser = true;
            return outcome;
        }
        user.setUsername(studentNo);
        user.setName(name);
        if (!StringUtils.hasText(user.getPasswordHash())) {
            user.setPasswordHash(passwordEncoder.encode(studentNo));
        }
        if (!StringUtils.hasText(user.getRole())) {
            user.setRole("student");
        }
        userMapper.updateById(user);
        return outcome;
    }

    private static class ImportUpsertOutcome {
        private boolean createdUser;
        private boolean createdStudent;
        private boolean updatedStudent;
    }
}

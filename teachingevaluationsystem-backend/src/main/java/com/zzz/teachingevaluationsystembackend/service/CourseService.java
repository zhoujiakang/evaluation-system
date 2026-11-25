package com.zzz.teachingevaluationsystembackend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.MybatisBatchUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zzz.teachingevaluationsystembackend.common.BizException;
import com.zzz.teachingevaluationsystembackend.entity.*;
import com.zzz.teachingevaluationsystembackend.mapper.*;
import com.zzz.teachingevaluationsystembackend.model.dto.PageResult;
import com.zzz.teachingevaluationsystembackend.model.dto.request.CourseUpsertRequest;
import com.zzz.teachingevaluationsystembackend.model.vo.CourseVO;
import lombok.RequiredArgsConstructor;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CourseService {
    private final GradeMapper gradeMapper;
    private final CourseMapper courseMapper;
    private final CourseClassMapper courseClassMapper;
    private final CourseTeacherMapper courseTeacherMapper;
    private final SqlSessionFactory sqlSessionFactory;

    public PageResult<CourseVO> courses(int page, int size, Long evaluationId, String grade) {
        LambdaQueryWrapper<Course> query = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(grade)) {
            Grade gradeEntity = findGrade(grade);
            query.eq(Course::getGradeId, gradeEntity == null ? -1L : gradeEntity.getId());
        }
        if (evaluationId != null) query.eq(Course::getEvaluationId, evaluationId);
        IPage<Course> result = courseMapper.selectPage(Page.of(page, size), query.orderByDesc(Course::getId));
        List<Course> courses = result.getRecords();
        if (courses.isEmpty()) {
            return new PageResult<>(result.getCurrent(), result.getPages(), result.getTotal(), List.of());
        }

        Set<Long> gradeIds = courses.stream().map(Course::getGradeId).filter(id -> id != null).collect(HashSet::new, HashSet::add, HashSet::addAll);
        Set<Long> courseIds = courses.stream().map(Course::getId).filter(id -> id != null).collect(HashSet::new, HashSet::add, HashSet::addAll);

        Map<Long, Grade> gradeMap = new HashMap<>();
        if (!gradeIds.isEmpty()) {
            for (Grade gradeEntity : gradeMapper.selectBatchIds(gradeIds)) gradeMap.put(gradeEntity.getId(), gradeEntity);
        }

        Map<Long, List<Long>> classIdsByCourseId = new HashMap<>();
        Map<Long, List<Long>> teacherIdsByCourseId = new HashMap<>();
        if (!courseIds.isEmpty()) {
            List<CourseClass> courseClasses = courseClassMapper.selectList(new LambdaQueryWrapper<CourseClass>()
                    .in(CourseClass::getCourseId, courseIds));
            for (CourseClass link : courseClasses) {
                if (link.getCourseId() == null || link.getClassId() == null) continue;
                classIdsByCourseId.computeIfAbsent(link.getCourseId(), k -> new ArrayList<>()).add(link.getClassId());
            }
            List<CourseTeacher> courseTeachers = courseTeacherMapper.selectList(new LambdaQueryWrapper<CourseTeacher>()
                    .in(CourseTeacher::getCourseId, courseIds));
            for (CourseTeacher link : courseTeachers) {
                if (link.getCourseId() == null || link.getTeacherId() == null) continue;
                teacherIdsByCourseId.computeIfAbsent(link.getCourseId(), k -> new ArrayList<>()).add(link.getTeacherId());
            }
        }

        return new PageResult<>(result.getCurrent(), result.getPages(), result.getTotal(), courses.stream()
                .map(it -> courseMap(it, gradeMap, classIdsByCourseId, teacherIdsByCourseId))
                .toList());
    }

    @Transactional
    public Long createCourse(CourseUpsertRequest request) {
        Course course = new Course();
        course.setGradeId(ensureGrade(request.getGrade()).getId());
        course.setEvaluationId(request.getEvaluationId());
        course.setCourseNameCn(request.getCourseNameCn());
        course.setCourseNameEn(request.getCourseNameEn());
        courseMapper.insert(course);
        saveCourseLinks(course.getId(), request);
        return course.getId();
    }

    @Transactional
    public void updateCourse(Long id, CourseUpsertRequest request) {
        Course course = courseMapper.selectById(id);
        if (course == null) throw new BizException(404, "课程不存在");
        course.setGradeId(ensureGrade(request.getGrade()).getId());
        course.setEvaluationId(request.getEvaluationId());
        course.setCourseNameCn(request.getCourseNameCn());
        course.setCourseNameEn(request.getCourseNameEn());
        courseMapper.updateById(course);
        courseClassMapper.delete(new LambdaQueryWrapper<CourseClass>().eq(CourseClass::getCourseId, id));
        courseTeacherMapper.delete(new LambdaQueryWrapper<CourseTeacher>().eq(CourseTeacher::getCourseId, id));
        saveCourseLinks(id, request);
    }

    public void deleteCourse(Long id) {
        courseClassMapper.delete(new LambdaQueryWrapper<CourseClass>().eq(CourseClass::getCourseId, id));
        courseTeacherMapper.delete(new LambdaQueryWrapper<CourseTeacher>().eq(CourseTeacher::getCourseId, id));
        courseMapper.deleteById(id);
    }

    private CourseVO courseMap(Course course, Map<Long, Grade> gradeMap, Map<Long, List<Long>> classIdsByCourseId,
                               Map<Long, List<Long>> teacherIdsByCourseId) {
        Grade grade = gradeMap.get(course.getGradeId());
        List<Long> classIds = classIdsByCourseId.getOrDefault(course.getId(), List.of());
        List<Long> teacherIds = teacherIdsByCourseId.getOrDefault(course.getId(), List.of());
        return new CourseVO(course.getId(), grade == null ? null : grade.getGradeName(), course.getCourseNameCn(), course.getCourseNameEn(), classIds, teacherIds);
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

    private Grade findGrade(String gradeName) {
        return gradeMapper.selectOne(new LambdaQueryWrapper<Grade>().eq(Grade::getGradeName, gradeName).last("limit 1"));
    }

    private void saveCourseLinks(Long courseId, CourseUpsertRequest request) {
        List<Long> classIds = request.getClassIds() == null ? List.of() : request.getClassIds();
        List<CourseClass> courseClassRows = new ArrayList<>();
        for (Long classId : classIds) {
            CourseClass link = new CourseClass();
            link.setCourseId(courseId);
            link.setClassId(classId);
            courseClassRows.add(link);
        }
        MybatisBatchUtils.execute(sqlSessionFactory, courseClassRows, CourseClassMapper.class.getName() + ".insert", 500);

        List<Long> teacherIds = request.getTeacherIds() == null ? List.of() : request.getTeacherIds();
        List<CourseTeacher> courseTeacherRows = new ArrayList<>();
        for (Long teacherId : teacherIds) {
            CourseTeacher link = new CourseTeacher();
            link.setCourseId(courseId);
            link.setTeacherId(teacherId);
            courseTeacherRows.add(link);
        }
        MybatisBatchUtils.execute(sqlSessionFactory, courseTeacherRows, CourseTeacherMapper.class.getName() + ".insert", 500);
    }
}

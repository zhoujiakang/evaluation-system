package com.zzz.teachingevaluationsystembackend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zzz.teachingevaluationsystembackend.common.BizException;
import com.zzz.teachingevaluationsystembackend.entity.ClassEntity;
import com.zzz.teachingevaluationsystembackend.entity.Grade;
import com.zzz.teachingevaluationsystembackend.mapper.ClassMapper;
import com.zzz.teachingevaluationsystembackend.mapper.GradeMapper;
import com.zzz.teachingevaluationsystembackend.model.dto.PageResult;
import com.zzz.teachingevaluationsystembackend.model.dto.request.ClassUpsertRequest;
import com.zzz.teachingevaluationsystembackend.model.vo.ClassVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;

@Service
@RequiredArgsConstructor
public class ClassService {
    private final ClassMapper classMapper;
    private final GradeMapper gradeMapper;

    public PageResult<ClassVO> classes(int page, int size, String className, String major, String grade) {
        LambdaQueryWrapper<ClassEntity> query = new LambdaQueryWrapper<>();
        query.orderByDesc(ClassEntity::getClassName);
        if (StringUtils.hasText(className)) query.like(ClassEntity::getClassName, className);
        if (StringUtils.hasText(major)) query.like(ClassEntity::getMajor, major);
        if (StringUtils.hasText(grade)) {
            Grade gradeEntity = findGrade(grade);
            query.eq(ClassEntity::getGradeId, gradeEntity == null ? -1L : gradeEntity.getId());
        }
        IPage<ClassEntity> result = classMapper.selectPage(Page.of(page, size), query.orderByDesc(ClassEntity::getId));
        Set<Long> gradeIds = result.getRecords().stream()
                .map(ClassEntity::getGradeId)
                .filter(Objects::nonNull)
                .collect(HashSet::new, HashSet::add, HashSet::addAll);
        Map<Long, Grade> gradeMap = new HashMap<>();
        if (!gradeIds.isEmpty()) {
            for (Grade gradeEntity : gradeMapper.selectBatchIds(gradeIds)) gradeMap.put(gradeEntity.getId(), gradeEntity);
        }
        return new PageResult<>(result.getCurrent(), result.getPages(), result.getTotal(), result.getRecords().stream().map(it -> classMap(it, gradeMap)).toList());
    }

    public Long createClass(ClassUpsertRequest request) {
        ClassEntity entity = new ClassEntity();
        entity.setClassName(request.getClassName());
        entity.setMajor(request.getMajor());
        entity.setGradeId(ensureGrade(request.getGrade()).getId());
        classMapper.insert(entity);
        return entity.getId();
    }

    public void updateClass(Long id, ClassUpsertRequest request) {
        ClassEntity entity = classMapper.selectById(id);
        if (entity == null) throw new BizException(404, "班级不存在");
        entity.setClassName(request.getClassName());
        entity.setMajor(request.getMajor());
        entity.setGradeId(ensureGrade(request.getGrade()).getId());
        classMapper.updateById(entity);
    }

    public void deleteClass(Long id) {
        classMapper.deleteById(id);
    }

    private ClassVO classMap(ClassEntity entity, Map<Long, Grade> gradeMap) {
        Grade grade = gradeMap.get(entity.getGradeId());
        return new ClassVO(entity.getId(), entity.getClassName(), entity.getMajor(), grade == null ? null : grade.getGradeName());
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
}

package com.zzz.teachingevaluationsystembackend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zzz.teachingevaluationsystembackend.common.BizException;
import com.zzz.teachingevaluationsystembackend.entity.Indicator;
import com.zzz.teachingevaluationsystembackend.mapper.IndicatorMapper;
import com.zzz.teachingevaluationsystembackend.model.dto.PageResult;
import com.zzz.teachingevaluationsystembackend.model.dto.request.IndicatorUpsertRequest;
import com.zzz.teachingevaluationsystembackend.model.vo.IndicatorVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class IndicatorService {
    private final IndicatorMapper indicatorMapper;

    public PageResult<IndicatorVO> indicators(int page, int size, String teacherType, Integer level, Long parentId) {
        LambdaQueryWrapper<Indicator> query = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(teacherType)) query.eq(Indicator::getTeacherType, teacherType);
        if (level != null) query.eq(Indicator::getLevel, level);
        if (parentId != null) query.eq(Indicator::getParentId, parentId);
        IPage<Indicator> result = indicatorMapper.selectPage(Page.of(page, size), query.orderByAsc(Indicator::getSortOrder).orderByDesc(Indicator::getId));
        return new PageResult<>(result.getCurrent(), result.getPages(), result.getTotal(), result.getRecords().stream().map(this::indicatorVO).toList());
    }

    public Long createIndicator(IndicatorUpsertRequest request) {
        Indicator indicator = fillIndicator(new Indicator(), request);
        indicatorMapper.insert(indicator);
        return indicator.getId();
    }

    public void updateIndicator(Long id, IndicatorUpsertRequest request) {
        Indicator indicator = indicatorMapper.selectById(id);
        if (indicator == null) throw new BizException(404, "指标不存在");
        indicatorMapper.updateById(fillIndicator(indicator, request));
    }

    public void deleteIndicator(Long id) {
        indicatorMapper.deleteById(id);
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

    private Indicator fillIndicator(Indicator indicator, IndicatorUpsertRequest request) {
        indicator.setTeacherType(request.getTeacherType());
        indicator.setLevel(request.getLevel());
        indicator.setParentId(request.getParentId());
        indicator.setName(request.getName());
        indicator.setNameEn(request.getNameEn());
        indicator.setWeight(request.getWeight());
        indicator.setSortOrder(Optional.ofNullable(request.getSortOrder()).orElse(0));
        indicator.setIsActive(Optional.ofNullable(request.getIsActive()).orElse(1));
        return indicator;
    }
}

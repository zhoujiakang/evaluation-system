package com.zzz.teachingevaluationsystembackend.model.dto.request;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class IndicatorUpsertRequest {
    private String teacherType;
    private Integer level;
    private Long parentId;
    private String name;
    private String nameEn;
    private BigDecimal weight;
    private Integer sortOrder;
    private Integer isActive;
}

package com.zzz.teachingevaluationsystembackend.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class IndicatorVO {
    private Long id;
    private String teacherType;
    private Integer level;
    private Long parentId;
    private String name;
    private String nameEn;
    private BigDecimal weight;
    private Integer sortOrder;
    private Integer isActive;
    private LocalDateTime lockedAt;
}

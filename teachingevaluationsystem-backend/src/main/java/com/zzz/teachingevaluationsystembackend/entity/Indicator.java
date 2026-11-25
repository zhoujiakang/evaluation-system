package com.zzz.teachingevaluationsystembackend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("indicator")
public class Indicator {
    @TableId(type = IdType.AUTO)
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
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

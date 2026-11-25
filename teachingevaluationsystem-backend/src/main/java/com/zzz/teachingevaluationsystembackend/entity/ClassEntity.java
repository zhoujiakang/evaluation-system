package com.zzz.teachingevaluationsystembackend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("class")
public class ClassEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String className;
    private String major;
    private Long gradeId;
    private LocalDateTime createdAt;
}

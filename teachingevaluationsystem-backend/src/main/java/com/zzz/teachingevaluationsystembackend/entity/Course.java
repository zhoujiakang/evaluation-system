package com.zzz.teachingevaluationsystembackend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("course")
public class Course {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long gradeId;
    private Long evaluationId;
    private String courseNameCn;
    private String courseNameEn;
    private LocalDateTime createdAt;
}

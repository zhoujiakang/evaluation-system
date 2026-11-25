package com.zzz.teachingevaluationsystembackend.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class TeacherScoreStatVO {
    private Long teacherId;
    private String teacherName;
    private String teacherType;
    private Integer submissionCount;
    private BigDecimal totalScoreSum;
    private BigDecimal averageScore;
}

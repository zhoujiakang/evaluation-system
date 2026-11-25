package com.zzz.teachingevaluationsystembackend.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class AdminScoreVO {
    private Long taskId;
    private Long evaluationId;
    private String evaluationName;
    private Long studentId;
    private String studentNo;
    private String studentName;
    private Long courseId;
    private String courseNameCn;
    private Long teacherId;
    private String teacherName;
    private String teacherType;
    private String taskStatus;
    private BigDecimal totalScore;
    private LocalDateTime submittedAt;
    private String submitMode;
}

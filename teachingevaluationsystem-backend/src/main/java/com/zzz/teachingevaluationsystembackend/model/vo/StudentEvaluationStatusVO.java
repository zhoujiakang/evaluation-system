package com.zzz.teachingevaluationsystembackend.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class StudentEvaluationStatusVO {
    private Long studentId;
    private String studentNo;
    private String studentName;
    private String className;
    private String major;
    private String grade;
    private Integer totalTasks;
    private Integer completedTasks;
    private Integer pendingTasks;
    private BigDecimal completionRate;
}

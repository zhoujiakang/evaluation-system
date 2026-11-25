package com.zzz.teachingevaluationsystembackend.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class StudentEvaluationProgressVO {
    private Long evaluationId;
    private String evaluationName;
    private Integer total;
    private Integer completed;
}

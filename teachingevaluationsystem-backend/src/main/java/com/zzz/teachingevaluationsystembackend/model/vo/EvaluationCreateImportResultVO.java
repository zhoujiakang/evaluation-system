package com.zzz.teachingevaluationsystembackend.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class EvaluationCreateImportResultVO {
    private Long evaluationId;
    private EvaluationCourseImportResultVO importResult;
}

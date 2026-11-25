package com.zzz.teachingevaluationsystembackend.model.dto.request;

import lombok.Data;

@Data
public class EvaluationUpsertRequest {
    private String name;
    private String academicYear;
    private String term;
    private String status;
}

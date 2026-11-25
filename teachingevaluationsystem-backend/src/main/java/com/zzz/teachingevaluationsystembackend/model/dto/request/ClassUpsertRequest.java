package com.zzz.teachingevaluationsystembackend.model.dto.request;

import lombok.Data;

@Data
public class ClassUpsertRequest {
    private String className;
    private String major;
    private String grade;
}

package com.zzz.teachingevaluationsystembackend.model.dto.request;

import lombok.Data;

@Data
public class StudentUpdateRequest {
    private String username;
    private String name;
    private String studentNo;
    private String className;
    private String major;
    private String grade;
    private Integer enrollYear;
}

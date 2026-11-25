package com.zzz.teachingevaluationsystembackend.model.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class CourseUpsertRequest {
    private Long evaluationId;
    private String grade;
    private String major;
    private String courseNameCn;
    private String courseNameEn;
    private List<Long> classIds;
    private List<Long> teacherIds;
}

package com.zzz.teachingevaluationsystembackend.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class CourseVO {
    private Long id;
    private String grade;
    private String courseNameCn;
    private String courseNameEn;
    private List<Long> classIds;
    private List<Long> teacherIds;
}

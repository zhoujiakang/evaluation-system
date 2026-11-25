package com.zzz.teachingevaluationsystembackend.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class StudentCourseTaskVO {
    private Long courseId;
    private String courseNameCn;
    private String courseNameEn;
    private List<StudentCourseTeacherTaskVO> teachers;
}

package com.zzz.teachingevaluationsystembackend.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class StudentTaskVO {
    private Long id;
    private Long evaluationId;
    private String evaluationName;
    private Long courseId;
    private String courseNameCn;
    private String courseNameEn;
    private Long teacherId;
    private String teacherName;
    private String teacherType;
    private String status;
    private LocalDateTime completedAt;
}

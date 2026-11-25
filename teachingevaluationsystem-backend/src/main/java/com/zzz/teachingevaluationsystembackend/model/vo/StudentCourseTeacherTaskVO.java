package com.zzz.teachingevaluationsystembackend.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class StudentCourseTeacherTaskVO {
    private Long taskId;
    private Long teacherId;
    private String teacherName;
    private String teacherType;
    private String status;
    private LocalDateTime completedAt;
}

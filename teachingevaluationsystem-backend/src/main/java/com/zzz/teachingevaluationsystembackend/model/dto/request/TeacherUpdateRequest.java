package com.zzz.teachingevaluationsystembackend.model.dto.request;

import lombok.Data;

@Data
public class TeacherUpdateRequest {
    private String username;
    private String name;
    private String teacherType;
}

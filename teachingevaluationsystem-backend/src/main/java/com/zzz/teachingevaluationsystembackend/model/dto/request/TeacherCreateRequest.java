package com.zzz.teachingevaluationsystembackend.model.dto.request;

import lombok.Data;

@Data
public class TeacherCreateRequest {
    private String username;
    private String name;
    private String password;
    private String teacherType;
}

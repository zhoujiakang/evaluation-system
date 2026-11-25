package com.zzz.teachingevaluationsystembackend.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TeacherVO {
    private Long id;
    private Long userId;
    private String username;
    private String name;
    private String teacherType;
}

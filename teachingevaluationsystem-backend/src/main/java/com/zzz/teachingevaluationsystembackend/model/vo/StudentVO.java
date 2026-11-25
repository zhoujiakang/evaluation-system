package com.zzz.teachingevaluationsystembackend.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class StudentVO {
    private Long id;
    private Long userId;
    private String username;
    private String name;
    private String studentNo;
    private String className;
    private String major;
    private String grade;
}

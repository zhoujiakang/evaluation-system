package com.zzz.teachingevaluationsystembackend.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ClassVO {
    private Long id;
    private String className;
    private String major;
    private String grade;
}

package com.zzz.teachingevaluationsystembackend.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class EvaluationCourseImportResultVO {
    private int totalRows;
    private int successRows;
    private int skippedRows;
    private int createdCourses;
    private int linkedCourses;
    private int createdTeachers;
    private int linkedTeacherRelations;
    private int linkedClassRelations;
}

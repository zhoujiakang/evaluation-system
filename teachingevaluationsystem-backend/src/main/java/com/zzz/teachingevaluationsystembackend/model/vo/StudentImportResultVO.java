package com.zzz.teachingevaluationsystembackend.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class StudentImportResultVO {
    private int totalRows;
    private int successRows;
    private int skippedRows;
    private int createdUsers;
    private int createdStudents;
    private int updatedStudents;
    private int createdGrades;
    private int createdClasses;
}

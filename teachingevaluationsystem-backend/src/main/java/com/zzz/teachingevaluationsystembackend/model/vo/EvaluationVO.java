package com.zzz.teachingevaluationsystembackend.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class EvaluationVO {
    private Long id;
    private String name;
    private String academicYear;
    private String term;
    private String status;
    private LocalDateTime publishedAt;
    private LocalDateTime closedAt;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

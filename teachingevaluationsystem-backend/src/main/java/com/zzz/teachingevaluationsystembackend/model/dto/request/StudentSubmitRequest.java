package com.zzz.teachingevaluationsystembackend.model.dto.request;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class StudentSubmitRequest {
    private List<Item> scores;
    private String remark;

    @Data
    public static class Item {
        private Long indicatorId;
        private BigDecimal score;
    }
}

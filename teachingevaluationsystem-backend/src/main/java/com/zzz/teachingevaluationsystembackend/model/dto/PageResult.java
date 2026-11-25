package com.zzz.teachingevaluationsystembackend.model.dto;

import lombok.Data;

import java.util.List;

@Data
public class PageResult<T> {
    private long current;
    private long pages;
    private long total;
    private List<T> records;

    public PageResult(long current, long pages, long total, List<T> records) {
        this.current = current;
        this.pages = pages;
        this.total = total;
        this.records = records;
    }

    public PageResult(long total, List<T> records) {
        this.current = total > 0 ? 1 : 0;
        this.pages = total > 0 ? 1 : 0;
        this.total = total;
        this.records = records;
    }
}

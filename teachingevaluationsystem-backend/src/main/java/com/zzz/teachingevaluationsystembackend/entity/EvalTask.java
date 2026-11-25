package com.zzz.teachingevaluationsystembackend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("eval_task")
public class EvalTask {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long evaluationId;
    private Long studentId;
    private Long courseId;
    private Long teacherId;
    private String status;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

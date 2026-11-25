package com.zzz.teachingevaluationsystembackend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("score_submission")
public class ScoreSubmission {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long evaluationId;
    private Long taskId;
    private Long submitterUserId;
    private String submitMode;
    private LocalDateTime submittedAt;
    private String remark;
}

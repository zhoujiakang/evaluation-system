package com.zzz.teachingevaluationsystembackend.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("admin_permission")
public class AdminPermission {
    @TableId
    private Long userId;
    private Long grantedBy;
    private LocalDateTime grantedAt;
}

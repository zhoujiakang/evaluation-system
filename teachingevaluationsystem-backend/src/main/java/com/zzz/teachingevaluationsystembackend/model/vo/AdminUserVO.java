package com.zzz.teachingevaluationsystembackend.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class AdminUserVO {
    private Long userId;
    private String username;
    private String name;
    private String role;
    private Boolean isAdmin;
    private Long grantedBy;
    private LocalDateTime grantedAt;
}

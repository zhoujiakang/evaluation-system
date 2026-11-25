package com.zzz.teachingevaluationsystembackend.auth;

import cn.dev33.satoken.stp.StpLogic;

public final class StpKit {
    private StpKit() {
    }

    public static final StpLogic ADMIN = new StpLogic("admin");
    public static final StpLogic STUDENT = new StpLogic("student");
}

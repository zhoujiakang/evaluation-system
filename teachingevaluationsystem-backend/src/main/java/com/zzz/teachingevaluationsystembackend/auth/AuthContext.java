package com.zzz.teachingevaluationsystembackend.auth;

public final class AuthContext {

    public static Long currentAdminUserId() {
        StpKit.ADMIN.checkLogin();
        return Long.parseLong(String.valueOf(StpKit.ADMIN.getLoginId()));
    }

    public static Long currentStudentUserId() {
        StpKit.STUDENT.checkLogin();
        return Long.parseLong(String.valueOf(StpKit.STUDENT.getLoginId()));
    }
}

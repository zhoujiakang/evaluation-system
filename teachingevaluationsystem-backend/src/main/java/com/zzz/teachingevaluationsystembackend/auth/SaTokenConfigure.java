package com.zzz.teachingevaluationsystembackend.auth;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SaTokenConfigure implements WebMvcConfigurer {
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handler -> {
            SaRouter.match("/api/admin/**")
                    .notMatch("/api/admin/auth/login")
                    .check(r -> StpKit.ADMIN.checkLogin());

            SaRouter.match("/api/student/**")
                    .notMatch("/api/student/auth/login")
                    .check(r -> StpKit.STUDENT.checkLogin());
        })).addPathPatterns("/**");
    }
}

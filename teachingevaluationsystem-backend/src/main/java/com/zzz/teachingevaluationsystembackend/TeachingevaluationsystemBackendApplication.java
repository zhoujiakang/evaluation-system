package com.zzz.teachingevaluationsystembackend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.zzz.teachingevaluationsystembackend.mapper")
public class TeachingevaluationsystemBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(TeachingevaluationsystemBackendApplication.class, args);
    }

}

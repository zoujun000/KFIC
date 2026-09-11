package com.freight;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@MapperScan("com.freight.mapper")
public class FreightApplication {
    public static void main(String[] args) {
        SpringApplication.run(FreightApplication.class, args);
    }
}

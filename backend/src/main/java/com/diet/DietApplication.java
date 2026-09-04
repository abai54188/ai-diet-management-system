package com.diet;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * AI智能饮食管理系统 启动类
 *
 * @author diet
 */
@SpringBootApplication
@MapperScan("com.diet.mapper")
public class DietApplication {

    public static void main(String[] args) {
        SpringApplication.run(DietApplication.class, args);
    }
}
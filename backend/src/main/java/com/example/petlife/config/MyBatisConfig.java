package com.example.petlife.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.example.petlife.mapper")
public class MyBatisConfig {
}

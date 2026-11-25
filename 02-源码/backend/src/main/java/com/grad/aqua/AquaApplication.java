package com.grad.aqua;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 水产养殖批次与水质监测管理系统启动类。
 * 开启定时任务：水质超限扫描与休药期计算（见 WaterScanJob）。
 */
@EnableScheduling
@MapperScan("com.grad.aqua.mapper")
@SpringBootApplication
public class AquaApplication {

    public static void main(String[] args) {
        SpringApplication.run(AquaApplication.class, args);
    }
}

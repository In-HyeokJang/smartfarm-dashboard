package com.samintech.smartfarmdashboard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * SmartFarm 대시보드 Spring Boot 애플리케이션 진입점.
 *
 * <p>{@code @EnableScheduling}: {@link com.samintech.smartfarmdashboard.service.SerialService}의
 * 시뮬레이션 모드에서 3초마다 가짜 센서 데이터를 생성하는 {@code @Scheduled} 메서드를 활성화합니다.</p>
 *
 * @author Jay
 */
@SpringBootApplication
@EnableScheduling
public class SmartfarmDashboardApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartfarmDashboardApplication.class, args);
    }
}
package com.samintech.smartfarmdashboard.dto;

import com.samintech.smartfarmdashboard.domain.SensorData;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 대시보드 화면과 SSE(Server-Sent Events) 실시간 전송에 사용되는 데이터 전달 객체(DTO).
 *
 * <p>Java Record로 선언하여 불변(Immutable) 객체로 만들었습니다.
 * Record는 JDK 16+에서 정식 지원되며, Lombok 없이도 getter/equals/hashCode/toString을 자동 생성합니다.</p>
 *
 * <p>사용처:
 * <ul>
 *   <li>{@link com.samintech.smartfarmdashboard.controller.DashboardController}: 초기 페이지 렌더링 시 Model에 담아 Thymeleaf로 전달</li>
 *   <li>{@link com.samintech.smartfarmdashboard.service.SseService}: SSE 이벤트로 브라우저에 JSON 전송</li>
 * </ul>
 * </p>
 *
 * @param id           DB 기본키
 * @param soilRaw      Arduino 원시 ADC 값 (0 ~ 1023)
 * @param soilPercent  수분 퍼센트 (0 ~ 100)
 * @param pumpOn       펌프 상태 (true = ON)
 * @param pumpStatus   화면 표시용 펌프 상태 문자열 ("ON" / "OFF")
 * @param measuredAt   측정 시각 (LocalDateTime)
 * @param measuredAtFormatted 화면 표시용 포맷된 시각 문자열 (예: "14:32:05")
 *
 * @author Jay
 */
public record SensorDataDto(
        Long id,
        int soilRaw,
        int soilPercent,
        boolean pumpOn,
        String pumpStatus,
        LocalDateTime measuredAt,
        String measuredAtFormatted
) {
    /** 시각 포맷: HH:mm:ss (시:분:초) */
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    /**
     * {@link SensorData} 엔티티로부터 DTO를 생성하는 팩토리 메서드.
     *
     * @param entity DB에서 조회한 SensorData 엔티티
     * @return 변환된 SensorDataDto
     */
    public static SensorDataDto from(SensorData entity) {
        return new SensorDataDto(
                entity.getId(),
                entity.getSoilRaw(),
                entity.getSoilPercent(),
                entity.isPumpOn(),
                entity.isPumpOn() ? "ON" : "OFF",
                entity.getMeasuredAt(),
                entity.getMeasuredAt().format(TIME_FMT)
        );
    }

    /**
     * 데이터가 없을 때 대시보드 초기 화면에 표시할 기본값 DTO를 반환합니다.
     *
     * @return 모든 값이 0/false인 기본 DTO
     */
    public static SensorDataDto empty() {
        LocalDateTime now = LocalDateTime.now();
        return new SensorDataDto(null, 0, 0, false, "OFF", now, now.format(TIME_FMT));
    }
}
package com.samintech.smartfarmdashboard.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Arduino로부터 수신한 토양 수분 센서 데이터를 저장하는 JPA 엔티티.
 *
 * <p>데이터 흐름: Arduino(USB 시리얼) → SerialService(파싱) → SensorDataService → SensorData → Supabase DB</p>
 *
 * <p>Arduino는 "SOIL:850,PUMP:OFF" 형식의 문자열을 시리얼로 전송하며,
 * {@link com.samintech.smartfarmdashboard.service.SerialService}가 이를 파싱하여
 * {@code SensorData.of()} 팩토리 메서드로 엔티티를 생성합니다.</p>
 *
 * <p>수분 계산 원리:
 * <ul>
 *   <li>ADC 값이 낮을수록 → 저항 낮음 → 토양이 습함</li>
 *   <li>ADC 값이 높을수록 → 저항 높음 → 토양이 건조함</li>
 *   <li>따라서 soilPercent = 100 - (soilRaw / 1023.0 * 100) 으로 역산</li>
 * </ul>
 * </p>
 *
 * @author Jay
 */
@Entity
@Table(name = "sensor_data")
@Getter
@NoArgsConstructor
public class SensorData {

    /** DB 자동 증가 기본키 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Arduino 아날로그 핀(A0)의 원시 ADC 값.
     * 범위: 0(완전 습윤) ~ 1023(완전 건조)
     */
    @Column(nullable = false)
    private Integer soilRaw;

    /**
     * soilRaw를 사람이 읽기 쉬운 수분 퍼센트로 변환한 값.
     * 범위: 0%(건조) ~ 100%(습윤)
     */
    @Column(nullable = false)
    private Integer soilPercent;

    /**
     * 워터 펌프 상태.
     * true = 펌프 동작 중(ON), false = 펌프 정지(OFF)
     * primitive boolean: Lombok이 isPumpOn()을 생성함 (Boolean wrapper는 getPumpOn()을 생성)
     */
    @Column(nullable = false)
    private boolean pumpOn;

    /** 센서 측정 시각 (서버 수신 시각 기준) */
    @Column(nullable = false)
    private LocalDateTime measuredAt;

    /**
     * Arduino 시리얼 데이터로부터 SensorData 인스턴스를 생성하는 팩토리 메서드.
     *
     * <p>ADC 원시값을 수분 퍼센트로 변환하고, 현재 시각을 측정 시각으로 설정합니다.</p>
     *
     * @param soilRaw Arduino analogRead(A0) 값 (0 ~ 1023)
     * @param pumpOn  펌프 상태 (true = ON, false = OFF)
     * @return 저장 준비된 SensorData 인스턴스
     */
    public static SensorData of(int soilRaw, boolean pumpOn) {
        SensorData data = new SensorData();
        data.soilRaw = soilRaw;
        // 역산: ADC 값이 클수록 건조 → 퍼센트는 낮아야 함
        // clamp(0, 100) 으로 센서 오차 범위 밖 값 방어
        data.soilPercent = Math.max(0, Math.min(100, 100 - (soilRaw * 100 / 1023)));
        data.pumpOn = pumpOn;
        data.measuredAt = LocalDateTime.now();
        return data;
    }
}
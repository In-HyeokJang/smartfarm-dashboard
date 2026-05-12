package com.samintech.smartfarmdashboard.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * {@link SensorData} 엔티티의 CRUD 및 조회를 담당하는 Spring Data JPA 리포지토리.
 *
 * <p>인터페이스만 선언하면 Spring이 런타임에 구현체를 자동 생성합니다.
 * 메서드 이름 규칙(findTopBy, findTop20By 등)으로 JPQL 없이 쿼리를 정의합니다.</p>
 *
 * @author Jay
 */
public interface SensorRepository extends JpaRepository<SensorData, Long> {

    /**
     * 가장 최근에 저장된 센서 데이터 1건을 조회합니다.
     *
     * <p>대시보드 상단 카드(현재 수분, 펌프 상태)에 표시하는 용도입니다.</p>
     *
     * @return 최신 SensorData (데이터가 없으면 Optional.empty())
     */
    Optional<SensorData> findTopByOrderByMeasuredAtDesc();

    /**
     * 최근 20건의 센서 데이터를 측정 시각 내림차순으로 조회합니다.
     *
     * <p>대시보드 수분 추이 차트(Chart.js)에 사용됩니다.
     * 20건이면 약 1분(3초 간격 기준)의 히스토리를 표시합니다.</p>
     *
     * @return 최근 20건 리스트 (최신 데이터가 인덱스 0)
     */
    List<SensorData> findTop20ByOrderByMeasuredAtDesc();
}
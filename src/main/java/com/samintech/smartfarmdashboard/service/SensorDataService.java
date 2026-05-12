package com.samintech.smartfarmdashboard.service;

import com.samintech.smartfarmdashboard.domain.SensorData;
import com.samintech.smartfarmdashboard.domain.SensorRepository;
import com.samintech.smartfarmdashboard.dto.SensorDataDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 센서 데이터의 저장 및 조회 비즈니스 로직을 담당하는 서비스.
 *
 * <p>데이터 흐름:
 * <pre>
 *   SerialService (데이터 수신/파싱)
 *        ↓
 *   SensorDataService (비즈니스 로직, DB 저장)
 *        ↓
 *   SensorRepository → Supabase PostgreSQL
 * </pre>
 * </p>
 *
 * @author Jay
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SensorDataService {

    private final SensorRepository sensorRepository;

    /**
     * 새 센서 데이터를 DB에 저장하고 DTO로 변환하여 반환합니다.
     *
     * <p>{@code @Transactional}로 감싸져 있어 저장 실패 시 롤백됩니다.</p>
     *
     * @param soilRaw Arduino analogRead(A0) 원시값 (0 ~ 1023)
     * @param pumpOn  펌프 현재 상태 (true = ON)
     * @return 저장 완료된 데이터의 DTO
     */
    @Transactional
    public SensorDataDto save(int soilRaw, boolean pumpOn) {
        SensorData saved = sensorRepository.save(SensorData.of(soilRaw, pumpOn));
        log.debug("센서 데이터 저장 완료 - soilRaw: {}, soilPercent: {}%, pumpOn: {}",
                saved.getSoilRaw(), saved.getSoilPercent(), saved.isPumpOn());
        return SensorDataDto.from(saved);
    }

    /**
     * 가장 최근에 저장된 센서 데이터 1건을 조회합니다.
     *
     * <p>대시보드 초기 로딩 시 현재 상태 카드에 표시할 데이터를 가져옵니다.
     * DB에 데이터가 없는 경우 빈 DTO({@link SensorDataDto#empty()})를 반환합니다.</p>
     *
     * @return 최신 센서 데이터 DTO, 없으면 empty DTO
     */
    @Transactional(readOnly = true)
    public SensorDataDto getLatest() {
        return sensorRepository.findTopByOrderByMeasuredAtDesc()
                .map(SensorDataDto::from)
                .orElse(SensorDataDto.empty());
    }

    /**
     * 최근 20건의 센서 데이터를 조회하여 차트용 DTO 리스트로 반환합니다.
     *
     * <p>최신 데이터가 리스트 앞에 오므로, 차트에서는 역순으로 표시해야 시간 순서대로 보입니다.
     * (JavaScript에서 {@code .reverse()} 처리)</p>
     *
     * @return 최근 20건의 센서 데이터 DTO 리스트 (최신 순)
     */
    @Transactional(readOnly = true)
    public List<SensorDataDto> getHistory() {
        return sensorRepository.findTop20ByOrderByMeasuredAtDesc()
                .stream()
                .map(SensorDataDto::from)
                .toList();
    }
}
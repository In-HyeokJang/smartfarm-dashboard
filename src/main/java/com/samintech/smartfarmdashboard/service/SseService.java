package com.samintech.smartfarmdashboard.service;

import com.samintech.smartfarmdashboard.dto.SensorDataDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * SSE(Server-Sent Events) 에미터를 관리하는 서비스.
 *
 * <p>SSE는 서버에서 브라우저로 데이터를 단방향으로 실시간 푸시하는 HTTP 표준 기술입니다.
 * WebSocket보다 단순하며, 센서 데이터 표시처럼 서버→클라이언트 방향만 필요한 경우 적합합니다.</p>
 *
 * <p>JSON 직렬화: {@code emitter.send(dto)} 호출 시 Spring MVC의 Jackson 메시지 컨버터가
 * {@link SensorDataDto} 레코드를 자동으로 JSON 문자열로 변환합니다.
 * 별도의 ObjectMapper 주입이 필요 없습니다.</p>
 *
 * <p>동작 방식:
 * <ol>
 *   <li>브라우저가 {@code GET /api/sse} 요청 → {@link com.samintech.smartfarmdashboard.controller.SseController}가 에미터 생성 후 이 서비스에 등록</li>
 *   <li>새 센서 데이터 수신 시 {@link SerialService}가 {@code broadcast()}를 호출</li>
 *   <li>이 서비스가 등록된 모든 브라우저 연결에 JSON 이벤트를 전송</li>
 * </ol>
 * </p>
 *
 * @author Jay
 */
@Slf4j
@Service
public class SseService {

    /**
     * 현재 연결된 SSE 에미터 목록.
     * {@link CopyOnWriteArrayList}를 사용하여 멀티스레드 환경(시리얼 수신 스레드 + HTTP 요청 스레드)에서
     * 동시 수정/읽기 시 발생하는 ConcurrentModificationException을 방지합니다.
     */
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    /**
     * 새 SSE 에미터를 생성하고 목록에 등록합니다.
     *
     * <p>에미터 타임아웃은 5분으로 설정합니다.
     * 타임아웃·완료·에러 발생 시 자동으로 목록에서 제거됩니다.
     * 브라우저(클라이언트)는 연결이 끊기면 자동으로 재연결을 시도합니다.</p>
     *
     * @return 생성된 SseEmitter (컨트롤러가 HTTP 응답으로 반환)
     */
    public SseEmitter createEmitter() {
        SseEmitter emitter = new SseEmitter(5 * 60 * 1000L);
        emitters.add(emitter);

        // 연결 종료 시 목록에서 제거 (메모리 누수 방지)
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(e -> emitters.remove(emitter));

        log.debug("SSE 에미터 생성. 현재 연결 수: {}", emitters.size());
        return emitter;
    }

    /**
     * 등록된 모든 브라우저 연결에 센서 데이터를 SSE 이벤트로 전송합니다.
     *
     * <p>이벤트명은 "sensor"로 전송되며, 브라우저 JavaScript에서
     * {@code evtSource.addEventListener('sensor', ...)} 로 수신합니다.</p>
     *
     * <p>{@link SensorDataDto}는 Java Record이며, Spring MVC의 Jackson 컨버터가
     * 자동으로 JSON으로 변환합니다. 별도 직렬화 코드 불필요.</p>
     *
     * <p>전송 실패한 에미터(연결 끊김 등)는 즉시 목록에서 제거합니다.</p>
     *
     * @param data 브라우저로 전송할 센서 데이터 DTO
     */
    public void broadcast(SensorDataDto data) {
        if (emitters.isEmpty()) return;

        // 전송 실패한 에미터를 별도 수집 후 제거 (반복 중 목록 수정 방지)
        List<SseEmitter> deadEmitters = new CopyOnWriteArrayList<>();

        for (SseEmitter emitter : emitters) {
            try {
                // Spring MVC가 SensorDataDto를 Jackson으로 자동 JSON 직렬화하여 전송
                // 브라우저에서 addEventListener('sensor', e => JSON.parse(e.data)) 로 수신
                emitter.send(SseEmitter.event()
                        .name("sensor")
                        .data(data));
            } catch (IOException e) {
                // 클라이언트 연결 끊김 → 다음 정리 대상으로 표시
                deadEmitters.add(emitter);
            }
        }

        emitters.removeAll(deadEmitters);
    }
}
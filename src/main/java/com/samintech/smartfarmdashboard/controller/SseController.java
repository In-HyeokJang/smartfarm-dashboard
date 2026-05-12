package com.samintech.smartfarmdashboard.controller;

import com.samintech.smartfarmdashboard.service.SseService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * SSE(Server-Sent Events) 연결 엔드포인트 컨트롤러.
 *
 * <p>브라우저가 {@code GET /api/sse}를 호출하면 SSE 연결이 수립되고,
 * 이후 새 센서 데이터가 들어올 때마다 서버에서 브라우저로 자동으로 데이터가 전송됩니다.
 * 브라우저는 페이지를 새로고침 없이 실시간 데이터를 받을 수 있습니다.</p>
 *
 * <p>JavaScript 사용 예시:
 * <pre>{@code
 *   const evtSource = new EventSource('/api/sse');
 *   evtSource.addEventListener('sensor', (e) => {
 *       const data = JSON.parse(e.data);
 *       console.log('수분:', data.soilPercent);
 *   });
 * }</pre>
 * </p>
 *
 * @author Jay
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SseController {

    private final SseService sseService;

    /**
     * SSE 연결을 수립하고 에미터를 반환합니다.
     *
     * <p>브라우저가 이 URL에 접속하면 HTTP 연결이 유지되며(Keep-Alive),
     * 서버는 새 데이터가 생길 때마다 이 연결로 데이터를 푸시합니다.</p>
     *
     * <p>Content-Type은 Spring이 자동으로 {@code text/event-stream}으로 설정합니다.</p>
     *
     * @return SSE 에미터 (Spring이 응답 스트림으로 처리)
     */
    @GetMapping("/sse")
    public SseEmitter connect() {
        return sseService.createEmitter();
    }
}
package com.samintech.smartfarmdashboard.controller;

import com.samintech.smartfarmdashboard.service.SerialService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 워터 펌프 수동 제어 REST API 컨트롤러.
 *
 * <p>대시보드의 "펌프 ON" / "펌프 OFF" 버튼을 클릭하면
 * JavaScript {@code fetch()} 로 이 API를 호출합니다.
 * 그러면 Arduino로 "PUMP_ON" 또는 "PUMP_OFF" 명령 문자열을 시리얼로 전송합니다.</p>
 *
 * <p>Arduino 스케치에서 명령 수신 예시:
 * <pre>
 *   if (Serial.available()) {
 *     String cmd = Serial.readStringUntil('\n');
 *     cmd.trim();
 *     if (cmd == "PUMP_ON")  { digitalWrite(RELAY_PIN, HIGH); }
 *     if (cmd == "PUMP_OFF") { digitalWrite(RELAY_PIN, LOW);  }
 *   }
 * </pre>
 * </p>
 *
 * @author Jay
 */
@RestController
@RequestMapping("/api/pump")
@RequiredArgsConstructor
public class PumpController {

    private final SerialService serialService;

    /**
     * 워터 펌프를 켭니다.
     *
     * <p>Arduino로 "PUMP_ON\n" 문자열을 시리얼 포트로 전송합니다.
     * 시뮬레이션 모드에서는 전송 없이 성공 응답만 반환합니다.</p>
     *
     * @return HTTP 200 OK 와 결과 메시지 JSON
     */
    @PostMapping("/on")
    public ResponseEntity<Map<String, String>> pumpOn() {
        serialService.sendCommand("PUMP_ON");
        return ResponseEntity.ok(Map.of("status", "OK", "message", "펌프를 켰습니다."));
    }

    /**
     * 워터 펌프를 끕니다.
     *
     * <p>Arduino로 "PUMP_OFF\n" 문자열을 시리얼 포트로 전송합니다.
     * 시뮬레이션 모드에서는 전송 없이 성공 응답만 반환합니다.</p>
     *
     * @return HTTP 200 OK 와 결과 메시지 JSON
     */
    @PostMapping("/off")
    public ResponseEntity<Map<String, String>> pumpOff() {
        serialService.sendCommand("PUMP_OFF");
        return ResponseEntity.ok(Map.of("status", "OK", "message", "펌프를 껐습니다."));
    }
}
package com.samintech.smartfarmdashboard.service;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;
import com.samintech.smartfarmdashboard.dto.SensorDataDto;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Random;

/**
 * Arduino와의 USB 시리얼 통신을 담당하는 핵심 서비스.
 *
 * <p><b>실제 Arduino 연결 모드</b> ({@code serial.enabled=true}):
 * <pre>
 *   Arduino IDE에서 업로드한 스케치가 아래 형식으로 2~3초마다 데이터를 전송합니다:
 *
 *     Serial.println("SOIL:" + analogRead(A0) + ",PUMP:" + pumpState);
 *
 *   예시 수신 문자열: "SOIL:850,PUMP:OFF"
 *
 *   이 서비스가 COM 포트를 열고 해당 문자열을 읽어 파싱합니다.
 * </pre>
 * </p>
 *
 * <p><b>시뮬레이션 모드</b> ({@code serial.enabled=false}):
 * Arduino 없이 개발/테스트할 때 사용합니다.
 * 3초마다 랜덤 가짜 데이터를 생성하여 실제 데이터처럼 처리합니다.
 * </p>
 *
 * <p><b>Arduino → Java 연결 원리</b>:
 * Arduino를 USB로 PC에 연결하면 Windows에서 COM 포트(COM3, COM4 등)가 생성됩니다.
 * Arduino IDE → Tools → Port 메뉴에서 어떤 COM 포트인지 확인할 수 있습니다.
 * 이 COM 포트를 jSerialComm 라이브러리로 열어 Arduino가 보내는 문자열을 읽습니다.
 * </p>
 *
 * <p><b>Arduino에서 명령 수신</b>:
 * 펌프 ON/OFF 명령을 Arduino로 역방향 전송할 수 있습니다.
 * Arduino 스케치에서 {@code Serial.readStringUntil('\n')} 으로 "PUMP_ON" 또는 "PUMP_OFF"를 수신하면 됩니다.
 * </p>
 *
 * @author Jay
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SerialService {

    private final SensorDataService sensorDataService;
    private final SseService sseService;

    /** application.yaml의 serial.port 값 (예: COM3, /dev/ttyUSB0) */
    @Value("${serial.port}")
    private String portName;

    /** application.yaml의 serial.baud-rate 값. Arduino Serial.begin() 값과 일치해야 함 */
    @Value("${serial.baud-rate}")
    private int baudRate;

    /** application.yaml의 serial.enabled 값. false이면 시뮬레이션 모드로 동작 */
    @Value("${serial.enabled}")
    private boolean serialEnabled;

    /** jSerialComm 시리얼 포트 인스턴스 (앱 종료 시 닫기 위해 필드로 보관) */
    private SerialPort serialPort;

    /**
     * 수신 버퍼: Arduino 데이터는 개행(\n)으로 끝나는 문자열이므로
     * 여러 번 분할 수신될 수 있어 StringBuilder로 조립합니다.
     */
    private final StringBuilder receiveBuffer = new StringBuilder();

    /** 시뮬레이션 모드에서 랜덤 데이터 생성용 */
    private final Random random = new Random();

    /**
     * 앱 시작 직후 시리얼 포트를 초기화합니다.
     *
     * <p>{@code serial.enabled=true}이면 실제 COM 포트를 열고,
     * {@code false}이면 시뮬레이션 모드 메시지를 로그에 출력합니다.</p>
     */
    @PostConstruct
    public void init() {
        if (serialEnabled) {
            openSerialPort();
        } else {
            log.info("===== 시리얼 통신 비활성화 - 시뮬레이션 모드로 동작합니다 =====");
        }
    }

    /**
     * 앱 종료 전 시리얼 포트를 안전하게 닫습니다.
     *
     * <p>포트를 닫지 않으면 다음 앱 시작 시 "포트가 이미 사용 중" 오류가 발생합니다.</p>
     */
    @PreDestroy
    public void cleanup() {
        if (serialPort != null && serialPort.isOpen()) {
            serialPort.closePort();
            log.info("시리얼 포트 {} 닫음", portName);
        }
    }

    /**
     * jSerialComm을 사용하여 COM 포트를 열고 데이터 리스너를 등록합니다.
     *
     * <p>포트를 열지 못하면 오류 로그를 출력하고 시뮬레이션 모드로 전환합니다.</p>
     */
    private void openSerialPort() {
        serialPort = SerialPort.getCommPort(portName);
        serialPort.setBaudRate(baudRate);
        // 데이터가 수신될 때마다 콜백 실행
        serialPort.addDataListener(new SerialPortDataListener() {
            @Override
            public int getListeningEvents() {
                // 수신 데이터 있을 때만 이벤트 발생
                return SerialPort.LISTENING_EVENT_DATA_AVAILABLE;
            }

            @Override
            public void serialEvent(SerialPortEvent event) {
                byte[] bytes = new byte[serialPort.bytesAvailable()];
                serialPort.readBytes(bytes, bytes.length);
                // 수신 바이트를 문자열로 변환하여 버퍼에 추가
                receiveBuffer.append(new String(bytes, StandardCharsets.UTF_8));
                processBuffer();
            }
        });

        if (serialPort.openPort()) {
            log.info("시리얼 포트 {} 연결 성공 ({}bps)", portName, baudRate);
        } else {
            log.error("시리얼 포트 {} 연결 실패 → 시뮬레이션 모드로 전환", portName);
            serialEnabled = false;
        }
    }

    /**
     * 수신 버퍼에서 개행(\n)으로 끝나는 완성된 메시지를 파싱합니다.
     *
     * <p>Arduino 데이터가 한 번에 오지 않고 여러 번에 나눠 올 수 있으므로,
     * 개행 문자를 기준으로 완성된 라인만 처리합니다.</p>
     */
    private void processBuffer() {
        String buffer = receiveBuffer.toString();
        int newlineIdx;
        // 개행 문자가 있는 만큼 반복 처리
        while ((newlineIdx = buffer.indexOf('\n')) != -1) {
            String line = buffer.substring(0, newlineIdx).trim();
            buffer = buffer.substring(newlineIdx + 1);
            if (!line.isEmpty()) {
                parseLine(line);
            }
        }
        // 처리된 부분 제거, 나머지는 다음 수신까지 보관
        receiveBuffer.setLength(0);
        receiveBuffer.append(buffer);
    }

    /**
     * Arduino 데이터 한 줄을 파싱하여 DB에 저장하고 SSE로 브라우저에 전송합니다.
     *
     * <p>기대 형식: {@code "SOIL:850,PUMP:OFF"}</p>
     * <p>형식 오류 시 오류 로그를 남기고 해당 줄은 무시합니다.</p>
     *
     * @param line Arduino로부터 수신한 한 줄 문자열
     */
    private void parseLine(String line) {
        try {
            // "SOIL:850,PUMP:OFF" → ["SOIL:850", "PUMP:OFF"]
            String[] parts = line.split(",");
            if (parts.length != 2) {
                log.warn("파싱 실패 - 예상 형식: 'SOIL:xxx,PUMP:ON/OFF', 수신: '{}'", line);
                return;
            }

            // "SOIL:850" → soilRaw = 850
            int soilRaw = Integer.parseInt(parts[0].split(":")[1].trim());
            // "PUMP:OFF" → pumpOn = false
            boolean pumpOn = "ON".equalsIgnoreCase(parts[1].split(":")[1].trim());

            processIncomingData(soilRaw, pumpOn);

        } catch (Exception e) {
            log.warn("Arduino 데이터 파싱 오류 - 수신: '{}', 원인: {}", line, e.getMessage());
        }
    }

    /**
     * 파싱된 센서 값을 DB에 저장하고 SSE로 실시간 브로드캐스트합니다.
     *
     * <p>실제 Arduino 데이터와 시뮬레이션 데이터 모두 이 메서드를 통해 처리됩니다.</p>
     *
     * @param soilRaw Arduino ADC 원시값
     * @param pumpOn  펌프 상태
     */
    private void processIncomingData(int soilRaw, boolean pumpOn) {
        SensorDataDto dto = sensorDataService.save(soilRaw, pumpOn);
        // 연결된 모든 브라우저에 실시간 전송
        sseService.broadcast(dto);
        log.debug("데이터 처리 완료 - 수분: {}%, 펌프: {}", dto.soilPercent(), pumpOn ? "ON" : "OFF");
    }

    /**
     * 시뮬레이션 모드에서 3초마다 가짜 센서 데이터를 생성합니다.
     *
     * <p>{@code serial.enabled=false}일 때만 동작합니다.
     * Arduino가 없어도 대시보드와 실시간 업데이트를 테스트할 수 있습니다.</p>
     *
     * <p>시뮬레이션 규칙:
     * <ul>
     *   <li>soilRaw: 200~900 사이 랜덤 (적당히 습한~건조한 범위)</li>
     *   <li>pumpOn: soilRaw가 700 이상이면 자동으로 ON (건조 시 펌프 작동)</li>
     * </ul>
     * </p>
     */
    @Scheduled(fixedDelay = 3000)
    public void simulateIfDisabled() {
        if (serialEnabled) return; // 실제 Arduino 연결 시 이 메서드는 아무것도 하지 않음

        // 200(습함) ~ 900(건조) 범위의 랜덤 ADC 값 생성
        int fakeRaw = 200 + random.nextInt(701);
        // ADC >= 700 → 건조 상태 → 펌프 자동 ON
        boolean fakePump = fakeRaw >= 700;

        processIncomingData(fakeRaw, fakePump);
    }

    /**
     * Arduino로 펌프 제어 명령을 전송합니다.
     *
     * <p>Arduino 스케치에서 {@code Serial.readStringUntil('\n')} 으로 수신할 수 있습니다.</p>
     *
     * @param command 전송할 명령 문자열 ("PUMP_ON" 또는 "PUMP_OFF")
     */
    public void sendCommand(String command) {
        if (!serialEnabled || serialPort == null || !serialPort.isOpen()) {
            log.info("시뮬레이션 모드 - 명령 전송 생략: {}", command);
            return;
        }
        byte[] bytes = (command + "\n").getBytes(StandardCharsets.UTF_8);
        serialPort.writeBytes(bytes, bytes.length);
        log.info("Arduino 명령 전송: {}", command);
    }
}
# 🌿 방구석 IoT 메이커스: 스마트팜 자동 급수 시스템 (Jay's Smart Farm)

> **"한계를 뛰어넘는 개발 역량 성장, 자신감, 그리고 시야의 확장은, 모니터 안의 코드에만 머물던 SW 개발자가 낯선 하드웨어를 직접 연결하고 현실의 기계를 제어해 볼 때 일어나게 된다."**

## 📌 프로젝트 개요
본 프로젝트는 Arduino와 Spring Boot를 결합하여 화분의 토양 수분을 실시간으로 모니터링하고, 필요 시 자동으로 물을 공급하는 **Smart Farm IoT 시스템**입니다. 4년 차 Java/Spring Boot 개발자로서 하드웨어 제어 능력을 확장하고, 데이터의 End-to-End 흐름을 직접 구현하는 것을 목표로 합니다.

---

## 🛠 Tech Stack
- **Language:** Java 21 (LTS)
- **Framework:** Spring Boot 3.2+
- **Database:** H2 Database (Local/In-memory)
- **View Engine:** Thymeleaf
- **Communication:** Serial Communication (via `jSerialComm`)
- **Hardware:** Arduino Uno, 토양 수분 센서, 5V 릴레이 모듈, 워터 펌프

---

## 🏗 시스템 아키텍처
1. **Sensors (Arduino):** 토양 수분 데이터를 측정하고 USB 시리얼 포트로 데이터를 전송합니다.
2. **Backend (Spring Boot):** 시리얼 포트를 리스닝하여 데이터를 파싱하고 DB에 저장합니다.
3. **Dashboard (Web):** 사용자에게 실시간 수분 상태를 시각화하여 보여주고 펌프 제어 명령을 내립니다.

---

## 📋 로컬 개발 로드맵 (Local Development Guide)

### 1단계: 프로젝트 기본 설정
- [x] Java 21 및 IntelliJ IDEA 프로젝트 생성
- [x] `application.yml` 설정 (properties에서 변환)
- [x] `build.gradle` 종속성 추가 (`jSerialComm`, `Lombok`, `JPA`, `H2`)

### 2단계: 데이터베이스 설계 (Domain)
- [ ] `SensorData`: 측정 시간, 수분 값, 펌프 상태를 저장하는 엔티티 구현
- [ ] `SensorRepository`: 데이터 저장 및 최근 기록 조회를 위한 인터페이스

### 3단계: 시리얼 통신 서비스 (Core)
- [ ] `SerialService`: 아두이노 포트 연결 및 실시간 데이터 스트림 수신 로직 구현
- [ ] 데이터 파싱 로직 (예: `SOIL:1023, PUMP:ON` -> 객체화)

### 4단계: 웹 대시보드 (View/Controller)
- [ ] `DashboardController`: 메인 페이지 매핑
- [ ] `index.html`: Thymeleaf를 활용한 수분량 및 상태 표시 화면 구성

---

## ⚙️ 로컬 실행 방법 (Local Setup)

### Prerequisites
- Arduino Uno가 PC와 USB로 연결되어 있어야 합니다.
- 아두이노에 `smartfarm.ino` 코드가 업로드된 상태여야 합니다.

### Configuration (`application.yml`)
```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb
    driverClassName: org.h2.Driver
    username: sa
    password:
  h2:
    console:
      enabled: true
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
```
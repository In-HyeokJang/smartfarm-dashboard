# 🌿 방구석 IoT 메이커스: 스마트팜 자동 급수 시스템

> **"한계를 뛰어넘는 개발 역량 성장, 자신감, 그리고 시야의 확장은, 모니터 안의 코드에만 머물던 SW 개발자가 낯선 하드웨어를 직접 연결하고 현실의 기계를 제어해 볼 때 일어나게 된다."**

## 📌 프로젝트 개요

Arduino와 Spring Boot를 결합하여 화분의 토양 수분을 실시간으로 모니터링하고, 필요 시 자동으로 물을 공급하는 **Smart Farm IoT 시스템**입니다.

4년 차 Java/Spring Boot 개발자로서 하드웨어 제어 능력을 확장하고, 센서 → 백엔드 → 대시보드까지 데이터의 End-to-End 흐름을 직접 구현하는 것을 목표로 합니다.

---

## 🛠 Tech Stack

| 분류 | 기술 |
|------|------|
| Language | Java 21 (LTS) |
| Framework | Spring Boot 4.0.6 (Spring MVC, Spring Security 7) |
| Database | Neon Serverless PostgreSQL (무료 호스팅) |
| ORM | Spring Data JPA + Hibernate 7 |
| View | Thymeleaf + Bootstrap 5.3 + Chart.js 4.x |
| 실시간 통신 | SSE (Server-Sent Events) |
| 하드웨어 통신 | jSerialComm 2.10.4 (Arduino USB 시리얼) |
| Hardware | Arduino Uno, 토양 수분 센서, 5V 릴레이 모듈, 워터 펌프 |

---

## 🏗 시스템 아키텍처

```
[Arduino]
  토양 수분 센서 → USB 시리얼(COM 포트)
       ↓
[Spring Boot Backend]
  SerialService → SensorDataService → Neon PostgreSQL
       ↓
  SseService → SSE 이벤트 푸시
       ↓
[Web Dashboard]
  Chart.js 실시간 그래프 + 펌프 수동 제어
       ↓ (명령)
  PumpController → SerialService → Arduino 릴레이 제어
```

---

## 📁 프로젝트 구조

```
src/main/java/com/samintech/smartfarmdashboard/
├── config/
│   ├── SecurityConfig.java          # Spring Security 설정 (로그인/로그아웃)
│   ├── DataInitializer.java         # 최초 실행 시 관리자 계정 DB 시드
│   └── DotenvEnvironmentPostProcessor.java  # .env 파일 자동 로드
├── controller/
│   ├── DashboardController.java     # 대시보드 페이지
│   ├── SseController.java           # GET /api/sse (실시간 데이터 스트림)
│   └── PumpController.java          # POST /api/pump/on|off
├── domain/
│   ├── SensorData.java              # 센서 데이터 JPA 엔티티
│   ├── SensorRepository.java
│   ├── AdminUser.java               # 관리자 계정 JPA 엔티티
│   └── AdminUserRepository.java
├── dto/
│   └── SensorDataDto.java           # Java Record (SSE 전송용)
└── service/
    ├── SerialService.java           # Arduino 시리얼 통신 + 시뮬레이션 모드
    ├── SensorDataService.java       # DB 저장/조회
    ├── SseService.java              # SSE 에미터 관리 + 브로드캐스트
    └── AdminUserDetailsService.java # Spring Security 인증
```

---

## ⚙️ 로컬 실행 방법

### 1. 사전 준비

- Java 21 이상
- Neon 계정 및 프로젝트 생성 ([neon.tech](https://neon.tech) 무료)

### 2. 환경변수 설정

프로젝트 루트에 `.env` 파일 생성 (`.env.example` 참고):

```properties
# Neon → Dashboard → Connection string → JDBC
NEON_JDBC_URL=jdbc:postgresql://ep-xxxx.region.aws.neon.tech/neondb?sslmode=require
NEON_USER=neondb_owner
NEON_PASSWORD=your_password

# 관리자 로그인 계정
ADMIN_USERNAME=admin
ADMIN_PASSWORD=your_admin_password

# Arduino 포트 (false: 시뮬레이션 모드로 실행)
SERIAL_PORT=COM3
SERIAL_ENABLED=false
```

### 3. IntelliJ 프로필 설정

Run Configuration → **Active profiles**: `local`

### 4. 실행

```bash
./gradlew bootRun
```

브라우저에서 `http://localhost:7070` 접속 후 `.env`에 설정한 관리자 계정으로 로그인

---

## 🔌 Arduino 연동

Arduino 없이도 **시뮬레이션 모드**로 동작합니다 (`SERIAL_ENABLED=false`).
3초마다 랜덤 토양 수분 데이터가 자동 생성되어 대시보드에 표시됩니다.

실제 Arduino 연결 시:
1. `.env`에서 `SERIAL_ENABLED=true`, `SERIAL_PORT=COM3` (포트 확인 후 수정)
2. Arduino에 아래 형식으로 데이터 전송하도록 스케치 업로드:
   ```
   SOIL:850,PUMP:OFF
   ```
3. 서버 → Arduino 명령 형식: `PUMP_ON` 또는 `PUMP_OFF`

---

## 🚀 운영 배포

WAR 빌드:
```bash
./gradlew bootWar
```

배포 시 환경변수 설정:
```bash
export SPRING_PROFILES_ACTIVE=prod
```

prod 프로필 적용 시: `ddl-auto: validate`, 쿼리 로그 비활성화, Thymeleaf 캐시 활성화

---

## 🔐 보안 설계

- DB 접속 정보, 관리자 계정 → `.env` 파일 (`.gitignore` 등록, GitHub 미업로드)
- 관리자 비밀번호 → BCrypt 해시로 DB 저장
- CSRF 토큰 → `<meta>` 태그 방식으로 fetch() 요청에 포함
- 모든 URL → Spring Security 인증 필수 (`/login` 제외)
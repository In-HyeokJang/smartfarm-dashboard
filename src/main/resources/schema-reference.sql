-- ============================================================
-- schema-reference.sql
-- 참고용 스키마 문서 (실제 테이블 생성은 JPA ddl-auto: update 가 자동 처리)
-- 이 파일을 직접 실행할 필요는 없습니다.
-- ============================================================

-- ── 관리자 계정 테이블 ─────────────────────────────────────────
-- AdminUser 엔티티에 매핑됨
-- 앱 최초 실행 시 DataInitializer가 .env의 초기 관리자 계정을 INSERT
CREATE TABLE IF NOT EXISTS admin_users (
    id          BIGSERIAL PRIMARY KEY,
    username    VARCHAR(50)  NOT NULL UNIQUE,    -- 로그인 아이디 (중복 불가)
    password    VARCHAR(255) NOT NULL,           -- BCrypt 해시값 (평문 저장 금지!)
    role        VARCHAR(20)  NOT NULL DEFAULT 'ADMIN',
    enabled     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP    NOT NULL
);

-- ── 센서 데이터 테이블 ─────────────────────────────────────────
-- SensorData 엔티티에 매핑됨
-- SerialService가 Arduino 데이터 수신 시마다 INSERT
CREATE TABLE IF NOT EXISTS sensor_data (
    id             BIGSERIAL PRIMARY KEY,
    soil_raw       INTEGER   NOT NULL,  -- Arduino ADC 원시값 (0~1023)
    soil_percent   INTEGER   NOT NULL,  -- 수분 퍼센트 (0~100%)
    pump_on        BOOLEAN   NOT NULL,  -- 펌프 상태 (true=ON)
    measured_at    TIMESTAMP NOT NULL   -- 서버 수신 시각
);

-- ── 인덱스 (선택사항, 성능 개선용) ─────────────────────────────
-- 최신 데이터 조회(findTopByOrderByMeasuredAtDesc)에 자주 사용
CREATE INDEX IF NOT EXISTS idx_sensor_data_measured_at ON sensor_data (measured_at DESC);

-- ============================================================
-- 비밀번호 변경 방법 (Supabase Table Editor 또는 SQL Editor에서 실행)
-- BCrypt 해시 생성: https://bcrypt-generator.com (rounds=10)
-- ============================================================
-- UPDATE admin_users
-- SET password = '$2a$10$해시값을_여기에_입력'
-- WHERE username = 'admin';
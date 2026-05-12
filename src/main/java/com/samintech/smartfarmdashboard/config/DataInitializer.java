package com.samintech.smartfarmdashboard.config;

import com.samintech.smartfarmdashboard.domain.AdminUser;
import com.samintech.smartfarmdashboard.domain.AdminUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 애플리케이션 최초 실행 시 필요한 초기 데이터를 DB에 생성하는 클래스.
 *
 * <p>{@link ApplicationRunner}를 구현하면 Spring Boot가 완전히 시작된 직후,
 * DB 연결과 모든 빈이 준비된 상태에서 {@link #run} 메서드를 1회 실행합니다.</p>
 *
 * <p>관리자 계정 초기화 규칙:
 * <ol>
 *   <li>앱 시작 시 {@code admin_users} 테이블에서 {@code .env}의 ADMIN_USERNAME 존재 여부 확인</li>
 *   <li>없으면: .env의 ADMIN_PASSWORD를 BCrypt로 해싱하여 DB에 저장 (최초 1회)</li>
 *   <li>있으면: 아무 작업도 하지 않음 (재시작해도 기존 계정/비밀번호 유지)</li>
 * </ol>
 * </p>
 *
 * <p>비밀번호 변경 방법:
 * <ul>
 *   <li>Supabase 대시보드 → Table Editor → admin_users 테이블 직접 수정</li>
 *   <li>단, 비밀번호 컬럼에는 반드시 BCrypt 해시값을 입력해야 함 (평문 금지)</li>
 *   <li>BCrypt 해시 생성 사이트: https://bcrypt-generator.com (rounds=10)</li>
 * </ul>
 * </p>
 *
 * @author Jay
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * .env 파일의 ADMIN_USERNAME. 초기 계정 생성에만 사용됩니다.
     * 이미 DB에 해당 username이 있으면 이 값은 무시됩니다.
     */
    @Value("${admin.username}")
    private String adminUsername;

    /**
     * .env 파일의 ADMIN_PASSWORD. 초기 계정 생성에만 사용됩니다.
     * 계정이 DB에 생성된 이후에는 이 값을 바꿔도 로그인 비밀번호가 변경되지 않습니다.
     * 비밀번호 변경은 Supabase 대시보드에서 직접 해야 합니다.
     */
    @Value("${admin.password}")
    private String adminPassword;

    /**
     * Spring Boot 시작 완료 후 자동으로 호출되는 초기화 메서드.
     *
     * <p>JPA {@code ddl-auto: update} 덕분에 {@code admin_users} 테이블은
     * 이미 이 메서드 실행 전에 자동 생성되어 있습니다.</p>
     *
     * @param args 커맨드라인 인수 (사용하지 않음)
     */
    @Override
    public void run(ApplicationArguments args) {
        if (adminUserRepository.findByUsername(adminUsername).isPresent()) {
            log.info("[초기화] 관리자 계정 이미 존재 → 건너뜀 (username: {})", adminUsername);
            return;
        }

        // 평문 비밀번호를 BCrypt로 해싱하여 DB에 저장
        String encodedPassword = passwordEncoder.encode(adminPassword);
        AdminUser admin = AdminUser.create(adminUsername, encodedPassword);
        adminUserRepository.save(admin);

        log.info("========================================");
        log.info("[초기화] 관리자 계정 DB 생성 완료");
        log.info("  username : {}", adminUsername);
        log.info("  password : .env 파일 ADMIN_PASSWORD 참조");
        log.info("  table    : admin_users (neon 사용)");
        log.info("  주의: 이후 비밀번호 변경은 neon 사용");
        log.info("========================================");
    }
}
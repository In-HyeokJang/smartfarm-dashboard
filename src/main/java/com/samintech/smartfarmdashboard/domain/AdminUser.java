package com.samintech.smartfarmdashboard.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 대시보드 관리자 계정을 저장하는 JPA 엔티티.
 *
 * <p>테이블명을 {@code admin_users}로 지정한 이유:
 * PostgreSQL에서 {@code user}는 예약어이므로 {@code users}만 사용해도
 * 문제가 생길 수 있어 명확하게 {@code admin_users}로 분리합니다.</p>
 *
 * <p>비밀번호 보안 정책:
 * <ul>
 *   <li>비밀번호는 절대 평문으로 저장하지 않음</li>
 *   <li>{@link org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder}로 단방향 해싱</li>
 *   <li>BCrypt는 salt가 자동 포함되어 같은 비밀번호도 매번 다른 해시값 생성</li>
 *   <li>복호화 불가 → 비밀번호 분실 시 재설정만 가능 (조회 불가)</li>
 * </ul>
 * </p>
 *
 * <p>계정 생성 흐름:
 * {@code .env} ADMIN_USERNAME/PASSWORD → {@link com.samintech.smartfarmdashboard.config.DataInitializer}
 * → BCrypt 해싱 → {@code admin_users} 테이블에 저장 (최초 1회)
 * </p>
 *
 * @author Jay
 */
@Entity
@Table(name = "admin_users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA 전용 기본 생성자, 외부에서 직접 new 금지
public class AdminUser {

    /** DB 자동 증가 기본키 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 로그인 아이디. 중복 불가(unique).
     * 최대 50자로 제한.
     */
    @Column(nullable = false, unique = true, length = 50)
    private String username;

    /**
     * BCrypt로 해싱된 비밀번호.
     * 평문 비밀번호가 아닌 해시값이 저장됨.
     * 길이 제한 없음(BCrypt 해시는 60자).
     */
    @Column(nullable = false)
    private String password;

    /**
     * 권한 역할. "ADMIN" 값으로 고정.
     * Spring Security의 {@code .roles("ADMIN")} 메서드가 내부적으로 "ROLE_ADMIN"으로 변환함.
     */
    @Column(nullable = false, length = 20)
    private String role;

    /**
     * 계정 활성화 상태.
     * false이면 로그인 불가 (계정 잠금 용도).
     */
    @Column(nullable = false)
    private boolean enabled = true;

    /**
     * 계정 생성 시각.
     * {@code updatable = false}: 한 번 저장 후 수정 불가.
     */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 새 관리자 계정을 생성하는 팩토리 메서드.
     *
     * <p>이 메서드 호출 전 반드시 비밀번호를 BCrypt로 인코딩해야 합니다.
     * 평문 비밀번호를 직접 넘기면 안 됩니다.</p>
     *
     * <pre>{@code
     *   String encoded = passwordEncoder.encode(rawPassword);
     *   AdminUser admin = AdminUser.create("jay", encoded);
     * }</pre>
     *
     * @param username       로그인 아이디
     * @param encodedPassword BCrypt로 인코딩된 비밀번호 (평문 금지)
     * @return 저장 준비된 AdminUser 인스턴스
     */
    public static AdminUser create(String username, String encodedPassword) {
        AdminUser user = new AdminUser();
        user.username = username;
        user.password = encodedPassword;
        user.role = "ADMIN";
        user.enabled = true;
        user.createdAt = LocalDateTime.now();
        return user;
    }
}
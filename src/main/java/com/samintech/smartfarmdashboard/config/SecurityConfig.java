package com.samintech.smartfarmdashboard.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security 설정 클래스.
 *
 * <p><b>인증 방식: DB 기반 (Supabase PostgreSQL)</b><br>
 * 관리자 계정은 {@code admin_users} 테이블에 BCrypt 해시로 저장됩니다.
 * Spring Security가 {@link com.samintech.smartfarmdashboard.service.AdminUserDetailsService}
 * 빈을 자동 감지하여 DB 조회 → BCrypt 비교 → 인증 처리를 수행합니다.</p>
 *
 * <p>이전 방식(InMemoryUserDetailsManager)의 문제점:
 * <ul>
 *   <li>비밀번호가 메모리에만 존재 → 앱 재시작 시 초기화</li>
 *   <li>비밀번호 변경 시 .env 수정 + 앱 재시작 필요</li>
 *   <li>계정 추가/삭제가 불가능</li>
 * </ul>
 * </p>
 *
 * <p>현재 방식(DB 기반)의 장점:
 * <ul>
 *   <li>Supabase에서 직접 계정 관리 가능</li>
 *   <li>앱 재시작 없이 비밀번호 변경 가능</li>
 *   <li>계정 비활성화(enabled=false)로 접근 차단 가능</li>
 *   <li>복수의 관리자 계정 지원</li>
 * </ul>
 * </p>
 *
 * <p>이 클래스에서 {@code UserDetailsService}를 별도로 등록하지 않는 이유:
 * {@link com.samintech.smartfarmdashboard.service.AdminUserDetailsService}가
 * {@code @Service}로 등록되고 {@code UserDetailsService}를 구현하면
 * Spring Security가 자동으로 발견하여 인증에 사용합니다.</p>
 *
 * @author Jay
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * HTTP 보안 필터 체인을 구성합니다.
     *
     * <p>URL별 접근 권한, 커스텀 로그인/로그아웃 페이지를 설정합니다.</p>
     *
     * @param http Spring Security의 HttpSecurity 빌더
     * @return 구성 완료된 SecurityFilterChain
     * @throws Exception HttpSecurity 설정 중 발생할 수 있는 예외
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(auth -> auth
                        // 로그인 페이지, 에러 페이지는 인증 없이 접근 허용
                        .requestMatchers("/login", "/error").permitAll()
                        // 나머지 모든 URL은 로그인 필수
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        // 커스텀 로그인 페이지 (templates/login.html)
                        .loginPage("/login")
                        // 로그인 성공 시 대시보드로 이동 (alwaysUse=true: 어디서 로그인해도 항상 대시보드)
                        .defaultSuccessUrl("/dashboard", true)
                        // 로그인 실패 시 ?error 파라미터로 실패 메시지 표시
                        .failureUrl("/login?error")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        // 세션 완전 삭제 + 쿠키 제거로 보안 강화
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )
                .build();
    }

    /**
     * 비밀번호 해싱에 사용할 BCrypt 인코더 Bean.
     *
     * <p>이 Bean은 다음 두 곳에서 사용됩니다:
     * <ul>
     *   <li>{@link DataInitializer}: 초기 관리자 비밀번호 해싱 시</li>
     *   <li>Spring Security 내부: 로그인 시 입력 비밀번호와 DB 해시 비교 시</li>
     * </ul>
     * </p>
     *
     * @return BCryptPasswordEncoder (work factor=10, 기본값)
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
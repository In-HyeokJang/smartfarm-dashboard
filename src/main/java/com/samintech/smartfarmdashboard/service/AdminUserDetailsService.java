package com.samintech.smartfarmdashboard.service;

import com.samintech.smartfarmdashboard.domain.AdminUser;
import com.samintech.smartfarmdashboard.domain.AdminUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Spring Security의 DB 기반 인증을 처리하는 서비스.
 *
 * <p>{@link UserDetailsService}를 구현하면 Spring Security가 자동으로 이 클래스를 인증에 사용합니다.
 * 별도의 {@link com.samintech.smartfarmdashboard.config.SecurityConfig} 설정 없이
 * 빈으로 등록되는 것만으로 Spring Security가 자동 감지합니다.</p>
 *
 * <p>인증 흐름:
 * <pre>
 *   사용자가 로그인 폼 제출
 *        ↓
 *   Spring Security → loadUserByUsername(username) 호출
 *        ↓
 *   admin_users 테이블에서 username으로 조회
 *        ↓
 *   반환된 UserDetails의 password(BCrypt 해시)와 입력 비밀번호 비교
 *        ↓
 *   일치하면 인증 성공 → 세션 생성 → 대시보드로 이동
 * </pre>
 * </p>
 *
 * @author Jay
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserDetailsService implements UserDetailsService {

    private final AdminUserRepository adminUserRepository;

    /**
     * Spring Security가 로그인 시 호출하는 메서드.
     * 주어진 사용자명으로 DB에서 관리자 계정을 조회하여 {@link UserDetails}를 반환합니다.
     *
     * <p>비밀번호 검증은 이 메서드에서 하지 않습니다.
     * Spring Security가 반환된 UserDetails의 password(해시)와
     * 사용자 입력 비밀번호를 BCrypt로 비교하는 것을 자동 처리합니다.</p>
     *
     * @param username 로그인 폼에 입력된 사용자명
     * @return 인증에 필요한 UserDetails (username, 인코딩된 password, roles 포함)
     * @throws UsernameNotFoundException DB에 해당 username이 없거나 계정이 비활성화된 경우
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AdminUser adminUser = adminUserRepository.findByUsername(username)
                .orElseThrow(() -> {
                    // 보안상 "사용자 없음"과 "비밀번호 틀림"을 구분하지 않음
                    log.warn("로그인 시도 - 존재하지 않는 계정: {}", username);
                    return new UsernameNotFoundException("인증 실패: " + username);
                });

        if (!adminUser.isEnabled()) {
            log.warn("로그인 시도 - 비활성화된 계정: {}", username);
            throw new UsernameNotFoundException("비활성화된 계정입니다: " + username);
        }

        // Spring Security의 User 빌더로 UserDetails 생성
        // .roles("ADMIN") → 내부적으로 "ROLE_ADMIN" 권한 부여
        // password는 DB의 BCrypt 해시값을 그대로 전달 (Spring Security가 비교를 담당)
        return User.builder()
                .username(adminUser.getUsername())
                .password(adminUser.getPassword())
                .roles(adminUser.getRole())
                .build();
    }
}
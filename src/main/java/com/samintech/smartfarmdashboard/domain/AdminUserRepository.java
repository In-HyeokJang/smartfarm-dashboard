package com.samintech.smartfarmdashboard.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * {@link AdminUser} 엔티티의 DB 조회를 담당하는 Spring Data JPA 리포지토리.
 *
 * <p>Spring Security 로그인 처리 시 {@link com.samintech.smartfarmdashboard.service.AdminUserDetailsService}가
 * 이 리포지토리를 통해 사용자명으로 계정을 조회합니다.</p>
 *
 * @author Jay
 */
public interface AdminUserRepository extends JpaRepository<AdminUser, Long> {

    /**
     * 사용자명(로그인 아이디)으로 관리자 계정을 조회합니다.
     *
     * <p>Spring Security 인증 과정에서 로그인 폼에 입력된 username으로 이 메서드를 호출합니다.
     * username 컬럼에 unique 제약이 있으므로 항상 0건 또는 1건만 반환됩니다.</p>
     *
     * @param username 조회할 로그인 아이디
     * @return 일치하는 AdminUser (없으면 Optional.empty())
     */
    Optional<AdminUser> findByUsername(String username);
}
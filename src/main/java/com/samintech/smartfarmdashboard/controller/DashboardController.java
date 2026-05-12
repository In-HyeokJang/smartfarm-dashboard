package com.samintech.smartfarmdashboard.controller;

import com.samintech.smartfarmdashboard.dto.SensorDataDto;
import com.samintech.smartfarmdashboard.service.SensorDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

/**
 * 대시보드 메인 페이지와 로그인 페이지를 처리하는 컨트롤러.
 *
 * <p>Thymeleaf 뷰를 반환하는 MVC 컨트롤러입니다.
 * REST API(JSON 반환)는 {@link PumpController}와 {@link SseController}에서 담당합니다.</p>
 *
 * @author Jay
 */
@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final SensorDataService sensorDataService;

    /**
     * 루트 경로(/) 접근 시 대시보드로 리다이렉트합니다.
     *
     * @return /dashboard 로 리다이렉트
     */
    @GetMapping("/")
    public String root() {
        return "redirect:/dashboard";
    }

    /**
     * 대시보드 메인 페이지를 렌더링합니다.
     *
     * <p>페이지 초기 로딩 시 DB에서 최신 데이터와 히스토리를 가져와 Model에 담습니다.
     * 이후 실시간 업데이트는 SSE({@link SseController})로 처리됩니다.</p>
     *
     * @param model  Thymeleaf 템플릿에 데이터를 전달하는 Model 객체
     * @param auth   Spring Security 인증 정보 (로그인한 사용자명 추출용)
     * @return 렌더링할 Thymeleaf 템플릿 이름 (templates/dashboard.html)
     */
    @GetMapping("/dashboard")
    public String dashboard(Model model, Authentication auth) {
        // 현재 로그인한 관리자 이름을 상단 네비바에 표시
        model.addAttribute("username", auth.getName());

        // 대시보드 카드에 표시할 최신 센서 데이터 (데이터 없으면 빈 DTO)
        SensorDataDto latest = sensorDataService.getLatest();
        model.addAttribute("latest", latest);

        // 수분 추이 차트에 사용할 최근 20건 (JavaScript에서 역순으로 표시)
        List<SensorDataDto> history = sensorDataService.getHistory();
        model.addAttribute("history", history);

        return "dashboard";
    }

    /**
     * Spring Security의 커스텀 로그인 페이지를 렌더링합니다.
     *
     * <p>Spring Security는 기본 로그인 페이지를 자동 제공하지만,
     * {@link com.samintech.smartfarmdashboard.config.SecurityConfig}에서
     * {@code .loginPage("/login")}으로 커스텀 페이지를 지정했습니다.</p>
     *
     * @return 렌더링할 Thymeleaf 템플릿 이름 (templates/login.html)
     */
    @GetMapping("/login")
    public String login() {
        return "login";
    }
}
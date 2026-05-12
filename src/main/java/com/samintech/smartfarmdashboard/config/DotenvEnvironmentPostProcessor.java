package com.samintech.smartfarmdashboard.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

/**
 * 프로젝트 루트의 {@code .env} 파일을 읽어 Spring 환경에 속성으로 주입하는 후처리기.
 *
 * <p>Spring Boot는 애플리케이션 시작 시 {@link EnvironmentPostProcessor}를 실행합니다.
 * 이 클래스는 {@code @Configuration}이나 {@code @Component}가 아닌,
 * {@code META-INF/spring/org.springframework.boot.env.EnvironmentPostProcessor} 파일에
 * 클래스명을 등록하여 Spring이 자동으로 발견하고 실행합니다.</p>
 *
 * <p>속성 우선순위 (높음 → 낮음):
 * <ol>
 *   <li>OS 환경변수 / IntelliJ Run Configuration 환경변수</li>
 *   <li>{@code .env} 파일 (이 클래스가 로드)</li>
 *   <li>{@code application.yaml}</li>
 * </ol>
 * 즉, OS 환경변수가 있으면 {@code .env}보다 우선합니다 (프로덕션 배포 시 유용).
 * </p>
 *
 * <p>보안 정책:
 * <ul>
 *   <li>{@code .env} 파일은 {@code .gitignore}에 등록 → GitHub에 절대 업로드 안 됨</li>
 *   <li>{@code .env.example} 파일만 GitHub에 올려 팀원이 참고하도록 함</li>
 *   <li>{@code ignoreIfMissing()}: .env가 없어도 오류 없이 실행 (CI/CD, 도커 등 프로덕션 환경 대비)</li>
 * </ul>
 * </p>
 *
 * @author Jay
 */
public class DotenvEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    /**
     * Spring 환경에 {@code .env} 파일의 속성을 추가합니다.
     *
     * <p>이미 OS 환경변수로 같은 키가 존재하면 {@code .env} 값이 무시됩니다.
     * {@code addLast()}를 사용하여 기존 속성 소스보다 낮은 우선순위를 부여합니다.</p>
     *
     * @param environment Spring의 ConfigurableEnvironment (속성 소스 컨테이너)
     * @param application SpringApplication 인스턴스 (사용하지 않음)
     */
    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Dotenv dotenv = Dotenv.configure()
                // .env 파일이 없어도 예외 없이 계속 실행 (프로덕션 서버에는 .env 없음)
                .ignoreIfMissing()
                .load();

        // .env의 모든 항목을 Map으로 수집
        Map<String, Object> dotenvProps = new HashMap<>();
        dotenv.entries().forEach(entry -> dotenvProps.put(entry.getKey(), entry.getValue()));

        if (!dotenvProps.isEmpty()) {
            // "dotenv" 이름의 속성 소스를 가장 낮은 우선순위로 추가
            // → OS 환경변수, 시스템 속성이 .env보다 항상 우선
            environment.getPropertySources().addLast(
                    new MapPropertySource("dotenv", dotenvProps)
            );
        }
    }

    /**
     * 이 후처리기의 실행 순서를 반환합니다.
     *
     * <p>{@code LOWEST_PRECEDENCE}로 설정하여 다른 {@link EnvironmentPostProcessor}보다
     * 나중에 실행되고, 속성 소스는 가장 낮은 우선순위를 가집니다.</p>
     *
     * @return 실행 순서 (낮은 숫자 = 먼저 실행)
     */
    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
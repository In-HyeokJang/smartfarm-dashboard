# CLAUDE.md

Behavioral guidelines to reduce common LLM coding mistakes. Merge with project-specific instructions as needed.

**Tradeoff:** These guidelines bias toward caution over speed. For trivial tasks, use judgment.

## 1. Think Before Coding

**Don't assume. Don't hide confusion. Surface tradeoffs.**

Before implementing:
- State your assumptions explicitly. If uncertain, ask.
- If multiple interpretations exist, present them - don't pick silently.
- If a simpler approach exists, say so. Push back when warranted.
- If something is unclear, stop. Name what's confusing. Ask.

## 2. Simplicity First

**Minimum code that solves the problem. Nothing speculative.**

- No features beyond what was asked.
- No abstractions for single-use code.
- No "flexibility" or "configurability" that wasn't requested.
- No error handling for impossible scenarios.
- If you write 200 lines and it could be 50, rewrite it.

Ask yourself: "Would a senior engineer say this is overcomplicated?" If yes, simplify.

## 3. Surgical Changes

**Touch only what you must. Clean up only your own mess.**

When editing existing code:
- Don't "improve" adjacent code, comments, or formatting.
- Don't refactor things that aren't broken.
- Match existing style, even if you'd do it differently.
- If you notice unrelated dead code, mention it - don't delete it.

When your changes create orphans:
- Remove imports/variables/functions that YOUR changes made unused.
- Don't remove pre-existing dead code unless asked.

The test: Every changed line should trace directly to the user's request.

## 4. Goal-Driven Execution

**Define success criteria. Loop until verified.**

Transform tasks into verifiable goals:
- "Add validation" → "Write tests for invalid inputs, then make them pass"
- "Fix the bug" → "Write a test that reproduces it, then make it pass"
- "Refactor X" → "Ensure tests pass before and after"

For multi-step tasks, state a brief plan:
```
1. [Step] → verify: [check]
2. [Step] → verify: [check]
3. [Step] → verify: [check]
```

Strong success criteria let you loop independently. Weak criteria ("make it work") require constant clarification.

## 5. 보안 및 환경변수 정책 (이 프로젝트 필수 규칙)

**민감한 정보는 반드시 `.env` 파일에 저장하고 코드에 직접 쓰지 않는다.**

민감한 정보의 기준:
- DB 접속 정보 (호스트, 비밀번호, 사용자명)
- 관리자 계정 (아이디, 비밀번호)
- API 키, 토큰, 시크릿

규칙:
- `.env` 파일 → `.gitignore`에 등록 (GitHub에 절대 올라가지 않음)
- `.env.example` 파일 → GitHub에 올려 팀원에게 형식 안내
- `application.yaml`에서는 `${VARIABLE_NAME}` 형식으로 참조
- `DotenvEnvironmentPostProcessor`가 앱 시작 시 `.env`를 자동 로드

신규 설정값 추가 시:
1. `.env`에 실제 값 추가
2. `.env.example`에 빈 값으로 추가 (설명 주석 포함)
3. `application.yaml`에서 `${NEW_VAR}` 로 참조

## 6. Comment Requirements (이 프로젝트 필수 규칙)

**새로 추가하는 모든 로직에는 Javadoc 스타일 주석을 한국어로 작성한다.**

클래스 레벨:
- 이 클래스가 무엇을 담당하는지
- 어떤 다른 클래스와 연동되는지
- 데이터 흐름 (예: Arduino → SerialService → SensorDataService → DB)

메서드 레벨:
- 무엇을 하는 메서드인지
- `@param` 각 파라미터 설명
- `@return` 반환값 설명
- 예외가 발생할 수 있는 상황

인라인 주석:
- 비즈니스 로직의 계산식 (예: 수분 % 변환 공식)
- 외부 라이브러리의 비직관적인 동작
- 하드웨어 관련 수치의 의미

예시:
```java
/**
 * Arduino로부터 수신한 토양 수분 데이터를 DB에 저장하는 서비스.
 *
 * <p>데이터 흐름: SerialService → SensorDataService → SensorRepository → Supabase(PostgreSQL)</p>
 *
 * @author Jay
 */
```

---

**These guidelines are working if:** fewer unnecessary changes in diffs, fewer rewrites due to overcomplication, and clarifying questions come before implementation rather than after mistakes.

@README.md

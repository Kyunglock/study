# Filter vs Interceptor vs AOP

> 예제 코드: `src/main/java/com/study/pipeline`
> 실행 가능한 문서(테스트): `src/test/java/com/study/pipeline/PipelineOrderTest.java`

세 가지 모두 "공통 관심사(로깅, 인증, 트랜잭션 등)를 핵심 로직에서 분리"하기 위한 도구다.
차이는 **누가 관리하고, 어느 지점에서, 무엇을 알 수 있느냐**에 있다.

---

## 1. 한눈에 보는 요청 흐름

```
 HTTP 요청
    │
    ▼
┌──────────────────────── Servlet Container (Tomcat) ────────────────────────┐
│                                                                            │
│   Filter 1 → Filter 2 → ...            ◀── [Filter] 서블릿 스펙             │
│                    │                                                       │
│                    ▼                                                       │
│   ┌──────────────── DispatcherServlet (Spring MVC) ──────────────────────┐ │
│   │                                                                      │ │
│   │   Interceptor.preHandle()        ◀── [Interceptor] 스프링 MVC          │ │
│   │          │                                                           │ │
│   │          ▼                                                           │ │
│   │   ┌──────── Spring Container (Bean) ──────────────────────────────┐  │ │
│   │   │  Controller → [AOP Proxy] → Service → [AOP Proxy] → Repository│  │ │
│   │   │                    ▲                                          │  │ │
│   │   │                    └── [AOP] 스프링 빈 메서드 호출              │  │ │
│   │   └───────────────────────────────────────────────────────────────┘  │ │
│   │          │                                                           │ │
│   │   Interceptor.postHandle()   (정상 종료 시에만)                         │ │
│   │   View 렌더링                                                         │ │
│   │   Interceptor.afterCompletion()  (항상)                               │ │
│   └──────────────────────────────────────────────────────────────────────┘ │
│                    │                                                       │
│   ... → Filter 2 → Filter 1  (chain.doFilter() 이후 코드)                   │
└────────────────────────────────────────────────────────────────────────────┘
    │
    ▼
 HTTP 응답
```

바깥에서 안쪽으로: **Filter ⊃ Interceptor ⊃ (Controller) ⊃ AOP**

---

## 2. 비교표

| 구분 | Filter | Interceptor | AOP |
|---|---|---|---|
| 소속 | 서블릿 스펙 (`jakarta.servlet.Filter`) | 스프링 MVC (`HandlerInterceptor`) | 스프링 AOP (`@Aspect`) |
| 관리 주체 | 서블릿 컨테이너 (Tomcat) | DispatcherServlet | 스프링 컨테이너 (프록시) |
| 동작 위치 | DispatcherServlet **앞** | DispatcherServlet **안**, 컨트롤러 전후 | 스프링 **빈 메서드** 호출 전후 |
| 대상 | 모든 요청 (정적 리소스 포함) | 매핑된 핸들러가 있는 요청 | 포인트컷에 걸린 빈 메서드 |
| 알 수 있는 정보 | HTTP 요청/응답 | HTTP + **어떤 컨트롤러 메서드인지** (`HandlerMethod`) | **메서드 이름, 파라미터, 반환값, 예외** (HTTP 는 모름) |
| request/response 교체 | **가능** (Wrapper 로 바꿔 넘김) | 불가능 | 해당 없음 |
| 진행 중단 | `chain.doFilter()` 를 안 부름 | `preHandle` 에서 `false` 반환 | `proceed()` 를 안 부름 |
| 예외 처리 | `@ExceptionHandler` 범위 **밖** | `@ExceptionHandler` 범위 밖 (호출은 DispatcherServlet 안) | 예외를 잡거나 바꿔 던질 수 있음 |
| 웹 아닌 곳에서 | 불가 | 불가 | **가능** (배치, 스케줄러 등) |
| 대표 사용처 | 인코딩, CORS, Spring Security, 요청 바디 로깅 | 로그인/권한 체크, 공통 모델 추가 | `@Transactional`, `@Cacheable`, 실행 시간 측정 |

---

## 3. 각각 자세히

### 3.1 Filter — "HTTP 문 앞의 경비원"

```java
public class TraceFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        trace.add("Filter: before chain - " + request.getRequestURI());
        try {
            chain.doFilter(request, response);   // 다음 필터 또는 DispatcherServlet 으로
        } finally {
            trace.add("Filter: after chain - status=" + response.getStatus());
        }
    }
}
```

- 스프링이 아니라 **서블릿 컨테이너**가 호출한다. 그래서 스프링 MVC 의 정보(어떤 컨트롤러인지)를 모른다.
- `chain.doFilter(request, response)` 에 **다른 객체를 넘길 수 있다**. 요청 바디는 스트림이라 한 번만 읽을 수 있는데,
  `ContentCachingRequestWrapper` 로 감싸서 넘기면 여러 번 읽을 수 있다. 이건 Filter 에서만 가능하다.
- `OncePerRequestFilter` 를 쓰는 이유: forward / include / error 디스패치 때 같은 필터가 다시 실행되는 걸 막아,
  요청당 정확히 한 번만 실행되게 한다.
- 등록: `FilterRegistrationBean` (URL 패턴은 서블릿 문법 `/pipeline/*`) — `PipelineWebConfig` 참고.
  `@Component` 만 붙여도 등록되지만 그러면 **모든 URL** 에 적용된다.

### 3.2 Interceptor — "컨트롤러 앞의 비서"

```java
public class TraceInterceptor implements HandlerInterceptor {
    public boolean preHandle(..., Object handler)  { /* 컨트롤러 호출 전. false 면 중단 */ }
    public void postHandle(..., ModelAndView mv)   { /* 컨트롤러 정상 종료 후 */ }
    public void afterCompletion(..., Exception ex) { /* 항상 마지막에 */ }
}
```

- DispatcherServlet 이 핸들러(컨트롤러 메서드)를 찾은 **다음**에 호출되므로, `handler` 를 `HandlerMethod` 로 캐스팅해서
  메서드나 어노테이션 정보를 볼 수 있다. → `@LoginRequired` 같은 커스텀 어노테이션 기반 인가에 딱 맞다.
- 등록: `WebMvcConfigurer.addInterceptors()` (경로는 스프링 문법 `/pipeline/**`, `excludePathPatterns` 지원).

세 메서드의 호출 조건이 시험에 자주 나온다.

| 메서드 | 언제 | 예외 발생 시 |
|---|---|---|
| `preHandle` | 컨트롤러 호출 전 | — |
| `postHandle` | 컨트롤러 **정상** 반환 후, 뷰 렌더링 전 | **호출 안 됨** |
| `afterCompletion` | 뷰 렌더링 후 (항상) | 호출됨. 단, `@ExceptionHandler` 가 처리했다면 `ex == null` |

> ⚠️ `@RestController` 에서는 `postHandle` 시점에 이미 응답 바디가 쓰여 있다.
> 바디를 가공하려면 `ResponseBodyAdvice` 를 써야 한다.

> ⚠️ `preHandle` 이 `false` 를 반환하면, **그 인터셉터의** `postHandle`, `afterCompletion` 도 호출되지 않는다.
> (`afterCompletion` 은 `preHandle` 이 `true` 를 반환한 인터셉터에 대해서만 호출됨)

### 3.3 AOP — "빈 메서드에 씌운 투명 망토"

```java
@Aspect
@Component
public class TraceAspect {
    @Around("execution(* com.study.pipeline.service..*(..))")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        // 전처리
        Object result = joinPoint.proceed();  // 실제 메서드 호출
        // 후처리
        return result;
    }
}
```

- 스프링이 빈을 만들 때 **프록시 객체**로 감싸서 등록한다. 다른 빈이 주입받는 건 실제 객체가 아니라 프록시다.
- HTTP 와 무관하다. `HttpServletRequest` 가 꼭 필요하면 `RequestContextHolder` 로 꺼낼 수는 있지만,
  그럴 땐 Interceptor 가 더 맞는 도구인지 먼저 생각해보자.
- 어드바이스 종류: `@Before`, `@AfterReturning`, `@AfterThrowing`, `@After`, `@Around` (가장 강력함 — 진행 여부와 반환값까지 제어).

#### ⚠️ 내부 호출(self-invocation) 문제

```java
@Service
public class PipelineService {
    public String outer() {
        return "outer -> " + inner();   // this.inner() — 프록시를 거치지 않음!
    }
    public String inner() { return "inner"; }
}
```

```
Controller ──▶ [Proxy] ──▶ PipelineService.outer() ──▶ this.inner()
                 ▲ AOP 적용                              ▲ AOP 미적용 (프록시를 안 거침)
```

`@Transactional` 이 "안 먹는" 가장 흔한 원인이 이것이다. 해결책은 보통 **메서드를 다른 빈으로 분리**하는 것.

---

## 4. 실제 호출 순서 (테스트로 검증됨)

`PipelineOrderTest` 의 시나리오별 결과.

### 4.1 정상 요청 — `GET /pipeline/hello?name=kim`

```
Filter: before chain - /pipeline/hello
Interceptor: preHandle - PipelineController.hello
Controller: hello
AOP: before - greet[kim]
AOP: after returning - Hello, kim
Interceptor: postHandle
Interceptor: afterCompletion - ex=none
Filter: after chain - status=200
```
양파처럼 **들어간 순서의 역순으로 빠져나온다.**

### 4.2 예외 발생 — `GET /pipeline/error`

```
Filter: before chain - /pipeline/error
Interceptor: preHandle - PipelineController.error
Controller: error
AOP: before - fail[]
AOP: after throwing - IllegalStateException
ExceptionHandler: service failed
Interceptor: afterCompletion - ex=none       ◀ postHandle 없음, ex 도 null
Filter: after chain - status=500
```
- `postHandle` 은 건너뛴다.
- `@ExceptionHandler` 가 예외를 처리했기 때문에 `afterCompletion` 의 `ex` 는 `null` 이다.
  예외를 로깅하려고 `afterCompletion` 의 `ex` 에 의존하면 안 되는 이유.

### 4.3 인터셉터 차단 — `GET /pipeline/hello` + `X-Block: true`

```
Filter: before chain - /pipeline/hello
Interceptor: preHandle - blocked PipelineController.hello
Filter: after chain - status=403
```
컨트롤러, AOP, `postHandle`, `afterCompletion` 모두 실행되지 않는다. Filter 는 바깥에 있으니 그대로 실행된다.

### 4.4 내부 호출 — `GET /pipeline/self-invocation`

`AOP: before - outer[]` 는 찍히지만 `AOP: before - inner[]` 는 찍히지 않는다.

---

## 5. 언제 무엇을 쓰나

```
요청/응답 객체 자체를 바꿔야 한다? (바디 캐싱, 인코딩, XSS 필터링)
  └─ Yes → Filter
  └─ No
      스프링 MVC 바깥(정적 리소스, 서블릿 전체)까지 적용해야 한다? / Spring Security 와 같은 레벨?
        └─ Yes → Filter
        └─ No
            어떤 컨트롤러 메서드인지 알아야 하거나, 웹 요청 단위의 처리인가? (인증/인가, API 로깅)
              └─ Yes → Interceptor
              └─ No
                  비즈니스 메서드 단위의 처리인가? (트랜잭션, 캐시, 실행 시간, 재시도)
                  웹이 아닌 곳(배치, 스케줄러)에서도 적용돼야 하는가?
                    └─ Yes → AOP
```

---

## 6. 직접 실행해보기

```bash
./gradlew bootRun
```

```bash
curl "localhost:8080/pipeline/hello?name=kim"
curl localhost:8080/pipeline/trace          # 방금 요청의 호출 순서 확인 (조회 후 비워짐)

curl localhost:8080/pipeline/error
curl localhost:8080/pipeline/trace

curl -H "X-Block: true" localhost:8080/pipeline/hello
curl localhost:8080/pipeline/trace

curl localhost:8080/pipeline/self-invocation
curl localhost:8080/pipeline/trace
```

> `/pipeline/trace` 는 인터셉터에서는 제외했지만 필터는 거친다. 그래서 조회 결과 **맨 끝**에
> `Filter: before chain - /pipeline/trace` 가, 다음 조회 결과 **맨 앞**에 `Filter: after chain - status=200` 이 섞여 나온다.
> (컨트롤러가 목록을 반환한 뒤에 필터의 after 코드가 실행되기 때문)
> 이것도 "Filter 와 Interceptor 는 적용 범위를 따로 설정한다"는 차이를 보여준다.
>
> 8080 포트가 사용 중이면 `./gradlew bootRun --args='--server.port=18080'` 처럼 포트를 바꿔서 실행한다.

테스트로 확인:

```bash
./gradlew test
```

---

## 7. 더 알아볼 것

- `DelegatingFilterProxy` — Spring Security 가 스프링 빈을 서블릿 필터로 등록하는 방법
- `ResponseBodyAdvice` / `RequestBodyAdvice` — `@RestController` 의 바디를 가공하는 방법
- `HandlerMethodArgumentResolver` — 인터셉터 대신 컨트롤러 파라미터로 로그인 사용자 주입
- JDK 동적 프록시 vs CGLIB — 스프링 AOP 프록시가 만들어지는 방식

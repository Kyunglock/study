package com.study.pipeline.aop;

import com.study.pipeline.trace.CallTrace;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * [AOP] 스프링 컨테이너의 기능. 웹 요청과 무관하게 "스프링 빈의 메서드 호출"을 가로챈다.
 *
 * - HTTP 를 전혀 모른다. 대신 메서드 이름, 파라미터, 반환값, 던진 예외를 정확히 다룰 수 있다.
 * - 프록시 기반이라 "외부에서 프록시를 통해" 호출될 때만 동작한다.
 *   → 같은 클래스 안에서 this.method() 로 부르는 내부 호출(self-invocation)에는 적용되지 않는다.
 * - 웹 계층뿐 아니라 서비스, 리포지토리, 배치, 스케줄러 등 어디든 적용 가능하다.
 * - 주 용도: 트랜잭션(@Transactional), 캐시(@Cacheable), 실행 시간 측정, 비즈니스 로그
 */
@Aspect
@Component
public class TraceAspect {

    private final CallTrace trace;

    public TraceAspect(CallTrace trace) {
        this.trace = trace;
    }

    // com.study.pipeline.service 패키지(하위 포함)의 모든 메서드
    @Around("execution(* com.study.pipeline.service..*(..))")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        String method = joinPoint.getSignature().getName();
        trace.add("AOP: before - " + method + Arrays.toString(joinPoint.getArgs()));
        try {
            Object result = joinPoint.proceed();
            trace.add("AOP: after returning - " + result);
            return result;
        } catch (Throwable t) {
            trace.add("AOP: after throwing - " + t.getClass().getSimpleName());
            throw t;
        }
    }
}

package com.study.pipeline.interceptor;

import com.study.pipeline.trace.CallTrace;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

/**
 * [Interceptor] 스프링 MVC 의 기능. DispatcherServlet "안"에서 컨트롤러 호출 전후에 동작한다.
 *
 * - handler 파라미터로 "어떤 컨트롤러의 어떤 메서드"가 호출될지 알 수 있다. (HandlerMethod)
 *   → 메서드에 붙은 어노테이션을 보고 인가 처리를 하는 식의 활용이 가능하다.
 * - preHandle 이 false 를 반환하면 컨트롤러가 호출되지 않는다.
 * - request / response 를 다른 객체로 교체할 수는 없다. (Filter 와의 차이)
 * - 주 용도: 로그인/권한 체크, 공통 모델 데이터 추가, API 호출 로깅
 */
public class TraceInterceptor implements HandlerInterceptor {

    public static final String BLOCK_HEADER = "X-Block";

    private final CallTrace trace;

    public TraceInterceptor(CallTrace trace) {
        this.trace = trace;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String handlerName = handler instanceof HandlerMethod hm
                ? hm.getBeanType().getSimpleName() + "." + hm.getMethod().getName()
                : handler.toString();

        if ("true".equals(request.getHeader(BLOCK_HEADER))) {
            trace.add("Interceptor: preHandle - blocked " + handlerName);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return false; // 컨트롤러, postHandle, afterCompletion 모두 호출되지 않는다.
        }
        trace.add("Interceptor: preHandle - " + handlerName);
        return true;
    }

    /**
     * 컨트롤러가 "정상" 종료된 뒤에만 호출된다. (예외가 나면 호출되지 않음)
     * 주의: @RestController(@ResponseBody) 는 이 시점에 이미 응답 바디가 쓰여 있어서 바디를 바꿀 수 없다.
     */
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
                           ModelAndView modelAndView) {
        trace.add("Interceptor: postHandle");
    }

    /**
     * 뷰 렌더링까지 끝난 뒤 "항상" 호출된다. (try-finally 의 finally 같은 역할)
     * 주의: @ExceptionHandler 가 예외를 처리했다면 ex 는 null 로 넘어온다.
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
                                Exception ex) {
        trace.add("Interceptor: afterCompletion - ex=" + (ex == null ? "none" : ex.getClass().getSimpleName()));
    }
}

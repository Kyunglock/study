package com.study.pipeline.filter;

import com.study.pipeline.trace.CallTrace;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * [Filter] 서블릿 스펙(jakarta.servlet)의 기능. DispatcherServlet "앞"에서 동작한다.
 *
 * - 스프링 MVC 를 모르기 때문에 어떤 컨트롤러가 호출될지 알 수 없다. (URL, 헤더, 바디 등 HTTP 수준 정보만 앎)
 * - chain.doFilter() 를 호출하지 않으면 요청이 DispatcherServlet 까지 가지 않는다.
 * - request / response 객체 자체를 다른 객체(Wrapper)로 바꿔서 넘길 수 있는 유일한 지점이다.
 *   (예: 바디를 여러 번 읽기 위한 ContentCachingRequestWrapper)
 * - 주 용도: 인코딩, CORS, 보안(Spring Security 는 필터 체인으로 동작), 요청/응답 로깅
 */
public class TraceFilter extends OncePerRequestFilter {

    private final CallTrace trace;

    public TraceFilter(CallTrace trace) {
        this.trace = trace;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        trace.add("Filter: before chain - " + request.getRequestURI());
        try {
            chain.doFilter(request, response);
        } finally {
            trace.add("Filter: after chain - status=" + response.getStatus());
        }
    }
}

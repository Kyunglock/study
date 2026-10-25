package com.study.security.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

/**
 * 로그인은 했지만 권한이 없을 때(403) 응답을 만든다. ExceptionTranslationFilter 가 호출한다.
 *
 * 메뉴 권한(AuthorizationFilter), 기능 권한(AOP), 데이터 권한(비즈니스 로직) 중
 * 어디서 AccessDeniedException 이 나든 결국 여기로 모인다. message 로 어느 단계에서 막혔는지 구분할 수 있다.
 */
@Component
public class JsonAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public JsonAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException e)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), Map.of("status", 403, "message", e.getMessage()));
    }
}

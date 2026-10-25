package com.study.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 흔한 함정: 전역 @ExceptionHandler(Exception.class) 가 있으면
 * DispatcherServlet "안"에서 난 AccessDeniedException(AOP, @PreAuthorize, 비즈니스 로직)을 먼저 잡아서
 * 403 이 아니라 500 이 나간다.
 *
 * 필터(메뉴 권한)에서 난 예외는 DispatcherServlet "밖"이라 @ExceptionHandler 가 못 잡으므로 그대로 403 이다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ExceptionHandlerPitfallTest {

    @TestConfiguration
    static class CatchAllAdviceConfig {

        @RestControllerAdvice
        static class CatchAllExceptionHandler {

            @ExceptionHandler(Exception.class)
            ResponseEntity<String> handle(Exception e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("서버 오류");
            }
        }
    }

    @Autowired
    MockMvc mockMvc;

    @Test
    void AOP에서_난_권한_예외는_전역_핸들러에_잡혀_500이_된다() throws Exception {
        mockMvc.perform(put("/project/1").param("name", "변경").with(httpBasic("exec", "1234")))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void PreAuthorize에서_난_권한_예외도_500이_된다() throws Exception {
        mockMvc.perform(post("/budget/1/approve").with(httpBasic("pmo", "1234")))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void 필터_메뉴_권한에서_난_예외는_DispatcherServlet_밖이라_그대로_403이다() throws Exception {
        mockMvc.perform(post("/budget/1/approve").with(httpBasic("sales", "1234")))
                .andExpect(status().isForbidden());
    }
}

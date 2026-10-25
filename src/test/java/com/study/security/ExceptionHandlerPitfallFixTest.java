package com.study.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ExceptionHandlerPitfallTest 의 해결 방법:
 * 전역 핸들러에서 AccessDeniedException 만 다시 던져서 Security 의 ExceptionTranslationFilter 가 처리하게 한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ExceptionHandlerPitfallFixTest {

    @TestConfiguration
    static class FixedAdviceConfig {

        @RestControllerAdvice
        static class FixedExceptionHandler {

            // 더 구체적인 예외 타입의 핸들러가 우선 선택된다.
            @ExceptionHandler(AccessDeniedException.class)
            void rethrow(AccessDeniedException e) {
                throw e;
            }

            @ExceptionHandler(Exception.class)
            ResponseEntity<String> handle(Exception e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("서버 오류");
            }
        }
    }

    @Autowired
    MockMvc mockMvc;

    @Test
    void AOP에서_난_권한_예외가_다시_403이_된다() throws Exception {
        mockMvc.perform(put("/project/1").param("name", "변경").with(httpBasic("exec", "1234")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("WRITE_PROJECT 권한 없음"));
    }

    @Test
    void PreAuthorize에서_난_권한_예외도_다시_403이_된다() throws Exception {
        mockMvc.perform(post("/budget/1/approve").with(httpBasic("pmo", "1234")))
                .andExpect(status().isForbidden());
    }
}

package com.study.security;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 인증 → 메뉴 권한(Security Filter) → 기능 권한(AOP / 비즈니스 로직) → 데이터 권한(비즈니스 로직)
 * 각 단계에서 누가 막히는지 계정별로 검증한다. 403 응답의 message 로 어느 단계에서 막혔는지 구분한다.
 *
 * 계정 (비밀번호 모두 1234): pm1, pm2 (PM) / pmo (PMO) / sales (SALES) / exec (EXECUTIVE)
 */
@SpringBootTest
@AutoConfigureMockMvc
class SecurityLayersTest {

    private static final String PASSWORD = "1234";

    @Autowired
    MockMvc mockMvc;

    @Nested
    class 인증 {

        @Test
        void 로그인하지_않으면_401() throws Exception {
            mockMvc.perform(get("/project/1"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void 비밀번호가_틀리면_401() throws Exception {
            mockMvc.perform(get("/project/1").with(httpBasic("pm1", "wrong")))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    class 메뉴_권한_SecurityConfig {

        @Test
        void 영업은_프로젝트_메뉴에_들어갈_수_없다() throws Exception {
            mockMvc.perform(get("/project/1").with(httpBasic("sales", PASSWORD)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value("Access Denied"));
        }

        @Test
        void PM은_계약_메뉴에_들어갈_수_없다() throws Exception {
            mockMvc.perform(post("/contract").param("title", "신규 계약").with(httpBasic("pm1", PASSWORD)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value("Access Denied"));
        }

        @Test
        void 영업은_예산_메뉴에_들어갈_수_없다() throws Exception {
            mockMvc.perform(post("/budget/1/approve").with(httpBasic("sales", PASSWORD)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value("Access Denied"));
        }
    }

    @Nested
    class 기능_권한_방식3_커스텀_AOP {

        @Test
        void 경영진은_프로젝트를_조회할_수_있다() throws Exception {
            mockMvc.perform(get("/project/1").with(httpBasic("exec", PASSWORD)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.managerUsername").value("pm1"));
        }

        @Test
        void 경영진은_프로젝트_메뉴에는_들어가지만_수정은_못한다() throws Exception {
            mockMvc.perform(put("/project/1").param("name", "변경").with(httpBasic("exec", PASSWORD)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value("WRITE_PROJECT 권한 없음"));
        }
    }

    @Nested
    class 기능_권한_방식1_비즈니스_로직 {

        @Test
        void 영업은_계약을_등록할_수_있다() throws Exception {
            mockMvc.perform(post("/contract").param("title", "신규 계약").with(httpBasic("sales", PASSWORD)))
                    .andExpect(status().isOk())
                    .andExpect(content().string("계약 등록: 신규 계약"));
        }

        @Test
        void 경영진은_계약_메뉴에는_들어가지만_등록은_못한다() throws Exception {
            mockMvc.perform(post("/contract").param("title", "신규 계약").with(httpBasic("exec", PASSWORD)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value("WRITE_CONTRACT 권한 없음"));
        }
    }

    @Nested
    class 기능_권한_방식2_PreAuthorize {

        @Test
        void 경영진은_예산을_결재할_수_있다() throws Exception {
            mockMvc.perform(post("/budget/1/approve").with(httpBasic("exec", PASSWORD)))
                    .andExpect(status().isOk())
                    .andExpect(content().string("예산 결재 완료: 1"));
        }

        @Test
        void PMO는_예산_메뉴에는_들어가지만_결재는_못한다() throws Exception {
            mockMvc.perform(post("/budget/1/approve").with(httpBasic("pmo", PASSWORD)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value("Access Denied"));
        }
    }

    @Nested
    class 데이터_권한_비즈니스_로직 {

        @Test
        void PM은_본인_담당_프로젝트를_수정할_수_있다() throws Exception {
            mockMvc.perform(put("/project/1").param("name", "A 프로젝트 v2").with(httpBasic("pm1", PASSWORD)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("A 프로젝트 v2"));
        }

        @Test
        void PM은_남의_프로젝트를_수정할_수_없다() throws Exception {
            mockMvc.perform(put("/project/2").param("name", "변경").with(httpBasic("pm1", PASSWORD)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value("본인 담당 프로젝트만 수정할 수 있습니다."));
        }

        @Test
        void PMO는_모든_프로젝트를_수정할_수_있다() throws Exception {
            mockMvc.perform(put("/project/2").param("name", "B 프로젝트 v2").with(httpBasic("pmo", PASSWORD)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("B 프로젝트 v2"));
        }
    }
}

package com.study.pipeline;

import com.study.pipeline.interceptor.TraceInterceptor;
import com.study.pipeline.trace.CallTrace;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 각 시나리오에서 Filter / Interceptor / Controller / AOP 가 어떤 순서로 호출되는지 검증한다.
 * 테스트가 곧 "실행 가능한 문서"다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PipelineOrderTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    CallTrace trace;

    @BeforeEach
    void setUp() {
        trace.clear();
    }

    @Test
    void 정상_요청은_Filter_Interceptor_Controller_AOP_순으로_들어갔다가_역순으로_나온다() throws Exception {
        mockMvc.perform(get("/pipeline/hello").param("name", "kim"))
                .andExpect(status().isOk())
                .andExpect(content().string("Hello, kim"));

        assertThat(trace.events()).containsExactly(
                "Filter: before chain - /pipeline/hello",
                "Interceptor: preHandle - PipelineController.hello",
                "Controller: hello",
                "AOP: before - greet[kim]",
                "AOP: after returning - Hello, kim",
                "Interceptor: postHandle",
                "Interceptor: afterCompletion - ex=none",
                "Filter: after chain - status=200"
        );
    }

    @Test
    void 예외가_나면_postHandle_은_건너뛰고_ExceptionHandler_가_처리하면_afterCompletion_의_ex_는_null_이다() throws Exception {
        mockMvc.perform(get("/pipeline/error"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("service failed"));

        assertThat(trace.events()).containsExactly(
                "Filter: before chain - /pipeline/error",
                "Interceptor: preHandle - PipelineController.error",
                "Controller: error",
                "AOP: before - fail[]",
                "AOP: after throwing - IllegalStateException",
                "ExceptionHandler: service failed",
                "Interceptor: afterCompletion - ex=none",
                "Filter: after chain - status=500"
        );
    }

    @Test
    void preHandle_이_false_를_반환하면_컨트롤러와_AOP_는_실행되지_않는다() throws Exception {
        mockMvc.perform(get("/pipeline/hello").header(TraceInterceptor.BLOCK_HEADER, "true"))
                .andExpect(status().isForbidden());

        assertThat(trace.events()).containsExactly(
                "Filter: before chain - /pipeline/hello",
                "Interceptor: preHandle - blocked PipelineController.hello",
                "Filter: after chain - status=403"
        );
    }

    @Test
    void 같은_클래스_내부_호출에는_AOP_가_적용되지_않는다() throws Exception {
        mockMvc.perform(get("/pipeline/self-invocation"))
                .andExpect(status().isOk())
                .andExpect(content().string("outer -> inner"));

        assertThat(trace.events())
                .contains("AOP: before - outer[]")
                .doesNotContain("AOP: before - inner[]");
    }
}

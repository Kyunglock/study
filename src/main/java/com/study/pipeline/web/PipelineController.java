package com.study.pipeline.web;

import com.study.pipeline.service.PipelineService;
import com.study.pipeline.trace.CallTrace;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/pipeline")
public class PipelineController {

    private final PipelineService pipelineService;
    private final CallTrace trace;

    public PipelineController(PipelineService pipelineService, CallTrace trace) {
        this.pipelineService = pipelineService;
        this.trace = trace;
    }

    /** 정상 흐름: Filter -> Interceptor -> Controller -> AOP(Service) */
    @GetMapping("/hello")
    public String hello(@RequestParam(defaultValue = "spring") String name) {
        trace.add("Controller: hello");
        return pipelineService.greet(name);
    }

    /** 예외 흐름: postHandle 은 건너뛰고, @ExceptionHandler 가 처리한 뒤 afterCompletion 이 호출된다. */
    @GetMapping("/error")
    public String error() {
        trace.add("Controller: error");
        return pipelineService.fail();
    }

    /** AOP 내부 호출 한계: outer 에만 AOP 가 적용되고 inner 에는 적용되지 않는다. */
    @GetMapping("/self-invocation")
    public String selfInvocation() {
        trace.add("Controller: selfInvocation");
        return pipelineService.outer();
    }

    /** 직전 요청들의 호출 순서를 확인하고 비운다. (브라우저/curl 로 직접 확인용) */
    @GetMapping("/trace")
    public List<String> lastTrace() {
        List<String> events = trace.events();
        trace.clear();
        return events;
    }
}

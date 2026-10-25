package com.study.pipeline.service;

import org.springframework.stereotype.Service;

@Service
public class PipelineService {

    public String greet(String name) {
        return "Hello, " + name;
    }

    public String fail() {
        throw new IllegalStateException("service failed");
    }

    public String outer() {
        // this.inner() 는 프록시를 거치지 않는 내부 호출이라 AOP 가 적용되지 않는다.
        return "outer -> " + inner();
    }

    public String inner() {
        return "inner";
    }
}

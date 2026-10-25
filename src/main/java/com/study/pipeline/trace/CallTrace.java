package com.study.pipeline.trace;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Filter / Interceptor / AOP / Controller 가 "언제" 호출되는지 순서대로 기록한다.
 * 학습용이라 모든 요청이 하나의 리스트를 공유한다. (동시 요청이 섞이면 순서가 엉킬 수 있음)
 */
@Component
public class CallTrace {

    private static final Logger log = LoggerFactory.getLogger(CallTrace.class);

    private final List<String> events = new CopyOnWriteArrayList<>();

    public void add(String event) {
        events.add(event);
        log.info("[trace] {}", event);
    }

    public List<String> events() {
        return List.copyOf(events);
    }

    public void clear() {
        events.clear();
    }
}

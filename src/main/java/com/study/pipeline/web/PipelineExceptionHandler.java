package com.study.pipeline.web;

import com.study.pipeline.trace.CallTrace;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = PipelineController.class)
public class PipelineExceptionHandler {

    private final CallTrace trace;

    public PipelineExceptionHandler(CallTrace trace) {
        this.trace = trace;
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<String> handle(IllegalStateException e) {
        trace.add("ExceptionHandler: " + e.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
    }
}

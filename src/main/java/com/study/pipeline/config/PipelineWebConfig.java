package com.study.pipeline.config;

import com.study.pipeline.filter.TraceFilter;
import com.study.pipeline.interceptor.TraceInterceptor;
import com.study.pipeline.trace.CallTrace;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class PipelineWebConfig implements WebMvcConfigurer {

    private final CallTrace trace;

    public PipelineWebConfig(CallTrace trace) {
        this.trace = trace;
    }

    /**
     * Filter 등록: 서블릿 컨테이너(Tomcat)에 등록된다.
     * URL 패턴은 서블릿 스펙 문법("/pipeline/*")을 따른다.
     */
    @Bean
    public FilterRegistrationBean<TraceFilter> traceFilter() {
        FilterRegistrationBean<TraceFilter> registration = new FilterRegistrationBean<>(new TraceFilter(trace));
        registration.addUrlPatterns("/pipeline/*");
        registration.setOrder(1);
        return registration;
    }

    /**
     * Interceptor 등록: 스프링 MVC(DispatcherServlet)에 등록된다.
     * 경로 패턴은 스프링 문법("/pipeline/**")을 따르고, 제외 경로도 지정할 수 있다.
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new TraceInterceptor(trace))
                .addPathPatterns("/pipeline/**")
                .excludePathPatterns("/pipeline/trace");
    }
}

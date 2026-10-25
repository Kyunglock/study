package com.study.security.config;

import com.study.security.auth.Permission;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity   // @PreAuthorize 활성화 (Spring Security 가 제공하는 권한 체크 AOP)
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JsonAccessDeniedHandler accessDeniedHandler)
            throws Exception {
        http
                // REST API + HTTP Basic 예제라 CSRF 를 끈다. 세션 + 폼 로그인 화면이라면 켜둬야 한다.
                .csrf(csrf -> csrf.disable())
                // 인증: Authorization 헤더의 아이디/비밀번호로 로그인 (curl -u pm1:1234)
                .httpBasic(Customizer.withDefaults())
                // 인가 실패(403) 응답을 JSON 으로. 필터에서 난 예외는 @ExceptionHandler 가 못 잡기 때문
                .exceptionHandling(exception -> exception.accessDeniedHandler(accessDeniedHandler))
                // 메뉴(화면) 권한: URL → 역할. AuthorizationFilter 가 이 규칙으로 판단한다.
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/pipeline/**", "/error").permitAll()   // 01 예제와 에러 페이지
                        .requestMatchers("/project/**")
                        .hasAnyRole(Permission.PM.name(), Permission.PMO.name(), Permission.EXECUTIVE.name())
                        .requestMatchers("/contract/**")
                        .hasAnyRole(Permission.SALES.name(), Permission.EXECUTIVE.name())
                        .requestMatchers("/budget/**")
                        .hasAnyRole(Permission.PMO.name(), Permission.EXECUTIVE.name())
                        .anyRequest().authenticated());
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}

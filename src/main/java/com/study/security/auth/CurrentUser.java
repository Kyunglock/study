package com.study.security.auth;

import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 컨트롤러 파라미터(@AuthenticationPrincipal)로 받을 수 없는 곳(AOP, 서비스)에서 로그인 사용자를 꺼낸다.
 * SecurityContextHolder 는 요청 스레드에 묶여 있어서 같은 요청 안이면 어디서든 꺼낼 수 있다.
 */
public final class CurrentUser {

    private CurrentUser() {
    }

    public static LoginUser get() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof LoginUser user)) {
            throw new AuthenticationCredentialsNotFoundException("로그인이 필요합니다.");
        }
        return user;
    }
}

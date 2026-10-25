package com.study.security.auth;

/**
 * 사용자 역할. 한 사용자는 하나의 역할을 가진다.
 *
 * Spring Security 에는 "ROLE_" 접두사를 붙여 GrantedAuthority 로 넣는다. (PM → "ROLE_PM")
 * hasRole("PM") / hasAnyRole("PM", ...) 은 내부적으로 "ROLE_PM" 과 비교하기 때문.
 */
public enum Permission {
    PM,         // 프로젝트 매니저
    PMO,        // 프로젝트 관리 조직
    SALES,      // 영업
    EXECUTIVE   // 경영진
}

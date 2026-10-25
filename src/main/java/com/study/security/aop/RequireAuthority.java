package com.study.security.aop;

import com.study.security.auth.Authority;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 기능 권한 체크용 커스텀 어노테이션. AuthorityAspect 가 처리한다.
 *
 * @PreAuthorize 와 달리 속성 타입을 enum 으로 지정할 수 있어서 타입 안전하다.
 *   @PreAuthorize("hasAuthority('WRITE_PROJECT')")   ← 문자열. 오타가 나도 컴파일 에러가 안 난다.
 *   @RequireAuthority(Authority.WRITE_PROJECT)       ← enum. IDE 자동완성, 컴파일 타임 검증
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireAuthority {

    Authority value();
}

package com.study.security.aop;

import com.study.security.auth.Authority;
import com.study.security.auth.CurrentUser;
import com.study.security.auth.LoginUser;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/**
 * @RequireAuthority 가 붙은 메서드 실행 전에 기능 권한을 체크한다.
 * @PreAuthorize 도 내부적으로 이런 식으로 동작한다. (Spring Security 가 미리 만들어 둔 Aspect)
 */
@Aspect
@Component
public class AuthorityAspect {

    @Before("@annotation(requireAuthority)")
    public void check(RequireAuthority requireAuthority) {
        Authority authority = requireAuthority.value();
        LoginUser user = CurrentUser.get();
        if (!authority.isAllowed(user.getPermission())) {
            throw new AccessDeniedException(authority + " 권한 없음");
        }
    }
}

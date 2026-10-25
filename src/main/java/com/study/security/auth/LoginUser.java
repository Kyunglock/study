package com.study.security.auth;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.ArrayList;
import java.util.List;

/**
 * 로그인한 사용자. Authentication 의 principal 로 SecurityContext 에 보관된다.
 *
 * 권한 목록(authorities)에는 두 종류를 함께 넣는다.
 * - 역할:      "ROLE_PM"         → SecurityConfig 의 hasRole / hasAnyRole 에서 사용 (메뉴 권한)
 * - 기능 권한:  "WRITE_PROJECT"   → @PreAuthorize("hasAuthority('...')") 에서 사용 (기능 권한)
 */
public class LoginUser extends User {

    private final Permission permission;

    public LoginUser(String username, String password, Permission permission) {
        super(username, password, authoritiesOf(permission));
        this.permission = permission;
    }

    private static List<GrantedAuthority> authoritiesOf(Permission permission) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + permission.name()));
        for (Authority authority : Authority.values()) {
            if (authority.isAllowed(permission)) {
                authorities.add(new SimpleGrantedAuthority(authority.name()));
            }
        }
        return authorities;
    }

    public Permission getPermission() {
        return permission;
    }

    public boolean is(Permission permission) {
        return this.permission == permission;
    }
}

package com.study.security.auth;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 로그인할 때 "한 번" 호출되어 사용자와 역할(Role)을 꺼내오는 곳.
 *
 * 흐름: 로그인 요청 → AuthenticationManager → DaoAuthenticationProvider
 *      → loadUserByUsername() (여기) → 비밀번호 확인 → Authentication 생성 → SecurityContext 에 저장
 *
 * 실무에서는 DB 를 조회한다. 예제라 메모리에 계정을 둔다. (비밀번호는 모두 1234)
 */
@Service
public class LoginUserDetailsService implements UserDetailsService {

    private record Account(String encodedPassword, Permission permission) {
    }

    private final Map<String, Account> accounts;

    public LoginUserDetailsService(PasswordEncoder passwordEncoder) {
        String password = passwordEncoder.encode("1234");
        this.accounts = Map.of(
                "pm1", new Account(password, Permission.PM),
                "pm2", new Account(password, Permission.PM),
                "pmo", new Account(password, Permission.PMO),
                "sales", new Account(password, Permission.SALES),
                "exec", new Account(password, Permission.EXECUTIVE)
        );
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        Account account = accounts.get(username);
        if (account == null) {
            throw new UsernameNotFoundException(username);
        }
        return new LoginUser(username, account.encodedPassword(), account.permission());
    }
}

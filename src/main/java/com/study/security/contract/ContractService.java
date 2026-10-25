package com.study.security.contract;

import com.study.security.auth.Authority;
import com.study.security.auth.LoginUser;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

/**
 * 기능 권한 방식 ①: 비즈니스 로직에서 직접 체크
 *
 * 장점: enum 을 그대로 호출해서 타입 안전하고, 권한에 따라 로직을 분기하기 쉽다.
 * 단점: 메서드마다 같은 if 문이 반복되고, 권한 체크가 핵심 로직과 섞인다.
 */
@Service
public class ContractService {

    public String write(String title, LoginUser user) {
        if (!Authority.WRITE_CONTRACT.isAllowed(user.getPermission())) {
            throw new AccessDeniedException(Authority.WRITE_CONTRACT + " 권한 없음");
        }
        return "계약 등록: " + title;
    }
}

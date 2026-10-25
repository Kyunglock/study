package com.study.security.auth;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

/**
 * 기능(행위) 권한. "이 기능을 어떤 역할이 쓸 수 있나"를 한 곳에 표처럼 모아둔다.
 *
 * - 기능 단위 조정은 여기 인자만 바꾸면 된다. (예: 영업에게 프로젝트 조회 허용 → READ_PROJECT 에 SALES 추가)
 * - "본인 담당 프로젝트만" 같은 데이터 단위 조건은 여기서 표현할 수 없다. → 비즈니스 로직에서 체크
 * - 코드라서 바꾸려면 재배포가 필요하다. 운영 중 변경이 필요하면 DB 테이블로 관리한다.
 */
public enum Authority {
    WRITE_PROJECT(Permission.PM, Permission.PMO),
    READ_PROJECT(Permission.PM, Permission.PMO, Permission.EXECUTIVE),
    WRITE_CONTRACT(Permission.SALES),
    READ_CONTRACT(Permission.SALES, Permission.EXECUTIVE),
    APPROVE_BUDGET(Permission.EXECUTIVE);

    private final Set<Permission> allowed;

    Authority(Permission... allowed) {
        this.allowed = EnumSet.copyOf(Arrays.asList(allowed));
    }

    public boolean isAllowed(Permission role) {
        return allowed.contains(role);
    }
}

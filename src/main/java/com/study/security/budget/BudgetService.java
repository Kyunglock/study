package com.study.security.budget;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 기능 권한 방식 ②: @PreAuthorize (Spring Security 의 메서드 보안 = AOP)
 *
 * LoginUser 가 로그인할 때 "APPROVE_BUDGET" 같은 기능 권한을 authorities 에 넣어 두었기 때문에
 * hasAuthority('APPROVE_BUDGET') 로 체크할 수 있다.
 */
@Service
public class BudgetService {

    @PreAuthorize("hasAuthority('APPROVE_BUDGET')")
    public String approve(Long id) {
        return "예산 결재 완료: " + id;
    }

    /**
     * ⚠️ 일부러 만든 함정: this.approve() 는 프록시를 거치지 않는 내부 호출이라 @PreAuthorize 가 적용되지 않는다.
     * 권한이 없는 사용자도 이 메서드를 통하면 결재할 수 있다. (테스트에서 확인)
     */
    public List<String> approveAll(List<Long> ids) {
        return ids.stream().map(this::approve).toList();
    }
}

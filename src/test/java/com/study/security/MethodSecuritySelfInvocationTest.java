package com.study.security;

import com.study.security.budget.BudgetService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithUserDetails;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @PreAuthorize 도 AOP(프록시)라서 같은 클래스 안의 내부 호출에는 적용되지 않는다.
 * 01 문서의 self-invocation 문제가 보안에서는 "권한 우회"가 된다.
 */
@SpringBootTest
class MethodSecuritySelfInvocationTest {

    @Autowired
    BudgetService budgetService;

    @Test
    @WithUserDetails("pmo")   // LoginUserDetailsService 로 pmo 계정을 로그인시킨 상태로 실행
    void 프록시를_거치면_PreAuthorize_가_막는다() {
        assertThatThrownBy(() -> budgetService.approve(1L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @WithUserDetails("pmo")
    void 내부_호출로_들어가면_PreAuthorize_가_적용되지_않아_권한이_우회된다() {
        assertThat(budgetService.approveAll(List.of(1L, 2L)))
                .containsExactly("예산 결재 완료: 1", "예산 결재 완료: 2");
    }
}

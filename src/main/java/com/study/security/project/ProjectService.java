package com.study.security.project;

import com.study.security.aop.RequireAuthority;
import com.study.security.auth.Authority;
import com.study.security.auth.LoginUser;
import com.study.security.auth.Permission;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;

/**
 * 기능 권한 방식 ③: 커스텀 어노테이션 + 직접 만든 AOP (@RequireAuthority)
 * 데이터 권한: 비즈니스 로직에서 체크
 */
@Service
public class ProjectService {

    private final ProjectRepository projectRepository;

    public ProjectService(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    @RequireAuthority(Authority.READ_PROJECT)
    public Project find(Long id) {
        return projectRepository.findById(id).orElseThrow(() -> new NoSuchElementException("project " + id));
    }

    @RequireAuthority(Authority.WRITE_PROJECT)
    public Project rename(Long id, String name, LoginUser user) {
        // this.find(id) 는 내부 호출이라 @RequireAuthority 가 적용되지 않는다. 저장소를 직접 쓴다.
        Project project = projectRepository.findById(id).orElseThrow(() -> new NoSuchElementException("project " + id));

        // 데이터 권한: PM 은 본인 담당 프로젝트만 수정 가능 (PMO 는 전체 가능)
        // 역할이 아니라 "데이터(담당자)"를 봐야 알 수 있어서 enum 으로는 표현할 수 없다.
        if (user.is(Permission.PM) && !project.isManagedBy(user.getUsername())) {
            throw new AccessDeniedException("본인 담당 프로젝트만 수정할 수 있습니다.");
        }
        return projectRepository.save(project.rename(name));
    }
}

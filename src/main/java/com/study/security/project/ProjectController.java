package com.study.security.project;

import com.study.security.auth.LoginUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/project")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping("/{id}")
    public Project find(@PathVariable Long id) {
        return projectService.find(id);
    }

    /** @AuthenticationPrincipal: SecurityContext 의 principal(LoginUser)을 파라미터로 받는다. */
    @PutMapping("/{id}")
    public Project rename(@PathVariable Long id, @RequestParam String name,
                          @AuthenticationPrincipal LoginUser user) {
        return projectService.rename(id, name, user);
    }
}

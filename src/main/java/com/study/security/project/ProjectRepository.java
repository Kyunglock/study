package com.study.security.project;

import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** 예제용 메모리 저장소. 1번은 pm1, 2번은 pm2 가 담당한다. */
@Repository
public class ProjectRepository {

    private final Map<Long, Project> store = new ConcurrentHashMap<>(Map.of(
            1L, new Project(1L, "A 프로젝트", "pm1"),
            2L, new Project(2L, "B 프로젝트", "pm2")
    ));

    public Optional<Project> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    public Project save(Project project) {
        store.put(project.id(), project);
        return project;
    }
}

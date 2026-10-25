package com.study.security.project;

/** managerUsername: 이 프로젝트를 담당하는 PM. 데이터 권한 체크에 쓴다. */
public record Project(Long id, String name, String managerUsername) {

    public Project rename(String newName) {
        return new Project(id, newName, managerUsername);
    }

    public boolean isManagedBy(String username) {
        return managerUsername.equals(username);
    }
}

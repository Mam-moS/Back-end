package com.mmos.mmos.src.repository;

import com.mmos.mmos.src.domain.entity.Project;
import com.mmos.mmos.src.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectRepository extends JpaRepository<Project,Long> {
    List<Project> findAllByUser(User user);

    Long countByUserAndProjectIsVisibleTrue(User user);
}

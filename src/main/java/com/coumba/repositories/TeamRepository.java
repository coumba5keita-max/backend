package com.coumba.repositories;

import com.coumba.entities.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {
    boolean existsByName(String name);
    Optional<Team> findByName(String name);
}

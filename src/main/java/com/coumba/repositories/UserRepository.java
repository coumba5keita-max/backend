package com.coumba.repositories;

import com.coumba.entities.User;
import com.coumba.entities.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    long countByRoleAndIsActiveTrue(Role role);
}

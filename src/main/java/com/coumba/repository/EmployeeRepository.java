package com.coumba.repository;

import com.coumba.model.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    // JpaRepository fournit déjà toutes les méthodes CRUD de base !
}
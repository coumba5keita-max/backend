package com.coumba.service;

import com.coumba.model.Employee;
import com.coumba.repository.EmployeeRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class EmployeeService {

    private final EmployeeRepository employeeRepository;

    public EmployeeService(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    // Obtenir tous les employés
    public List<Employee> getAllEmployees() {
        return employeeRepository.findAll();
    }

    // Obtenir un employé par son ID
    public Optional<Employee> getEmployeeById(Long id) {
        return employeeRepository.findById(id);
    }

    // Enregistrer un nouvel employé
    public Employee saveEmployee(Employee employee) {
        return employeeRepository.save(employee);
    }

    // Supprimer un employé par son ID
    public void deleteEmployee(Long id) {
        employeeRepository.deleteById(id);
    }
}
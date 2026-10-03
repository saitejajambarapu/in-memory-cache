package com.inmemory.cache.service;


import com.inmemory.cache.model.Employee;
import com.inmemory.cache.repositories.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;

    public List<Employee> getAllEmployees() {
        List<Employee> employees =employeeRepository.findAll();
        return employees;
    }

    public Employee getEmployeeById(Long id) {
        Employee employee =  employeeRepository.findById(id).orElseThrow(()->new RuntimeException("No User Found"));
        return employee;
    }
}
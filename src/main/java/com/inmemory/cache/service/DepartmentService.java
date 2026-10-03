package com.inmemory.cache.service;

import com.inmemory.cache.model.Department;
import com.inmemory.cache.repositories.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentRepository departmentRepository;

    public List<Department> getDepartments(
            String name,
            String location) {

        return departmentRepository
                .findByNameAndLocation(
                        name,
                        location
                );
    }

    public Department getDepartmentById(Long id) {

        return departmentRepository
                .findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "No Department Found"
                        )
                );
    }
}
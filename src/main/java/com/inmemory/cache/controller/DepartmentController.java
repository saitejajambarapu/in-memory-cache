package com.inmemory.cache.controller;


import com.inmemory.cache.annotation.MyCache;
import com.inmemory.cache.model.Department;
import com.inmemory.cache.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService departmentService;

    @GetMapping
    @MyCache("AllDepartmentsCache")
    public List<Department> getDepartments(
            @RequestParam String name,
            @RequestParam String location) {

        return departmentService.getDepartments(
                name,
                location
        );
    }

    @GetMapping("/{id}")
    @MyCache("IndividualDepartmentCache")
    public Department getDepartmentById(
            @PathVariable Long id) {

        return departmentService.getDepartmentById(id);
    }
}
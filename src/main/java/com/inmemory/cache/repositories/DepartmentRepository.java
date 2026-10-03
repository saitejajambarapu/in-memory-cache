package com.inmemory.cache.repositories;

import com.inmemory.cache.model.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DepartmentRepository
        extends JpaRepository<Department, Long> {

    List<Department> findByLocation(String location);

    List<Department> findByNameAndLocation(
            String name,
            String location
    );

}

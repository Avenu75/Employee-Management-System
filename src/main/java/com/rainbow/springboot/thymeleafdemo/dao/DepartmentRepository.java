package com.rainbow.springboot.thymeleafdemo.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.rainbow.springboot.thymeleafdemo.entity.Department;

/**
 * Data-access layer for Department.
 * Extending JpaRepository gives us save(), findAll(), findById(), deleteById(), etc. for free.
 */
public interface DepartmentRepository extends JpaRepository<Department, Integer> {

    List<Department> findAllByOrderByNameAsc();
}

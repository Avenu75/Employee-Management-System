package com.rainbow.springboot.thymeleafdemo.service;

import java.util.List;

import com.rainbow.springboot.thymeleafdemo.entity.Department;

public interface DepartmentService {

    List<Department> findAll();

    Department findById(int theId);

    void save(Department theDepartment);

    void deleteById(int theId);
}

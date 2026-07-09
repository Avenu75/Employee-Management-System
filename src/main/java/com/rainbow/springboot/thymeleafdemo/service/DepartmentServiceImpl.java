package com.rainbow.springboot.thymeleafdemo.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.rainbow.springboot.thymeleafdemo.dao.DepartmentRepository;
import com.rainbow.springboot.thymeleafdemo.entity.Department;
import com.rainbow.springboot.thymeleafdemo.exception.DepartmentNotFoundException;

@Service
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentRepository departmentRepository;

    public DepartmentServiceImpl(DepartmentRepository theDepartmentRepository) {
        departmentRepository = theDepartmentRepository;
    }

    @Override
    public List<Department> findAll() {
        return departmentRepository.findAllByOrderByNameAsc();
    }

    @Override
    public Department findById(int theId) {
        Optional<Department> result = departmentRepository.findById(theId);

        if (result.isEmpty()) {
            throw new DepartmentNotFoundException("Did not find department id - " + theId);
        }

        return result.get();
    }

    @Override
    public void save(Department theDepartment) {
        departmentRepository.save(theDepartment);
    }

    @Override
    public void deleteById(int theId) {
        departmentRepository.deleteById(theId);
    }
}

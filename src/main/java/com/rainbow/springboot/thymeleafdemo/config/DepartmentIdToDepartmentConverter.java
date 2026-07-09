package com.rainbow.springboot.thymeleafdemo.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import com.rainbow.springboot.thymeleafdemo.dao.DepartmentRepository;
import com.rainbow.springboot.thymeleafdemo.entity.Department;

/**
 * Spring MVC converter that turns the department id submitted by the
 * <select> dropdown on the Employee form (a String, e.g. "2") back into
 * a full Department entity so it can be assigned to Employee.department.
 *
 * Without this, Spring would not know how to bind a plain form value to
 * a JPA entity field.
 */
@Component
public class DepartmentIdToDepartmentConverter implements Converter<String, Department> {

    private final DepartmentRepository departmentRepository;

    public DepartmentIdToDepartmentConverter(DepartmentRepository theDepartmentRepository) {
        departmentRepository = theDepartmentRepository;
    }

    @Override
    public Department convert(String source) {
        if (source == null || source.isBlank()) {
            return null;
        }
        try {
            int id = Integer.parseInt(source);
            return departmentRepository.findById(id).orElse(null);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}

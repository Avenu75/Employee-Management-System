package com.rainbow.springboot.thymeleafdemo.dao;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.rainbow.springboot.thymeleafdemo.entity.Employee;
import com.rainbow.springboot.thymeleafdemo.entity.EmployeeStatus;

import jakarta.persistence.criteria.Predicate;

/** Builds the optional filters used by the employee list and its exports. */
public final class EmployeeSpecs {

    private EmployeeSpecs() {
    }

    /**
     * Every argument is optional (null or blank means "do not filter on this").
     * The keyword is matched against first name, last name, email and job title.
     */
    public static Specification<Employee> filter(String keyword, Integer departmentId, EmployeeStatus status) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (keyword != null && !keyword.isBlank()) {
                String like = "%" + keyword.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.<String>get("firstName")), like),
                        cb.like(cb.lower(root.<String>get("lastName")), like),
                        cb.like(cb.lower(root.<String>get("email")), like),
                        cb.like(cb.lower(root.<String>get("jobTitle")), like)));
            }
            if (departmentId != null) {
                predicates.add(cb.equal(root.get("department").get("id"), departmentId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}

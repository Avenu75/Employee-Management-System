package com.rainbow.springboot.thymeleafdemo.service;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;

import com.rainbow.springboot.thymeleafdemo.entity.Employee;

public interface EmployeeService {

    List<Employee> findAll();

    /**
     * Returns a single page of employees, optionally filtered by a keyword
     * (matched against first name, last name, or email).
     *
     * @param keyword  free-text search term, or null/blank to return everyone
     * @param page     zero-based page number
     * @param size     number of rows per page
     * @param sortBy   field name to sort by (e.g. "lastName")
     */
    Page<Employee> search(String keyword, int page, int size, String sortBy);

    /**
     * Filtered page: keyword (name, email, job title), department and status are all optional.
     * sortBy is checked against a whitelist; anything unknown falls back to last name.
     */
    Page<Employee> filter(String keyword, Integer departmentId, String status,
                          int page, int size, String sortBy, boolean descending);

    /** Same filters without paging, used by the CSV and PDF exports. */
    List<Employee> filterAll(String keyword, Integer departmentId, String status,
                             String sortBy, boolean descending);

    Employee findById(int theId);

    void save(Employee theEmployee);

    void deleteById(int theId);

    long countActive();

    long countInactive();

    long countTotal();

    /** Department name -> number of employees, used for the dashboard chart. */
    Map<String, Long> countByDepartment();
}

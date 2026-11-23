package com.rainbow.springboot.thymeleafdemo.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rainbow.springboot.thymeleafdemo.dao.EmployeeRepository;
import com.rainbow.springboot.thymeleafdemo.dao.EmployeeSpecs;
import com.rainbow.springboot.thymeleafdemo.entity.Employee;
import com.rainbow.springboot.thymeleafdemo.entity.EmployeeStatus;
import com.rainbow.springboot.thymeleafdemo.exception.EmployeeNotFoundException;

/**
 * Business logic layer for Employee.
 *
 * The Controller never talks to the Repository directly - it always goes
 * through this Service. This "layered architecture" keeps business rules
 * (like logging, validation of business rules, calculations) out of the
 * web layer and out of the persistence layer.
 */
@Service
public class EmployeeServiceImpl implements EmployeeService {

    private static final Logger log = LoggerFactory.getLogger(EmployeeServiceImpl.class);

    /** Columns the list may be sorted by; protects against invalid or hostile sortBy values. */
    private static final Set<String> SORTABLE = Set.of("firstName", "lastName", "email", "jobTitle", "salary", "hireDate");

    private final EmployeeRepository employeeRepository;

    public EmployeeServiceImpl(EmployeeRepository theEmployeeRepository) {
        employeeRepository = theEmployeeRepository;
    }

    @Override
    public List<Employee> findAll() {
        return employeeRepository.findAllByOrderByLastNameAsc();
    }

    @Override
    public Page<Employee> search(String keyword, int page, int size, String sortBy) {
        String sortField = (sortBy == null || sortBy.isBlank()) ? "lastName" : sortBy;
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1), Sort.by(sortField).ascending());

        if (keyword == null || keyword.isBlank()) {
            return employeeRepository.findAll(pageable);
        }

        return employeeRepository
                .findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
                        keyword, keyword, keyword, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Employee> filter(String keyword, Integer departmentId, String status,
                                 int page, int size, String sortBy, boolean descending) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), sort(sortBy, descending));
        return employeeRepository.findAll(EmployeeSpecs.filter(keyword, departmentId, parseStatus(status)), pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Employee> filterAll(String keyword, Integer departmentId, String status,
                                    String sortBy, boolean descending) {
        return employeeRepository.findAll(EmployeeSpecs.filter(keyword, departmentId, parseStatus(status)),
                sort(sortBy, descending));
    }

    private static Sort sort(String sortBy, boolean descending) {
        String field = (sortBy != null && SORTABLE.contains(sortBy)) ? sortBy : "lastName";
        return descending ? Sort.by(field).descending() : Sort.by(field).ascending();
    }

    private static EmployeeStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return EmployeeStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Employee findById(int theId) {
        Optional<Employee> result = employeeRepository.findById(theId);

        if (result.isEmpty()) {
            throw new EmployeeNotFoundException("Did not find employee id - " + theId);
        }

        Employee employee = result.get();
        if (employee.getDepartment() != null) {
            employee.getDepartment().getName();
        }
        return employee;
    }

    @Override
    public void save(Employee theEmployee) {
        log.info("Saving employee: {}", theEmployee);
        employeeRepository.save(theEmployee);
    }

    @Override
    public void deleteById(int theId) {
        log.warn("Deleting employee with id: {}", theId);
        employeeRepository.deleteById(theId);
    }

    @Override
    public long countActive() {
        return employeeRepository.countByStatus(EmployeeStatus.ACTIVE);
    }

    @Override
    public long countInactive() {
        return employeeRepository.countByStatus(EmployeeStatus.INACTIVE);
    }

    @Override
    public long countTotal() {
        return employeeRepository.count();
    }

    @Override
    public Map<String, Long> countByDepartment() {
        Map<String, Long> result = new LinkedHashMap<>();
        List<Object[]> rows = employeeRepository.countEmployeesGroupedByDepartment();
        for (Object[] row : rows) {
            result.put((String) row[0], (Long) row[1]);
        }
        return result;
    }
}

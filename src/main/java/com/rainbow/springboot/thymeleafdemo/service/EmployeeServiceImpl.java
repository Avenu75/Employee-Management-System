package com.rainbow.springboot.thymeleafdemo.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.rainbow.springboot.thymeleafdemo.dao.EmployeeRepository;
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
    public Employee findById(int theId) {
        Optional<Employee> result = employeeRepository.findById(theId);

        if (result.isEmpty()) {
            // Thrown to a @ControllerAdvice handler which renders a friendly error page
            throw new EmployeeNotFoundException("Did not find employee id - " + theId);
        }

        return result.get();
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

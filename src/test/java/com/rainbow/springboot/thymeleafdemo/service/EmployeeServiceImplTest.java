package com.rainbow.springboot.thymeleafdemo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.rainbow.springboot.thymeleafdemo.dao.EmployeeRepository;
import com.rainbow.springboot.thymeleafdemo.entity.Employee;
import com.rainbow.springboot.thymeleafdemo.entity.EmployeeStatus;
import com.rainbow.springboot.thymeleafdemo.exception.EmployeeNotFoundException;

/**
 * Unit tests for EmployeeServiceImpl.
 *
 * These tests use Mockito to fake ("mock") the EmployeeRepository, so they
 * run fast and do NOT require a real MySQL database connection - they only
 * verify the business logic in the Service layer.
 */
@ExtendWith(MockitoExtension.class)
class EmployeeServiceImplTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @InjectMocks
    private EmployeeServiceImpl employeeService;

    private Employee sampleEmployee;

    @BeforeEach
    void setUp() {
        sampleEmployee = new Employee("Rachel", "Green", "rachel@company.com");
        sampleEmployee.setId(1);
        sampleEmployee.setSalary(new BigDecimal("3200.00"));
        sampleEmployee.setHireDate(LocalDate.of(2021, 3, 15));
        sampleEmployee.setStatus(EmployeeStatus.ACTIVE);
    }

    @Test
    void findById_returnsEmployee_whenFound() {
        when(employeeRepository.findById(1)).thenReturn(Optional.of(sampleEmployee));

        Employee result = employeeService.findById(1);

        assertEquals("Rachel", result.getFirstName());
    }

    @Test
    void findById_throwsException_whenNotFound() {
        when(employeeRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(EmployeeNotFoundException.class, () -> employeeService.findById(999));
    }

    @Test
    void save_delegatesToRepository() {
        employeeService.save(sampleEmployee);

        verify(employeeRepository).save(sampleEmployee);
    }

    @Test
    void deleteById_delegatesToRepository() {
        employeeService.deleteById(1);

        verify(employeeRepository).deleteById(1);
    }

    @Test
    void findAll_returnsListSortedByLastName() {
        when(employeeRepository.findAllByOrderByLastNameAsc()).thenReturn(List.of(sampleEmployee));

        List<Employee> result = employeeService.findAll();

        assertEquals(1, result.size());
        assertEquals("Green", result.get(0).getLastName());
    }

    @Test
    void countActive_delegatesToRepository() {
        when(employeeRepository.countByStatus(EmployeeStatus.ACTIVE)).thenReturn(5L);

        assertEquals(5L, employeeService.countActive());
    }
}

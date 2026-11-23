package com.rainbow.springboot.thymeleafdemo.dao;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.rainbow.springboot.thymeleafdemo.entity.Employee;
import com.rainbow.springboot.thymeleafdemo.entity.EmployeeStatus;

/**
 * Data-access layer for Employee.
 *
 * Spring Data JPA automatically implements this interface at runtime -
 * we only need to declare the method signatures; Spring generates the SQL.
 */
public interface EmployeeRepository extends JpaRepository<Employee, Integer>, JpaSpecificationExecutor<Employee> {

    @Override
    @EntityGraph(attributePaths = {"department"})
    Optional<Employee> findById(Integer id);

    @Override
    @EntityGraph(attributePaths = {"department"})
    Page<Employee> findAll(Pageable pageable);

    /** Filtered, paged list (keyword / department / status); loads the department in the same query. */
    @Override
    @EntityGraph(attributePaths = {"department"})
    Page<Employee> findAll(Specification<Employee> spec, Pageable pageable);

    /** Filtered, unpaged list, used by the CSV and PDF exports. */
    @Override
    @EntityGraph(attributePaths = {"department"})
    List<Employee> findAll(Specification<Employee> spec, Sort sort);

    // Kept for backward compatibility with the original tutorial project
    @EntityGraph(attributePaths = {"department"})
    List<Employee> findAllByOrderByLastNameAsc();

    /**
     * Search employees by first name, last name or email (case-insensitive),
     * with paging and sorting support (used by the "search" box on the employee list page).
     */
    @EntityGraph(attributePaths = {"department"})
    Page<Employee> findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
            String firstName, String lastName, String email, Pageable pageable);

    /** Used by the dashboard to show "Total active employees". */
    long countByStatus(EmployeeStatus status);

    /** Used by the dashboard chart: number of employees per department. */
    @Query("SELECT e.department.name, COUNT(e) FROM Employee e WHERE e.department IS NOT NULL GROUP BY e.department.name")
    List<Object[]> countEmployeesGroupedByDepartment();
}

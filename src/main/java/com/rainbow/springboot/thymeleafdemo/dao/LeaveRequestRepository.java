package com.rainbow.springboot.thymeleafdemo.dao;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.rainbow.springboot.thymeleafdemo.entity.Employee;
import com.rainbow.springboot.thymeleafdemo.entity.LeaveRequest;
import com.rainbow.springboot.thymeleafdemo.entity.LeaveStatus;
import com.rainbow.springboot.thymeleafdemo.entity.LeaveType;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Integer> {

    @EntityGraph(attributePaths = {"employee", "reviewedBy"})
    List<LeaveRequest> findByEmployeeOrderByStartDateDesc(Employee employee);

    @EntityGraph(attributePaths = {"employee", "employee.department"})
    List<LeaveRequest> findByStatusOrderByCreatedAtAsc(LeaveStatus status);

    @EntityGraph(attributePaths = {"employee", "employee.department", "reviewedBy"})
    List<LeaveRequest> findTop50ByStatusInOrderByReviewedAtDesc(Collection<LeaveStatus> statuses);

    long countByStatus(LeaveStatus status);

    /** Requests of one employee, in the given statuses, that touch the date range. */
    @Query("select l from LeaveRequest l where l.employee = :employee and l.status in :statuses "
            + "and l.startDate <= :end and l.endDate >= :start")
    List<LeaveRequest> findOverlapping(@Param("employee") Employee employee,
                                       @Param("statuses") Collection<LeaveStatus> statuses,
                                       @Param("start") LocalDate start,
                                       @Param("end") LocalDate end);

    /** Total working days for one employee, type and status, for requests starting in [from, to]. Null when there are none. */
    @Query("select sum(l.days) from LeaveRequest l where l.employee = :employee "
            + "and l.leaveType = :type and l.status = :status and l.startDate >= :from and l.startDate <= :to")
    Long sumDays(@Param("employee") Employee employee,
                 @Param("type") LeaveType type,
                 @Param("status") LeaveStatus status,
                 @Param("from") LocalDate from,
                 @Param("to") LocalDate to);

    /** Approved requests of one type that touch the date range (used by payroll for unpaid leave). */
    @EntityGraph(attributePaths = {"employee"})
    @Query("select l from LeaveRequest l where l.status = :status and l.leaveType = :type "
            + "and l.startDate <= :end and l.endDate >= :start")
    List<LeaveRequest> findByTypeAndStatusOverlapping(@Param("type") LeaveType type,
                                                      @Param("status") LeaveStatus status,
                                                      @Param("start") LocalDate start,
                                                      @Param("end") LocalDate end);
}

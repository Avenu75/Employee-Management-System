package com.rainbow.springboot.thymeleafdemo.dao;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.rainbow.springboot.thymeleafdemo.entity.Attendance;
import com.rainbow.springboot.thymeleafdemo.entity.AttendanceStatus;
import com.rainbow.springboot.thymeleafdemo.entity.Employee;

public interface AttendanceRepository extends JpaRepository<Attendance, Integer> {

    List<Attendance> findByAttendanceDateOrderByEmployeeLastNameAsc(LocalDate date);

    List<Attendance> findByEmployeeOrderByAttendanceDateDesc(Employee employee);

    Optional<Attendance> findByEmployeeAndAttendanceDate(Employee employee, LocalDate date);

    long countByAttendanceDateAndStatus(LocalDate date, AttendanceStatus status);
}

package com.rainbow.springboot.thymeleafdemo.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.rainbow.springboot.thymeleafdemo.entity.Attendance;

public interface AttendanceService {

    List<Attendance> findByDate(LocalDate date);

    List<Attendance> findByEmployee(int employeeId);

    /** Creates a record if none exists for that employee+date yet, otherwise updates the existing one. */
    void recordAttendance(int employeeId, LocalDate date, String status, String note);

    /** status -> count, for the given date - used on the attendance page's summary cards. */
    Map<String, Long> summaryForDate(LocalDate date);
}

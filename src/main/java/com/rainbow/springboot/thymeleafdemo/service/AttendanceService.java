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

    /** Today's record for an employee, or null if they have not clocked in and none was entered. */
    Attendance findToday(int employeeId);

    /** The employee's most recent records (up to 31), newest first. */
    List<Attendance> recent(int employeeId);

    /** Records the check-in time now. PRESENT, or LATE after the work start time plus grace period. */
    Attendance clockIn(int employeeId);

    /** Records the check-out time now. Requires an earlier check-in today. */
    Attendance clockOut(int employeeId);
}

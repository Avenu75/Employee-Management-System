package com.rainbow.springboot.thymeleafdemo.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rainbow.springboot.thymeleafdemo.dao.AttendanceRepository;
import com.rainbow.springboot.thymeleafdemo.dao.EmployeeRepository;
import com.rainbow.springboot.thymeleafdemo.entity.Attendance;
import com.rainbow.springboot.thymeleafdemo.entity.AttendanceStatus;
import com.rainbow.springboot.thymeleafdemo.entity.Employee;
import com.rainbow.springboot.thymeleafdemo.exception.EmployeeNotFoundException;

@Service
public class AttendanceServiceImpl implements AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;

    /** Work start time (HH:mm) and grace period; a check-in after start + grace is recorded as LATE. */
    @Value("${app.attendance.work-start:09:00}")
    private String workStart;

    @Value("${app.attendance.grace-minutes:15}")
    private int graceMinutes;

    public AttendanceServiceImpl(AttendanceRepository theAttendanceRepository, EmployeeRepository theEmployeeRepository) {
        attendanceRepository = theAttendanceRepository;
        employeeRepository = theEmployeeRepository;
    }

    @Override
    public List<Attendance> findByDate(LocalDate date) {
        return attendanceRepository.findByAttendanceDateOrderByEmployeeLastNameAsc(date);
    }

    @Override
    public List<Attendance> findByEmployee(int employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new EmployeeNotFoundException("Did not find employee id - " + employeeId));
        return attendanceRepository.findByEmployeeOrderByAttendanceDateDesc(employee);
    }

    @Override
    public void recordAttendance(int employeeId, LocalDate date, String status, String note) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new EmployeeNotFoundException("Did not find employee id - " + employeeId));

        Optional<Attendance> existing = attendanceRepository.findByEmployeeAndAttendanceDate(employee, date);
        Attendance attendance = existing.orElseGet(Attendance::new);
        attendance.setEmployee(employee);
        attendance.setAttendanceDate(date);
        attendance.setStatus(AttendanceStatus.valueOf(status));
        attendance.setNote(note);

        attendanceRepository.save(attendance);
    }

    @Override
    public Map<String, Long> summaryForDate(LocalDate date) {
        Map<String, Long> summary = new LinkedHashMap<>();
        for (AttendanceStatus status : AttendanceStatus.values()) {
            summary.put(status.name(), attendanceRepository.countByAttendanceDateAndStatus(date, status));
        }
        return summary;
    }

    @Override
    @Transactional(readOnly = true)
    public Attendance findToday(int employeeId) {
        Employee employee = loadEmployee(employeeId);
        return attendanceRepository.findByEmployeeAndAttendanceDate(employee, LocalDate.now()).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Attendance> recent(int employeeId) {
        return attendanceRepository.findTop31ByEmployeeOrderByAttendanceDateDesc(loadEmployee(employeeId));
    }

    @Override
    @Transactional
    public Attendance clockIn(int employeeId) {
        Employee employee = loadEmployee(employeeId);
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now().withSecond(0).withNano(0);

        Attendance record = attendanceRepository.findByEmployeeAndAttendanceDate(employee, today).orElseGet(Attendance::new);
        if (record.getStatus() == AttendanceStatus.ON_LEAVE && record.getId() != 0) {
            throw new IllegalStateException("You are on approved leave today, so you cannot clock in.");
        }
        if (record.getCheckIn() != null) {
            throw new IllegalStateException("You already clocked in today at " + record.getCheckIn() + ".");
        }

        LocalTime lateAfter = LocalTime.parse(workStart).plusMinutes(graceMinutes);
        record.setEmployee(employee);
        record.setAttendanceDate(today);
        record.setCheckIn(now);
        record.setStatus(now.isAfter(lateAfter) ? AttendanceStatus.LATE : AttendanceStatus.PRESENT);
        return attendanceRepository.save(record);
    }

    @Override
    @Transactional
    public Attendance clockOut(int employeeId) {
        Employee employee = loadEmployee(employeeId);
        Attendance record = attendanceRepository.findByEmployeeAndAttendanceDate(employee, LocalDate.now()).orElse(null);
        if (record == null || record.getCheckIn() == null) {
            throw new IllegalStateException("Clock in first, then you can clock out.");
        }
        if (record.getCheckOut() != null) {
            throw new IllegalStateException("You already clocked out today at " + record.getCheckOut() + ".");
        }
        LocalTime now = LocalTime.now().withSecond(0).withNano(0);
        if (now.isBefore(record.getCheckIn())) {
            now = record.getCheckIn();
        }
        record.setCheckOut(now);
        return attendanceRepository.save(record);
    }

    private Employee loadEmployee(int employeeId) {
        return employeeRepository.findById(employeeId)
                .orElseThrow(() -> new EmployeeNotFoundException("Did not find employee id - " + employeeId));
    }
}

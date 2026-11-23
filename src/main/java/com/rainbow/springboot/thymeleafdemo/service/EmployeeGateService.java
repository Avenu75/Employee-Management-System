package com.rainbow.springboot.thymeleafdemo.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rainbow.springboot.thymeleafdemo.dao.AttendanceRepository;
import com.rainbow.springboot.thymeleafdemo.dao.EmployeeRepository;
import com.rainbow.springboot.thymeleafdemo.dto.GateRow;
import com.rainbow.springboot.thymeleafdemo.entity.Attendance;
import com.rainbow.springboot.thymeleafdemo.entity.AttendanceStatus;
import com.rainbow.springboot.thymeleafdemo.entity.Employee;
import com.rainbow.springboot.thymeleafdemo.entity.EmployeeStatus;
import com.rainbow.springboot.thymeleafdemo.exception.EmployeeNotFoundException;

/**
 * Reception-gate attendance for EMPLOYEES. It writes the same Attendance records that
 * employees create themselves with My attendance, so reports and payroll stay consistent.
 */
@Service
public class EmployeeGateService {

    private final AttendanceRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;

    @Value("${app.attendance.work-start:09:00}")
    private String workStart;

    @Value("${app.attendance.grace-minutes:15}")
    private int graceMinutes;

    public EmployeeGateService(AttendanceRepository theAttendanceRepository, EmployeeRepository theEmployeeRepository) {
        attendanceRepository = theAttendanceRepository;
        employeeRepository = theEmployeeRepository;
    }

    /** Every active employee with that day's record (if any). */
    @Transactional(readOnly = true)
    public List<GateRow> board(LocalDate date) {
        Map<Integer, Attendance> byEmployee = new HashMap<>();
        for (Attendance record : attendanceRepository.findByAttendanceDateOrderByEmployeeLastNameAsc(date)) {
            byEmployee.put(record.getEmployee().getId(), record);
        }

        List<GateRow> rows = new ArrayList<>();
        for (Employee employee : employeeRepository.findAllByOrderByLastNameAsc()) {
            if (employee.getStatus() != EmployeeStatus.ACTIVE) {
                continue;
            }
            Attendance r = byEmployee.get(employee.getId());
            String state = "NONE";
            if (r != null) {
                if (r.getStatus() == AttendanceStatus.ON_LEAVE) {
                    state = "LEAVE";
                } else if (r.getStatus() == AttendanceStatus.ABSENT && r.getCheckIn() == null) {
                    state = "ABSENT";
                } else if (r.getCheckOut() != null) {
                    state = "OUT";
                } else if (r.getCheckIn() != null) {
                    state = "IN";
                }
            }
            rows.add(new GateRow(employee.getId(), employee.getFullName(), employee.getJobTitle(), state,
                    r != null && r.getStatus() == AttendanceStatus.LATE,
                    r == null ? null : r.getCheckIn(), r == null ? null : r.getCheckOut()));
        }
        return rows;
    }

    @Transactional
    public void checkIn(int employeeId) {
        Employee employee = load(employeeId);
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now().withSecond(0).withNano(0);

        Attendance record = attendanceRepository.findByEmployeeAndAttendanceDate(employee, today).orElseGet(Attendance::new);
        if (record.getId() != 0 && record.getStatus() == AttendanceStatus.ON_LEAVE) {
            throw new IllegalStateException(employee.getFullName() + " is on approved leave today.");
        }
        if (record.getCheckIn() != null) {
            throw new IllegalStateException(employee.getFullName() + " already checked in at " + record.getCheckIn() + ".");
        }
        LocalTime lateAfter = LocalTime.parse(workStart).plusMinutes(graceMinutes);
        record.setEmployee(employee);
        record.setAttendanceDate(today);
        record.setCheckIn(now);
        record.setCheckOut(null);
        record.setStatus(now.isAfter(lateAfter) ? AttendanceStatus.LATE : AttendanceStatus.PRESENT);
        attendanceRepository.save(record);
    }

    @Transactional
    public void checkOut(int employeeId) {
        Employee employee = load(employeeId);
        Attendance record = attendanceRepository.findByEmployeeAndAttendanceDate(employee, LocalDate.now()).orElse(null);
        if (record == null || record.getCheckIn() == null) {
            throw new IllegalStateException(employee.getFullName() + " has not checked in yet.");
        }
        if (record.getCheckOut() != null) {
            throw new IllegalStateException(employee.getFullName() + " already checked out at " + record.getCheckOut() + ".");
        }
        LocalTime now = LocalTime.now().withSecond(0).withNano(0);
        record.setCheckOut(now.isBefore(record.getCheckIn()) ? record.getCheckIn() : now);
        attendanceRepository.save(record);
    }

    @Transactional
    public void markAbsent(int employeeId) {
        Employee employee = load(employeeId);
        LocalDate today = LocalDate.now();
        Attendance record = attendanceRepository.findByEmployeeAndAttendanceDate(employee, today).orElseGet(Attendance::new);
        if (record.getCheckIn() != null) {
            throw new IllegalStateException(employee.getFullName() + " already checked in today.");
        }
        if (record.getId() != 0 && record.getStatus() == AttendanceStatus.ON_LEAVE) {
            throw new IllegalStateException(employee.getFullName() + " is on approved leave today.");
        }
        record.setEmployee(employee);
        record.setAttendanceDate(today);
        record.setStatus(AttendanceStatus.ABSENT);
        attendanceRepository.save(record);
    }

    /** Undo a wrong entry for today (approved-leave records are left alone). */
    @Transactional
    public void clearToday(int employeeId) {
        Employee employee = load(employeeId);
        attendanceRepository.findByEmployeeAndAttendanceDate(employee, LocalDate.now()).ifPresent(record -> {
            if (record.getStatus() == AttendanceStatus.ON_LEAVE) {
                throw new IllegalStateException("That entry comes from approved leave and cannot be cleared here.");
            }
            attendanceRepository.delete(record);
        });
    }

    private Employee load(int employeeId) {
        return employeeRepository.findById(employeeId)
                .orElseThrow(() -> new EmployeeNotFoundException("Did not find employee id - " + employeeId));
    }
}

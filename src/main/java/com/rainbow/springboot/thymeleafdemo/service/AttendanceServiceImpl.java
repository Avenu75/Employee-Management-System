package com.rainbow.springboot.thymeleafdemo.service;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;

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
}

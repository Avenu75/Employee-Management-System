package com.rainbow.springboot.thymeleafdemo.service;

import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rainbow.springboot.thymeleafdemo.dao.AttendanceRepository;
import com.rainbow.springboot.thymeleafdemo.dto.ClockReminder;
import com.rainbow.springboot.thymeleafdemo.entity.AppUser;
import com.rainbow.springboot.thymeleafdemo.entity.Attendance;
import com.rainbow.springboot.thymeleafdemo.entity.AttendanceStatus;
import com.rainbow.springboot.thymeleafdemo.entity.EmployeeStatus;
import com.rainbow.springboot.thymeleafdemo.util.WorkDays;

/** Decides whether to show the "remember to clock in / out" banner to a signed-in employee. */
@Service
public class ClockReminderService {

    private final AttendanceRepository attendanceRepository;

    public ClockReminderService(AttendanceRepository theAttendanceRepository) {
        attendanceRepository = theAttendanceRepository;
    }

    @Transactional(readOnly = true)
    public ClockReminder reminderFor(AppUser user) {
        try {
            if (user == null || user.getEmployee() == null) {
                return null;
            }
            LocalDate today = LocalDate.now();
            if (!WorkDays.isWorkingDay(today) || user.getEmployee().getStatus() != EmployeeStatus.ACTIVE) {
                return null;
            }
            Attendance record = attendanceRepository.findByEmployeeAndAttendanceDate(user.getEmployee(), today).orElse(null);
            if (record != null && record.getStatus() == AttendanceStatus.ON_LEAVE) {
                return null;
            }
            if (record == null || record.getCheckIn() == null) {
                return new ClockReminder("Welcome back. Please clock in to record that you have started work today.", false);
            }
            if (record.getCheckOut() == null) {
                return new ClockReminder("You clocked in at " + record.getCheckIn()
                        + ". Remember to clock out when you close from work.", true);
            }
            return null;
        } catch (RuntimeException ex) {
            return null; // a banner must never break a page
        }
    }
}

package com.rainbow.springboot.thymeleafdemo.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rainbow.springboot.thymeleafdemo.dao.AttendanceRepository;
import com.rainbow.springboot.thymeleafdemo.dto.PresenceEntry;
import com.rainbow.springboot.thymeleafdemo.entity.AppUser;
import com.rainbow.springboot.thymeleafdemo.entity.Attendance;

/**
 * Shows employees whether management is in the office, based on managers' own clock in and out.
 * The answer is cached for 30 seconds because it is shown on every page.
 */
@Service
public class ManagementPresenceService {

    private static final long CACHE_MILLIS = 30_000;

    private final CommunicationAccess access;
    private final AttendanceRepository attendanceRepository;

    private List<PresenceEntry> cached = Collections.emptyList();
    private long cachedAt;
    private LocalDate cachedDay;

    public ManagementPresenceService(CommunicationAccess theAccess, AttendanceRepository theAttendanceRepository) {
        access = theAccess;
        attendanceRepository = theAttendanceRepository;
    }

    @Transactional(readOnly = true)
    public synchronized List<PresenceEntry> today() {
        long now = System.currentTimeMillis();
        LocalDate today = LocalDate.now();
        if (today.equals(cachedDay) && now - cachedAt < CACHE_MILLIS) {
            return cached;
        }

        List<PresenceEntry> entries = new ArrayList<>();
        for (AppUser manager : access.managers()) {
            if (manager.getEmployee() == null) {
                continue; // no employee record, so no clock in
            }
            Attendance record = attendanceRepository.findByEmployeeAndAttendanceDate(manager.getEmployee(), today).orElse(null);
            boolean in = record != null && record.getCheckIn() != null && record.getCheckOut() == null;
            entries.add(new PresenceEntry(manager.getFullName(), record == null ? null : record.getCheckIn(), in));
        }
        cached = entries;
        cachedAt = now;
        cachedDay = today;
        return cached;
    }
}

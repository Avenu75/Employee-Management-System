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

import com.rainbow.springboot.thymeleafdemo.dao.OtherWorkerRepository;
import com.rainbow.springboot.thymeleafdemo.dao.WorkerAttendanceRepository;
import com.rainbow.springboot.thymeleafdemo.dto.GateRow;
import com.rainbow.springboot.thymeleafdemo.entity.AppUser;
import com.rainbow.springboot.thymeleafdemo.entity.AttendanceStatus;
import com.rainbow.springboot.thymeleafdemo.entity.EmployeeStatus;
import com.rainbow.springboot.thymeleafdemo.entity.OtherWorker;
import com.rainbow.springboot.thymeleafdemo.entity.WorkerAttendance;
import com.rainbow.springboot.thymeleafdemo.exception.OtherWorkerNotFoundException;

/** Reception-gate attendance for other workers. */
@Service
public class WorkerAttendanceService {

    private final WorkerAttendanceRepository attendanceRepository;
    private final OtherWorkerRepository workerRepository;

    @Value("${app.attendance.work-start:09:00}")
    private String workStart;

    @Value("${app.attendance.grace-minutes:15}")
    private int graceMinutes;

    public WorkerAttendanceService(WorkerAttendanceRepository theAttendanceRepository,
                                   OtherWorkerRepository theWorkerRepository) {
        attendanceRepository = theAttendanceRepository;
        workerRepository = theWorkerRepository;
    }

    /** Every active worker with that day's record (if any). */
    @Transactional(readOnly = true)
    public List<GateRow> board(LocalDate date) {
        Map<Integer, WorkerAttendance> byWorker = new HashMap<>();
        for (WorkerAttendance record : attendanceRepository.findByAttendanceDate(date)) {
            byWorker.put(record.getOtherWorker().getId(), record);
        }

        List<GateRow> rows = new ArrayList<>();
        for (OtherWorker worker : workerRepository.findAllByOrderByFullNameAsc()) {
            if (worker.getStatus() != EmployeeStatus.ACTIVE) {
                continue;
            }
            WorkerAttendance r = byWorker.get(worker.getId());
            String state = "NONE";
            if (r != null) {
                if (r.getStatus() == AttendanceStatus.ABSENT) {
                    state = "ABSENT";
                } else if (r.getCheckOut() != null) {
                    state = "OUT";
                } else if (r.getCheckIn() != null) {
                    state = "IN";
                }
            }
            rows.add(new GateRow(worker.getId(), worker.getFullName(), worker.getRoleType(), state,
                    r != null && r.getStatus() == AttendanceStatus.LATE,
                    r == null ? null : r.getCheckIn(), r == null ? null : r.getCheckOut()));
        }
        return rows;
    }

    @Transactional
    public void checkIn(int workerId, AppUser recordedBy) {
        OtherWorker worker = load(workerId);
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now().withSecond(0).withNano(0);

        WorkerAttendance record = attendanceRepository.findByOtherWorkerAndAttendanceDate(worker, today)
                .orElseGet(WorkerAttendance::new);
        if (record.getCheckIn() != null) {
            throw new IllegalStateException(worker.getFullName() + " already checked in at " + record.getCheckIn() + ".");
        }
        LocalTime lateAfter = LocalTime.parse(workStart).plusMinutes(graceMinutes);
        record.setOtherWorker(worker);
        record.setAttendanceDate(today);
        record.setCheckIn(now);
        record.setCheckOut(null);
        record.setStatus(now.isAfter(lateAfter) ? AttendanceStatus.LATE : AttendanceStatus.PRESENT);
        record.setRecordedBy(recordedBy);
        attendanceRepository.save(record);
    }

    @Transactional
    public void checkOut(int workerId, AppUser recordedBy) {
        OtherWorker worker = load(workerId);
        WorkerAttendance record = attendanceRepository.findByOtherWorkerAndAttendanceDate(worker, LocalDate.now()).orElse(null);
        if (record == null || record.getCheckIn() == null) {
            throw new IllegalStateException(worker.getFullName() + " has not checked in yet.");
        }
        if (record.getCheckOut() != null) {
            throw new IllegalStateException(worker.getFullName() + " already checked out at " + record.getCheckOut() + ".");
        }
        LocalTime now = LocalTime.now().withSecond(0).withNano(0);
        record.setCheckOut(now.isBefore(record.getCheckIn()) ? record.getCheckIn() : now);
        record.setRecordedBy(recordedBy);
        attendanceRepository.save(record);
    }

    @Transactional
    public void markAbsent(int workerId, AppUser recordedBy) {
        OtherWorker worker = load(workerId);
        LocalDate today = LocalDate.now();
        WorkerAttendance record = attendanceRepository.findByOtherWorkerAndAttendanceDate(worker, today)
                .orElseGet(WorkerAttendance::new);
        if (record.getCheckIn() != null) {
            throw new IllegalStateException(worker.getFullName() + " already checked in today.");
        }
        record.setOtherWorker(worker);
        record.setAttendanceDate(today);
        record.setStatus(AttendanceStatus.ABSENT);
        record.setRecordedBy(recordedBy);
        attendanceRepository.save(record);
    }

    /** Undo a wrong entry for today. */
    @Transactional
    public void clearToday(int workerId) {
        OtherWorker worker = load(workerId);
        attendanceRepository.findByOtherWorkerAndAttendanceDate(worker, LocalDate.now())
                .ifPresent(attendanceRepository::delete);
    }

    private OtherWorker load(int workerId) {
        return workerRepository.findById(workerId)
                .orElseThrow(() -> new OtherWorkerNotFoundException("Did not find worker id - " + workerId));
    }

}

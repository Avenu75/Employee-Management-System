package com.rainbow.springboot.thymeleafdemo.entity;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/** Daily attendance of an "other worker", recorded by reception at the gate. */
@Entity
@Table(name = "worker_attendance",
        uniqueConstraints = @UniqueConstraint(name = "uq_worker_attendance", columnNames = {"other_worker_id", "attendance_date"}))
public class WorkerAttendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "other_worker_id", nullable = false)
    private OtherWorker otherWorker;

    @Column(name = "attendance_date", nullable = false)
    private LocalDate attendanceDate;

    @Column(name = "check_in")
    private LocalTime checkIn;

    @Column(name = "check_out")
    private LocalTime checkOut;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AttendanceStatus status = AttendanceStatus.PRESENT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recorded_by_user_id")
    private AppUser recordedBy;

    public WorkerAttendance() {
    }

    public int getId() { return id; }
    public OtherWorker getOtherWorker() { return otherWorker; }
    public void setOtherWorker(OtherWorker otherWorker) { this.otherWorker = otherWorker; }
    public LocalDate getAttendanceDate() { return attendanceDate; }
    public void setAttendanceDate(LocalDate attendanceDate) { this.attendanceDate = attendanceDate; }
    public LocalTime getCheckIn() { return checkIn; }
    public void setCheckIn(LocalTime checkIn) { this.checkIn = checkIn; }
    public LocalTime getCheckOut() { return checkOut; }
    public void setCheckOut(LocalTime checkOut) { this.checkOut = checkOut; }
    public AttendanceStatus getStatus() { return status; }
    public void setStatus(AttendanceStatus status) { this.status = status; }
    public AppUser getRecordedBy() { return recordedBy; }
    public void setRecordedBy(AppUser recordedBy) { this.recordedBy = recordedBy; }

    /** Hours and minutes between check-in and check-out, e.g. "7h 45m", or "-" when not finished. */
    public String getWorkedLabel() {
        if (checkIn == null || checkOut == null) {
            return "-";
        }
        long minutes = java.time.Duration.between(checkIn, checkOut).toMinutes();
        return (minutes / 60) + "h " + (minutes % 60) + "m";
    }
}

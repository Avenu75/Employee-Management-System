package com.rainbow.springboot.thymeleafdemo.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rainbow.springboot.thymeleafdemo.dao.AppUserRepository;
import com.rainbow.springboot.thymeleafdemo.dao.AttendanceRepository;
import com.rainbow.springboot.thymeleafdemo.dao.LeaveRequestRepository;
import com.rainbow.springboot.thymeleafdemo.entity.AppUser;
import com.rainbow.springboot.thymeleafdemo.entity.Attendance;
import com.rainbow.springboot.thymeleafdemo.entity.AttendanceStatus;
import com.rainbow.springboot.thymeleafdemo.entity.Employee;
import com.rainbow.springboot.thymeleafdemo.entity.LeaveRequest;
import com.rainbow.springboot.thymeleafdemo.entity.LeaveStatus;
import com.rainbow.springboot.thymeleafdemo.entity.LeaveType;
import com.rainbow.springboot.thymeleafdemo.security.PermissionCode;
import com.rainbow.springboot.thymeleafdemo.util.WorkDays;

/**
 * Leave workflow: request, approve or reject, cancel, and annual-leave balance.
 *
 * Business rules are enforced here (not in the controller) and reported with
 * IllegalArgumentException / IllegalStateException carrying a user-friendly message.
 */
@Service
public class LeaveService {

    private final LeaveRequestRepository leaveRepository;
    private final AttendanceRepository attendanceRepository;
    private final AppUserRepository appUserRepository;
    private final NotificationService notificationService;

    @Value("${app.leave.annual-days:21}")
    private int annualAllowance;

    public LeaveService(LeaveRequestRepository theLeaveRepository,
                        AttendanceRepository theAttendanceRepository,
                        AppUserRepository theAppUserRepository,
                        NotificationService theNotificationService) {
        leaveRepository = theLeaveRepository;
        attendanceRepository = theAttendanceRepository;
        appUserRepository = theAppUserRepository;
        notificationService = theNotificationService;
    }

    /** Annual-leave position of one employee for one calendar year. */
    public static final class Balance {
        private final int allowance;
        private final int used;
        private final int pending;

        public Balance(int allowance, int used, int pending) {
            this.allowance = allowance;
            this.used = used;
            this.pending = pending;
        }

        public int getAllowance() { return allowance; }
        public int getUsed() { return used; }
        public int getPending() { return pending; }
        public int getRemaining() { return allowance - used - pending; }
    }

    @Transactional(readOnly = true)
    public Balance balance(Employee employee, int year) {
        LocalDate from = LocalDate.of(year, 1, 1);
        LocalDate to = LocalDate.of(year, 12, 31);
        int used = orZero(leaveRepository.sumDays(employee, LeaveType.ANNUAL, LeaveStatus.APPROVED, from, to));
        int pending = orZero(leaveRepository.sumDays(employee, LeaveType.ANNUAL, LeaveStatus.PENDING, from, to));
        return new Balance(annualAllowance, used, pending);
    }

    private static int orZero(Long value) {
        return value == null ? 0 : value.intValue();
    }

    @Transactional(readOnly = true)
    public List<LeaveRequest> history(Employee employee) {
        return leaveRepository.findByEmployeeOrderByStartDateDesc(employee);
    }

    @Transactional(readOnly = true)
    public List<LeaveRequest> pending() {
        return leaveRepository.findByStatusOrderByCreatedAtAsc(LeaveStatus.PENDING);
    }

    @Transactional(readOnly = true)
    public List<LeaveRequest> recentlyDecided() {
        return leaveRepository.findTop50ByStatusInOrderByReviewedAtDesc(
                EnumSet.of(LeaveStatus.APPROVED, LeaveStatus.REJECTED));
    }

    @Transactional
    public LeaveRequest request(Employee employee, LeaveType type, LocalDate start, LocalDate end, String reason) {
        if (employee == null) {
            throw new IllegalArgumentException("Your account is not linked to an employee record.");
        }
        if (type == null || start == null || end == null) {
            throw new IllegalArgumentException("Choose a leave type, a start date and an end date.");
        }
        if (end.isBefore(start)) {
            throw new IllegalArgumentException("The end date cannot be before the start date.");
        }
        if (type != LeaveType.SICK && start.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Only sick leave can start in the past.");
        }
        if (start.getYear() != end.getYear()) {
            throw new IllegalArgumentException("A request cannot span two calendar years. Please submit one request per year.");
        }
        int days = WorkDays.count(start, end);
        if (days == 0) {
            throw new IllegalArgumentException("That range contains no working days (Monday to Friday).");
        }
        String cleanReason = reason == null ? null : reason.trim();
        if (cleanReason != null && cleanReason.length() > 500) {
            throw new IllegalArgumentException("The reason must be at most 500 characters.");
        }

        List<LeaveRequest> clashes = leaveRepository.findOverlapping(employee,
                EnumSet.of(LeaveStatus.PENDING, LeaveStatus.APPROVED), start, end);
        if (!clashes.isEmpty()) {
            throw new IllegalArgumentException("You already have a pending or approved request that overlaps these dates.");
        }

        if (type == LeaveType.ANNUAL) {
            Balance balance = balance(employee, start.getYear());
            if (days > balance.getRemaining()) {
                throw new IllegalArgumentException("You only have " + Math.max(balance.getRemaining(), 0)
                        + " annual leave day(s) left for " + start.getYear() + " but this request needs " + days + ".");
            }
        }

        LeaveRequest leave = new LeaveRequest();
        leave.setEmployee(employee);
        leave.setLeaveType(type);
        leave.setStartDate(start);
        leave.setEndDate(end);
        leave.setDays(days);
        leave.setReason(cleanReason == null || cleanReason.isEmpty() ? null : cleanReason);
        leave.setStatus(LeaveStatus.PENDING);
        leaveRepository.save(leave);

        AppUser requester = appUserRepository.findFirstByEmployee(employee).orElse(null);
        notificationService.notifyHolders(PermissionCode.CAN_MANAGE_LEAVE,
                employee.getFullName() + " requested " + days + " day(s) of "
                        + type.getLabel().toLowerCase() + " (" + start + " to " + end + ").",
                "/leave/manage", requester);
        return leave;
    }

    @Transactional
    public void decide(int requestId, boolean approve, String comment, AppUser reviewer) {
        LeaveRequest leave = leaveRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("That leave request no longer exists."));

        if (leave.getStatus() != LeaveStatus.PENDING) {
            throw new IllegalStateException("This request has already been " + leave.getStatus().name().toLowerCase() + ".");
        }
        if (reviewer != null && !reviewer.isSuperAdmin() && reviewer.getEmployee() != null
                && reviewer.getEmployee().getId() == leave.getEmployee().getId()) {
            throw new IllegalStateException("You cannot approve or reject your own leave request.");
        }
        String cleanComment = comment == null ? "" : comment.trim();
        if (!approve && cleanComment.isEmpty()) {
            throw new IllegalArgumentException("Please give a reason when rejecting a request.");
        }
        if (cleanComment.length() > 500) {
            throw new IllegalArgumentException("The comment must be at most 500 characters.");
        }

        leave.setStatus(approve ? LeaveStatus.APPROVED : LeaveStatus.REJECTED);
        leave.setReviewedBy(reviewer);
        leave.setReviewedAt(LocalDateTime.now());
        leave.setReviewComment(cleanComment.isEmpty() ? null : cleanComment);
        leaveRepository.save(leave);

        if (approve) {
            markAttendanceOnLeave(leave);
        }

        String verdict = approve ? "approved" : "rejected";
        AppUser requesterUser = appUserRepository.findFirstByEmployee(leave.getEmployee()).orElse(null);
        notificationService.notify(requesterUser,
                "Your " + leave.getLeaveType().getLabel().toLowerCase() + " request (" + leave.getStartDate()
                        + " to " + leave.getEndDate() + ") was " + verdict + "."
                        + (leave.getReviewComment() != null ? " Comment: " + leave.getReviewComment() : ""),
                "/leave");
    }

    @Transactional
    public void cancel(int requestId, Employee employee) {
        LeaveRequest leave = leaveRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("That leave request no longer exists."));
        if (employee == null || leave.getEmployee().getId() != employee.getId()) {
            throw new IllegalStateException("You can only cancel your own requests.");
        }
        if (leave.getStatus() != LeaveStatus.PENDING) {
            throw new IllegalStateException("Only pending requests can be cancelled. Ask a manager about approved leave.");
        }
        leave.setStatus(LeaveStatus.CANCELLED);
        leaveRepository.save(leave);
    }

    /** Shows approved leave in the attendance register as ON_LEAVE for each working day. */
    private void markAttendanceOnLeave(LeaveRequest leave) {
        for (LocalDate d = leave.getStartDate(); !d.isAfter(leave.getEndDate()); d = d.plusDays(1)) {
            if (!WorkDays.isWorkingDay(d)) {
                continue;
            }
            Attendance record = attendanceRepository
                    .findByEmployeeAndAttendanceDate(leave.getEmployee(), d).orElseGet(Attendance::new);
            if (record.getCheckIn() != null) {
                continue; // the person actually worked that day; keep the real record
            }
            record.setEmployee(leave.getEmployee());
            record.setAttendanceDate(d);
            record.setStatus(AttendanceStatus.ON_LEAVE);
            record.setNote("Approved " + leave.getLeaveType().getLabel().toLowerCase());
            attendanceRepository.save(record);
        }
    }
}

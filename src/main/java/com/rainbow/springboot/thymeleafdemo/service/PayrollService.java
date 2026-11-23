package com.rainbow.springboot.thymeleafdemo.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rainbow.springboot.thymeleafdemo.dao.AttendanceRepository;
import com.rainbow.springboot.thymeleafdemo.dao.EmployeeRepository;
import com.rainbow.springboot.thymeleafdemo.dao.EmployeeSpecs;
import com.rainbow.springboot.thymeleafdemo.dao.LeaveRequestRepository;
import com.rainbow.springboot.thymeleafdemo.dao.PayrollAdjustmentRepository;
import com.rainbow.springboot.thymeleafdemo.dao.PayrollItemRepository;
import com.rainbow.springboot.thymeleafdemo.dao.PayrollRunRepository;
import com.rainbow.springboot.thymeleafdemo.dto.PayrollReadiness;
import com.rainbow.springboot.thymeleafdemo.dto.PayrollRow;
import com.rainbow.springboot.thymeleafdemo.dto.PayrollSummary;
import com.rainbow.springboot.thymeleafdemo.entity.Attendance;
import com.rainbow.springboot.thymeleafdemo.entity.AttendanceStatus;
import com.rainbow.springboot.thymeleafdemo.entity.Employee;
import com.rainbow.springboot.thymeleafdemo.entity.EmployeeStatus;
import com.rainbow.springboot.thymeleafdemo.entity.LeaveRequest;
import com.rainbow.springboot.thymeleafdemo.entity.LeaveStatus;
import com.rainbow.springboot.thymeleafdemo.entity.LeaveType;
import com.rainbow.springboot.thymeleafdemo.entity.PayrollAdjustment;
import com.rainbow.springboot.thymeleafdemo.entity.PayrollCalcMethod;
import com.rainbow.springboot.thymeleafdemo.entity.PayrollItem;
import com.rainbow.springboot.thymeleafdemo.entity.PayrollItemKind;
import com.rainbow.springboot.thymeleafdemo.entity.PayrollLine;
import com.rainbow.springboot.thymeleafdemo.entity.PayrollLineItem;
import com.rainbow.springboot.thymeleafdemo.entity.PayrollRun;
import com.rainbow.springboot.thymeleafdemo.entity.PayrollRunStatus;
import com.rainbow.springboot.thymeleafdemo.util.PayrollCalculator;
import com.rainbow.springboot.thymeleafdemo.util.WorkDays;

/**
 * Payroll in three steps:
 *  1. SET UP the rules (PayrollItem: tax, pension, dues, allowances) and the one-off
 *     adjustments for the month (PayrollAdjustment: advances, loan repayments, bonuses);
 *  2. GENERATE the month: every figure is worked out and saved as a DRAFT run;
 *  3. FINALISE it, which locks the month so it can never change.
 *
 * Until a month is generated the page shows a live PREVIEW that is not saved.
 * Unpaid days = approved unpaid leave + days recorded ABSENT + working days before
 * the hire date. The salary on the employee record is a MONTHLY gross salary.
 */
@Service
public class PayrollService {

    private final EmployeeRepository employeeRepository;
    private final AttendanceRepository attendanceRepository;
    private final LeaveRequestRepository leaveRepository;
    private final PayrollItemRepository itemRepository;
    private final PayrollAdjustmentRepository adjustmentRepository;
    private final PayrollRunRepository runRepository;
    private final AuditLogService auditLogService;

    @Value("${app.currency:$}")
    private String currency;

    public PayrollService(EmployeeRepository theEmployeeRepository,
                          AttendanceRepository theAttendanceRepository,
                          LeaveRequestRepository theLeaveRepository,
                          PayrollItemRepository theItemRepository,
                          PayrollAdjustmentRepository theAdjustmentRepository,
                          PayrollRunRepository theRunRepository,
                          AuditLogService theAuditLogService) {
        employeeRepository = theEmployeeRepository;
        attendanceRepository = theAttendanceRepository;
        leaveRepository = theLeaveRepository;
        itemRepository = theItemRepository;
        adjustmentRepository = theAdjustmentRepository;
        runRepository = theRunRepository;
        auditLogService = theAuditLogService;
    }

    public String getCurrency() {
        return currency;
    }

    // ------------------------------------------------------------------ viewing

    /** The saved payroll for the month if it has been generated, otherwise a live preview. */
    @Transactional(readOnly = true)
    public PayrollSummary view(YearMonth month) {
        Optional<PayrollRun> run = runRepository.findByMonth(month.toString());
        if (run.isPresent()) {
            return fromRun(run.get(), month);
        }
        return preview(month);
    }

    /** The saved payroll for the month, or empty when it has not been generated. */
    @Transactional(readOnly = true)
    public Optional<PayrollSummary> saved(YearMonth month) {
        return runRepository.findByMonth(month.toString()).map(r -> fromRun(r, month));
    }

    @Transactional(readOnly = true)
    public PayrollSummary preview(YearMonth month) {
        int workingDays = WorkDays.countInMonth(month);
        return new PayrollSummary(month, workingDays, null, null, null, null, null, buildRows(month, workingDays));
    }

    @Transactional(readOnly = true)
    public PayrollReadiness readiness(YearMonth month) {
        List<PayrollItem> active = itemRepository.findByActiveTrueOrderByKindAscNameAsc();
        List<Employee> employees = payableEmployees(month);
        List<String> noSalary = new ArrayList<>();
        for (Employee e : employees) {
            if (e.getSalary() == null || e.getSalary().signum() <= 0) {
                noSalary.add(e.getFullName());
            }
        }
        int adjustments = adjustmentRepository.findByMonthOrderByIdAsc(month.toString()).size();
        return new PayrollReadiness(active, adjustments, employees.size(), noSalary);
    }

    @Transactional(readOnly = true)
    public List<PayrollRun> recentRuns() {
        return runRepository.findTop24ByOrderByMonthDesc();
    }

    // --------------------------------------------------------------- generating

    /** Works out and saves the month. A DRAFT is replaced; a FINALISED month cannot be touched. */
    @Transactional
    public PayrollSummary generate(YearMonth month, String username) {
        Optional<PayrollRun> existing = runRepository.findByMonth(month.toString());
        if (existing.isPresent() && existing.get().getStatus() == PayrollRunStatus.FINALISED) {
            throw new IllegalStateException("Payroll for " + month + " is finalised and cannot be generated again.");
        }
        PayrollReadiness ready = readiness(month);
        if (ready.getActiveItems().isEmpty()) {
            throw new IllegalStateException("Set up at least one active payroll item (for example income tax) before generating payroll.");
        }
        if (ready.getEmployeeCount() == 0) {
            throw new IllegalStateException("There are no active employees to pay for " + month + ".");
        }
        if (existing.isPresent()) {
            runRepository.delete(existing.get());
            runRepository.flush();
        }

        int workingDays = WorkDays.countInMonth(month);
        List<PayrollRow> rows = buildRows(month, workingDays);

        PayrollRun run = new PayrollRun();
        run.setMonth(month.toString());
        run.setStatus(PayrollRunStatus.DRAFT);
        run.setWorkingDays(workingDays);
        run.setGeneratedBy(username);
        for (PayrollRow r : rows) {
            PayrollLine line = new PayrollLine();
            line.setEmployeeId(r.getEmployeeId());
            line.setEmployeeName(r.getName());
            line.setJobTitle(r.getJobTitle());
            line.setDepartment(r.getDepartment());
            line.setBase(r.getBase());
            line.setUnpaidDays(r.getUnpaidDays());
            line.setUnpaidDeduction(r.getUnpaidDeduction());
            line.setEarnings(r.getEarnings());
            line.setGross(r.getGross());
            line.setDeductions(r.getDeductions());
            line.setNet(r.getNet());
            for (PayrollRow.Item i : r.getItems()) {
                PayrollLineItem li = new PayrollLineItem();
                li.setLabel(i.getLabel());
                li.setKind(i.getKind());
                li.setBasis(i.getBasis());
                li.setAmount(i.getAmount());
                line.addItem(li);
            }
            run.addLine(line);
        }
        runRepository.save(run);
        auditLogService.record("PAYROLL_GENERATED", username, null,
                "Generated payroll for " + month + " (" + rows.size() + " employees, " + ready.getActiveItems().size()
                        + " payroll items, " + ready.getAdjustmentCount() + " adjustments)");
        return fromRun(run, month);
    }

    @Transactional
    public void finalise(YearMonth month, String username) {
        PayrollRun run = runRepository.findByMonth(month.toString())
                .orElseThrow(() -> new IllegalStateException("Generate payroll for " + month + " before finalising it."));
        if (run.getStatus() == PayrollRunStatus.FINALISED) {
            return;
        }
        run.setStatus(PayrollRunStatus.FINALISED);
        run.setFinalisedBy(username);
        run.setFinalisedAt(java.time.LocalDateTime.now());
        runRepository.save(run);
        auditLogService.record("PAYROLL_FINALISED", username, null, "Finalised payroll for " + month + " (locked)");
    }

    /** True when the month has been finalised, so its adjustments can no longer change. */
    @Transactional(readOnly = true)
    public boolean isFinalised(YearMonth month) {
        return runRepository.findByMonth(month.toString())
                .map(r -> r.getStatus() == PayrollRunStatus.FINALISED).orElse(false);
    }

    // ------------------------------------------------------------- payroll items

    @Transactional(readOnly = true)
    public List<PayrollItem> allItems() {
        return itemRepository.findAllByOrderByKindAscNameAsc();
    }

    @Transactional(readOnly = true)
    public PayrollItem findItem(int id) {
        PayrollItem item = itemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Payroll item not found: " + id));
        item.getEmployees().size(); // load for the form
        return item;
    }

    @Transactional
    public void saveItem(PayrollItem form, List<Integer> employeeIds, String username) {
        PayrollItem item = form.getId() == 0 ? new PayrollItem() : findItem(form.getId());
        item.setName(form.getName().trim());
        item.setKind(form.getKind());
        item.setCalcMethod(form.getCalcMethod());
        item.setAmountValue(form.getAmountValue());
        item.setAllEmployees(form.isAllEmployees());
        item.setActive(form.isActive());
        item.setNote(form.getNote() == null || form.getNote().isBlank() ? null : form.getNote().trim());
        item.getEmployees().clear();
        if (!item.isAllEmployees() && employeeIds != null) {
            for (Integer id : employeeIds) {
                employeeRepository.findById(id).ifPresent(item.getEmployees()::add);
            }
        }
        boolean isNew = item.getId() == 0;
        itemRepository.save(item);
        auditLogService.record(isNew ? "PAYROLL_ITEM_CREATED" : "PAYROLL_ITEM_CHANGED", username, null,
                item.getKind() + " \"" + item.getName() + "\" " + item.getDescribe()
                        + (item.getCalcMethod() == PayrollCalcMethod.PERCENT ? "" : " fixed")
                        + (item.isActive() ? "" : " (inactive)"));
    }

    @Transactional
    public void toggleItem(int id, String username) {
        PayrollItem item = itemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Payroll item not found: " + id));
        item.setActive(!item.isActive());
        itemRepository.save(item);
        auditLogService.record("PAYROLL_ITEM_TOGGLED", username, null,
                "\"" + item.getName() + "\" is now " + (item.isActive() ? "active" : "inactive"));
    }

    @Transactional
    public void deleteItem(int id, String username) {
        PayrollItem item = itemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Payroll item not found: " + id));
        itemRepository.delete(item);
        auditLogService.record("PAYROLL_ITEM_DELETED", username, null, "Deleted payroll item \"" + item.getName() + "\"");
    }

    // ---------------------------------------------------------------- adjustments

    @Transactional(readOnly = true)
    public List<PayrollAdjustment> adjustments(YearMonth month) {
        return adjustmentRepository.findByMonthOrderByIdAsc(month.toString());
    }

    @Transactional(readOnly = true)
    public List<Employee> payableEmployees(YearMonth month) {
        List<Employee> result = new ArrayList<>();
        for (Employee e : employeeRepository.findAll(
                EmployeeSpecs.filter(null, null, EmployeeStatus.ACTIVE), Sort.by("lastName", "firstName"))) {
            if (e.getHireDate() == null || !e.getHireDate().isAfter(month.atEndOfMonth())) {
                result.add(e);
            }
        }
        return result;
    }

    @Transactional
    public void addAdjustment(YearMonth month, int employeeId, String label, PayrollItemKind kind,
                              BigDecimal amount, String note, String username) {
        if (isFinalised(month)) {
            throw new IllegalStateException("Payroll for " + month + " is finalised; adjustments can no longer change.");
        }
        Employee e = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found."));
        PayrollAdjustment a = new PayrollAdjustment();
        a.setEmployee(e);
        a.setMonth(month.toString());
        a.setLabel(label.trim());
        a.setKind(kind);
        a.setAmount(amount);
        a.setNote(note == null || note.isBlank() ? null : note.trim());
        a.setCreatedBy(username);
        adjustmentRepository.save(a);
        auditLogService.record("PAYROLL_ADJUSTMENT_ADDED", username, null,
                kind + " \"" + a.getLabel() + "\" " + amount + " for " + e.getFullName() + " in " + month);
    }

    @Transactional
    public void deleteAdjustment(int id, String username) {
        PayrollAdjustment a = adjustmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Adjustment not found."));
        if (isFinalised(YearMonth.parse(a.getMonth()))) {
            throw new IllegalStateException("Payroll for " + a.getMonth() + " is finalised; adjustments can no longer change.");
        }
        adjustmentRepository.delete(a);
        auditLogService.record("PAYROLL_ADJUSTMENT_DELETED", username, null,
                "Removed \"" + a.getLabel() + "\" for " + a.getEmployee().getFullName() + " in " + a.getMonth());
    }

    // ------------------------------------------------------------------ internals

    private List<PayrollRow> buildRows(YearMonth month, int workingDays) {
        LocalDate start = month.atDay(1);
        LocalDate end = month.atEndOfMonth();

        Map<Integer, Integer> unpaidLeaveDays = new HashMap<>();
        for (LeaveRequest leave : leaveRepository.findByTypeAndStatusOverlapping(
                LeaveType.UNPAID, LeaveStatus.APPROVED, start, end)) {
            int days = WorkDays.overlap(leave.getStartDate(), leave.getEndDate(), start, end);
            unpaidLeaveDays.merge(leave.getEmployee().getId(), days, Integer::sum);
        }

        Map<Integer, Integer> absentDays = new HashMap<>();
        for (Attendance a : attendanceRepository.findByAttendanceDateBetween(start, end)) {
            if (a.getStatus() == AttendanceStatus.ABSENT && WorkDays.isWorkingDay(a.getAttendanceDate())) {
                absentDays.merge(a.getEmployee().getId(), 1, Integer::sum);
            }
        }

        List<PayrollItem> items = itemRepository.findByActiveTrueOrderByKindAscNameAsc();
        Map<Integer, List<PayrollAdjustment>> adjustmentsByEmployee = new HashMap<>();
        for (PayrollAdjustment a : adjustmentRepository.findByMonthOrderByIdAsc(month.toString())) {
            adjustmentsByEmployee.computeIfAbsent(a.getEmployee().getId(), k -> new ArrayList<>()).add(a);
        }

        List<PayrollRow> rows = new ArrayList<>();
        for (Employee e : payableEmployees(month)) {
            int unpaid = unpaidLeaveDays.getOrDefault(e.getId(), 0) + absentDays.getOrDefault(e.getId(), 0);
            if (e.getHireDate() != null && e.getHireDate().isAfter(start)) {
                unpaid += WorkDays.count(start, e.getHireDate().minusDays(1));
            }

            List<PayrollCalculator.Component> components = new ArrayList<>();
            for (PayrollItem item : items) {
                if (item.appliesTo(e.getId())) {
                    components.add(new PayrollCalculator.Component(item.getName(), item.getKind(),
                            item.getCalcMethod(), item.getAmountValue(), false));
                }
            }
            for (PayrollAdjustment a : adjustmentsByEmployee.getOrDefault(e.getId(), List.of())) {
                components.add(new PayrollCalculator.Component(a.getLabel(), a.getKind(),
                        PayrollCalcMethod.FIXED, a.getAmount(), true));
            }

            PayrollCalculator.Result r = PayrollCalculator.compute(e.getSalary(), workingDays, unpaid, components);
            String dept = e.getDepartment() != null ? e.getDepartment().getName() : "No department";

            List<PayrollRow.Item> lineItems = new ArrayList<>();
            for (PayrollCalculator.Line l : r.getLines()) {
                lineItems.add(new PayrollRow.Item(l.getLabel(), l.getKind(), l.getBasis(), l.getAmount()));
            }
            rows.add(new PayrollRow(e.getId(), e.getFullName(), e.getJobTitle(), dept, r.getBase(),
                    r.getUnpaidDays(), r.getUnpaidDeduction(), r.getEarnings(), r.getGross(),
                    r.getDeductions(), r.getNet(), lineItems));
        }
        return rows;
    }

    private PayrollSummary fromRun(PayrollRun run, YearMonth month) {
        List<PayrollRow> rows = new ArrayList<>();
        for (PayrollLine l : run.getLines()) {
            List<PayrollRow.Item> items = new ArrayList<>();
            for (PayrollLineItem i : l.getItems()) {
                items.add(new PayrollRow.Item(i.getLabel(), i.getKind(), i.getBasis(), i.getAmount()));
            }
            rows.add(new PayrollRow(l.getEmployeeId(), l.getEmployeeName(), l.getJobTitle(),
                    l.getDepartment() == null ? "No department" : l.getDepartment(), l.getBase(), l.getUnpaidDays(),
                    l.getUnpaidDeduction(), l.getEarnings(), l.getGross(), l.getDeductions(), l.getNet(), items));
        }
        return new PayrollSummary(month, run.getWorkingDays(), run.getStatus(), run.getGeneratedBy(),
                run.getGeneratedAt(), run.getFinalisedBy(), run.getFinalisedAt(), rows);
    }
}

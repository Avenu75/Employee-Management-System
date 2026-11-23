package com.rainbow.springboot.thymeleafdemo.service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;

import com.rainbow.springboot.thymeleafdemo.dto.PayrollItemTotal;
import com.rainbow.springboot.thymeleafdemo.dto.PayrollRow;
import com.rainbow.springboot.thymeleafdemo.dto.PayrollSummary;
import com.rainbow.springboot.thymeleafdemo.entity.Employee;

/**
 * Generates the PDF downloads (employee directory and monthly payroll).
 * Each method returns the finished file as bytes so a failure can never
 * leave a half-written PDF in the browser.
 */
@Service
public class PdfExportService {

    private static final Color INK = new Color(15, 23, 42);
    private static final Color TEAL = new Color(15, 118, 110);
    private static final Color STRIPE = new Color(241, 245, 249);
    private static final Color LINE = new Color(203, 213, 225);

    private static final Font TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, INK);
    private static final Font SUB = FontFactory.getFont(FontFactory.HELVETICA, 10, new Color(71, 85, 105));
    private static final Font HEAD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.5f, Color.WHITE);
    private static final Font BODY = FontFactory.getFont(FontFactory.HELVETICA, 8.5f, INK);
    private static final Font BOLD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.5f, INK);
    private static final Font SMALL = FontFactory.getFont(FontFactory.HELVETICA, 8, new Color(100, 116, 139));

    public byte[] employeesPdf(List<Employee> employees, String filterDescription) {
        List<String[]> rows = new ArrayList<>();
        for (Employee e : employees) {
            rows.add(new String[] {
                    e.getFullName(),
                    nz(e.getEmail()),
                    nz(e.getJobTitle()),
                    e.getDepartment() != null ? e.getDepartment().getName() : "-",
                    e.getHireDate() != null ? e.getHireDate().toString() : "-",
                    e.getStatus() != null ? e.getStatus().name() : "-"
            });
        }
        String sub = employees.size() + " employee(s)" + (filterDescription.isBlank() ? "" : "  |  Filter: " + filterDescription);
        return build("Employee Directory", sub,
                new String[] {"Name", "Email", "Job title", "Department", "Hired", "Status"},
                new float[] {20, 28, 17, 17, 10, 8}, new boolean[6], rows, null);
    }

    public byte[] payrollPdf(PayrollSummary s, String currency) {
        List<String[]> rows = new ArrayList<>();
        for (PayrollRow r : s.getRows()) {
            rows.add(new String[] {
                    r.getName(), r.getDepartment(), money(currency, r.getBase()), String.valueOf(r.getUnpaidDays()),
                    money(currency, r.getUnpaidDeduction()), money(currency, r.getEarnings()),
                    money(currency, r.getGross()), money(currency, r.getDeductions()), money(currency, r.getNet())
            });
        }
        String[] totals = {"TOTAL", "", money(currency, s.getTotalBase()), "", money(currency, s.getTotalUnpaid()),
                money(currency, s.getTotalEarnings()), money(currency, s.getTotalGross()),
                money(currency, s.getTotalDeductions()), money(currency, s.getTotalNet())};
        String month = s.getMonth().getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + s.getMonth().getYear();
        String status = s.getStatus() == null ? "PREVIEW (not generated)" : s.getStatus().name();

        StringBuilder sub = new StringBuilder();
        sub.append(s.getWorkingDays()).append(" working days  |  status: ").append(status);
        StringBuilder earn = new StringBuilder();
        StringBuilder ded = new StringBuilder();
        for (PayrollItemTotal t : s.getItemTotals()) {
            StringBuilder target = t.isEarning() ? earn : ded;
            if (target.length() > 0) {
                target.append(", ");
            }
            target.append(t.getLabel()).append(' ').append(money(currency, t.getAmount()));
        }
        if (earn.length() > 0) {
            sub.append("  |  Earnings: ").append(earn);
        }
        if (ded.length() > 0) {
            sub.append("  |  Deductions: ").append(ded);
        }
        return build("Payroll Summary - " + month, sub.toString(),
                new String[] {"Employee", "Department", "Base", "Unpaid days", "Unpaid deduction", "Earnings", "Gross", "Deductions", "Net pay"},
                new float[] {18, 14, 10, 8, 11, 10, 10, 10, 10},
                new boolean[] {false, false, true, true, true, true, true, true, true}, rows, totals);
    }

    private byte[] build(String title, String subtitle, String[] headers, float[] widths,
                         boolean[] rightAlign, List<String[]> rows, String[] totals) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4.rotate(), 36, 36, 36, 44);
        try {
            PdfWriter writer = PdfWriter.getInstance(doc, out);
            writer.setPageEvent(new PageNumbers());
            doc.open();

            doc.add(new Paragraph("VitalOps HR", SMALL));
            doc.add(new Paragraph(title, TITLE));
            Paragraph sub = new Paragraph(subtitle, SUB);
            sub.setSpacingAfter(2);
            doc.add(sub);
            Paragraph generated = new Paragraph("Generated "
                    + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")), SMALL);
            generated.setSpacingAfter(10);
            doc.add(generated);

            PdfPTable table = new PdfPTable(widths.length);
            table.setWidthPercentage(100);
            table.setWidths(widths);
            table.setHeaderRows(1);

            for (int i = 0; i < headers.length; i++) {
                PdfPCell c = new PdfPCell(new Phrase(headers[i], HEAD));
                c.setBackgroundColor(INK);
                c.setBorderColor(INK);
                c.setPadding(6);
                c.setHorizontalAlignment(rightAlign[i] ? Element.ALIGN_RIGHT : Element.ALIGN_LEFT);
                table.addCell(c);
            }

            if (rows.isEmpty()) {
                PdfPCell empty = new PdfPCell(new Phrase("No records.", BODY));
                empty.setColspan(widths.length);
                empty.setPadding(10);
                empty.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(empty);
            }

            int n = 0;
            for (String[] row : rows) {
                for (int i = 0; i < row.length; i++) {
                    table.addCell(bodyCell(row[i], BODY, rightAlign[i], n % 2 == 1 ? STRIPE : Color.WHITE));
                }
                n++;
            }
            if (totals != null) {
                for (int i = 0; i < totals.length; i++) {
                    PdfPCell c = bodyCell(totals[i], BOLD, rightAlign[i], new Color(204, 251, 241));
                    c.setBorderColorTop(TEAL);
                    table.addCell(c);
                }
            }
            doc.add(table);
            doc.close();
        } catch (DocumentException ex) {
            throw new IllegalStateException("Could not create the PDF: " + ex.getMessage(), ex);
        }
        return out.toByteArray();
    }

    private PdfPCell bodyCell(String text, Font font, boolean right, Color background) {
        PdfPCell c = new PdfPCell(new Phrase(text == null ? "" : text, font));
        c.setBackgroundColor(background);
        c.setBorderColor(LINE);
        c.setBorderWidth(0.5f);
        c.setPadding(5);
        c.setHorizontalAlignment(right ? Element.ALIGN_RIGHT : Element.ALIGN_LEFT);
        return c;
    }

    private static String money(String currency, BigDecimal value) {
        return currency + String.format(Locale.ENGLISH, "%,.2f", value);
    }

    private static String nz(String v) {
        return v == null ? "" : v;
    }

    /** Adds "Page N" at the bottom centre of every page. */
    private static class PageNumbers extends PdfPageEventHelper {
        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            Rectangle page = document.getPageSize();
            ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_CENTER,
                    new Phrase("Page " + writer.getPageNumber(), SMALL),
                    (page.getLeft() + page.getRight()) / 2, page.getBottom() + 22, 0);
        }
    }
}

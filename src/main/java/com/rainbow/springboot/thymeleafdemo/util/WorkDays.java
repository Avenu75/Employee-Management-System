package com.rainbow.springboot.thymeleafdemo.util;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Working-day arithmetic (Monday to Friday). Public holidays are not
 * modelled; see DEPLOYMENT.md for this documented limitation.
 */
public final class WorkDays {

    private WorkDays() {
    }

    public static boolean isWorkingDay(LocalDate date) {
        DayOfWeek day = date.getDayOfWeek();
        return day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY;
    }

    /** Working days from "from" to "to", both inclusive. Returns 0 for a null or reversed range. */
    public static int count(LocalDate from, LocalDate to) {
        if (from == null || to == null || to.isBefore(from)) {
            return 0;
        }
        int total = 0;
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            if (isWorkingDay(d)) {
                total++;
            }
        }
        return total;
    }

    public static int countInMonth(YearMonth month) {
        return count(month.atDay(1), month.atEndOfMonth());
    }

    /** Working days that fall inside BOTH [start, end] and [from, to]. */
    public static int overlap(LocalDate start, LocalDate end, LocalDate from, LocalDate to) {
        LocalDate a = start.isAfter(from) ? start : from;
        LocalDate b = end.isBefore(to) ? end : to;
        return count(a, b);
    }
}

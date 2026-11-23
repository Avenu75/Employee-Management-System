package com.rainbow.springboot.thymeleafdemo.dto;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** One person (an other-worker or an employee) on the reception board. */
public class GateRow {

    private final int id;
    private final String name;
    private final String detail;
    private final String state;      // NONE, IN, OUT, ABSENT or LEAVE
    private final boolean late;
    private final LocalTime checkIn;
    private final LocalTime checkOut;

    public GateRow(int id, String name, String detail, String state, boolean late, LocalTime checkIn, LocalTime checkOut) {
        this.id = id;
        this.name = name;
        this.detail = detail == null ? "" : detail;
        this.state = state;
        this.late = late;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getDetail() { return detail; }
    public String getState() { return state; }
    public boolean isLate() { return late; }
    public LocalTime getCheckIn() { return checkIn; }
    public LocalTime getCheckOut() { return checkOut; }

    /** Rows whose name or detail contains the search text (all rows when it is blank). */
    public static List<GateRow> filter(List<GateRow> rows, String query) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        if (q.isEmpty()) {
            return rows;
        }
        List<GateRow> matches = new ArrayList<>();
        for (GateRow row : rows) {
            if ((row.name + " " + row.detail).toLowerCase(Locale.ROOT).contains(q)) {
                matches.add(row);
            }
        }
        return matches;
    }

    /** Counts for the summary cards. */
    public static Map<String, Long> summary(List<GateRow> rows) {
        long onSite = 0, left = 0, late = 0, absent = 0, leave = 0;
        for (GateRow row : rows) {
            switch (row.state) {
                case "IN": onSite++; break;
                case "OUT": left++; break;
                case "ABSENT": absent++; break;
                case "LEAVE": leave++; break;
                default: break;
            }
            if (row.late) {
                late++;
            }
        }
        long total = rows.size();
        Map<String, Long> map = new LinkedHashMap<>();
        map.put("total", total);
        map.put("onSite", onSite);
        map.put("left", left);
        map.put("late", late);
        map.put("absent", absent);
        map.put("leave", leave);
        map.put("waiting", total - onSite - left - absent - leave);
        return map;
    }
}

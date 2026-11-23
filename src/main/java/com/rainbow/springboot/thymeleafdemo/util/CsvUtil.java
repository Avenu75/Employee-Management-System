package com.rainbow.springboot.thymeleafdemo.util;

/** Safe CSV cell formatting for the exports. */
public final class CsvUtil {

    private CsvUtil() {
    }

    /**
     * Quotes the value when needed and neutralises spreadsheet formulas: a cell
     * that starts with = + - or @ would otherwise be executed by Excel when the
     * file is opened (CSV injection), so it is prefixed with an apostrophe.
     */
    public static String cell(String value) {
        if (value == null) {
            return "";
        }
        String v = value;
        if (!v.isEmpty()) {
            char first = v.charAt(0);
            if (first == '=' || first == '+' || first == '-' || first == '@' || first == '\t' || first == '\r') {
                v = "'" + v;
            }
        }
        if (v.contains(",") || v.contains("\"") || v.contains("\n") || v.contains("\r")) {
            v = "\"" + v.replace("\"", "\"\"") + "\"";
        }
        return v;
    }

    public static String row(String... cells) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cells.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(cell(cells[i]));
        }
        return sb.toString();
    }
}

package io.github.caioeduardopereirafelix.financeapi.statement;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class StatementDate {

    static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
    static final LocalDate EARLIEST = LocalDate.of(2000, 1, 1);

    private static final Pattern OFX = Pattern.compile(
            "^(\\d{4})(\\d{2})(\\d{2})(?:(\\d{2})(\\d{2})(\\d{2})?)?(?:\\.\\d+)?(?:\\[\\s*([+-]?\\d+(?:\\.\\d+)?)[^\\]]*\\])?.*$");
    private static final Pattern ISO = Pattern.compile(
            "^(\\d{4})-(\\d{2})-(\\d{2})(?:[ T](\\d{2}):(\\d{2})(?::(\\d{2}))?)?.*$");
    private static final Pattern DAY_FIRST = Pattern.compile(
            "^(\\d{1,2})[/.-](\\d{1,2})[/.-](\\d{4}|\\d{2})(?!\\d)(?:[ T,]+(\\d{1,2}):(\\d{2})(?::(\\d{2}))?)?.*$");

    private StatementDate() {
    }

    static Instant ofx(String raw) {
        Matcher m = OFX.matcher(raw == null ? "" : raw.trim());
        if (!m.matches()) {
            throw new DateTimeException("data invalida: " + raw);
        }
        LocalDate date = LocalDate.of(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3)));
        check(date);
        if (m.group(4) == null) {
            return atNoon(date);
        }
        LocalTime time = LocalTime.of(Integer.parseInt(m.group(4)), Integer.parseInt(m.group(5)),
                m.group(6) == null ? 0 : Integer.parseInt(m.group(6)));
        if (time.equals(LocalTime.MIDNIGHT) && m.group(7) == null) {
            return atNoon(date);
        }
        LocalDateTime local = LocalDateTime.of(date, time);
        if (m.group(7) == null) {
            return local.atZone(ZONE).toInstant();
        }
        int seconds = (int) Math.round(Double.parseDouble(m.group(7)) * 3600);
        return local.toInstant(ZoneOffset.ofTotalSeconds(seconds));
    }

    static Instant csv(String raw) {
        String text = raw == null ? "" : raw.trim();
        Matcher iso = ISO.matcher(text);
        if (iso.matches()) {
            return build(Integer.parseInt(iso.group(1)), Integer.parseInt(iso.group(2)), Integer.parseInt(iso.group(3)),
                    iso.group(4), iso.group(5), iso.group(6));
        }
        Matcher br = DAY_FIRST.matcher(text);
        if (br.matches()) {
            int year = Integer.parseInt(br.group(3));
            if (br.group(3).length() == 2) {
                year += 2000;
            }
            return build(year, Integer.parseInt(br.group(2)), Integer.parseInt(br.group(1)),
                    br.group(4), br.group(5), br.group(6));
        }
        throw new DateTimeException("data invalida: " + raw);
    }

    private static Instant build(int year, int month, int day, String hour, String minute, String second) {
        LocalDate date = LocalDate.of(year, month, day);
        check(date);
        if (hour == null) {
            return atNoon(date);
        }
        LocalTime time = LocalTime.of(Integer.parseInt(hour), Integer.parseInt(minute),
                second == null ? 0 : Integer.parseInt(second));
        return LocalDateTime.of(date, time).atZone(ZONE).toInstant();
    }

    private static void check(LocalDate date) {
        if (date.isBefore(EARLIEST)) {
            throw new DateTimeException("data anterior a 2000: " + date);
        }
    }

    private static Instant atNoon(LocalDate date) {
        return date.atTime(LocalTime.NOON).atZone(ZONE).toInstant();
    }
}

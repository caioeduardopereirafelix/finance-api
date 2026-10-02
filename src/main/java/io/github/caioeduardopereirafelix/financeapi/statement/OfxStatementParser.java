package io.github.caioeduardopereirafelix.financeapi.statement;

import io.github.caioeduardopereirafelix.financeapi.exceptions.InvalidFieldException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class OfxStatementParser {

    private static final int MAX_PROBLEMS = 10;
    private static final int MAX_DESCRIPTION = 250;
    private static final Pattern MARKER = Pattern.compile("(?i)<(ACCTID|STMTTRN)>");

    public ParsedStatement parse(String text, boolean invertSign) {
        List<StatementEntry> entries = new ArrayList<>();
        List<String> problems = new ArrayList<>();
        StatementFingerprint fingerprint = new StatementFingerprint();
        String lower = text.toLowerCase(Locale.ROOT);
        String account = "";
        int found = 0;
        int invalid = 0;

        Matcher marker = MARKER.matcher(text);
        while (marker.find()) {
            if (marker.group(1).equalsIgnoreCase("ACCTID")) {
                account = StatementText.clean(valueAt(text, marker.end()), 60);
                continue;
            }

            found++;
            String block = text.substring(marker.end(), blockEnd(lower, marker.end()));
            try {
                String rawDate = tag(block, "DTPOSTED");
                String rawAmount = tag(block, "TRNAMT");
                if (rawDate == null || rawAmount == null) {
                    throw new IllegalArgumentException("data ou valor ausente");
                }
                Instant date = StatementDate.ofx(rawDate);
                BigDecimal amount = StatementAmount.parse(rawAmount);
                if (amount == null) {
                    throw new IllegalArgumentException("valor ausente");
                }
                if (invertSign) {
                    amount = amount.negate();
                }
                String description = description(tag(block, "NAME"), tag(block, "MEMO"));
                String fitId = StatementText.clean(tag(block, "FITID"), 80);
                String id = fitId.isEmpty()
                        ? "ofx:" + fingerprint.of(date, amount, description)
                        : "ofx:" + account + ":" + fitId;
                entries.add(new StatementEntry(id, description, amount, date, null));
            } catch (RuntimeException e) {
                invalid++;
                if (problems.size() < MAX_PROBLEMS) {
                    problems.add("Lancamento " + found + ": " + e.getMessage());
                }
            }
        }

        if (found == 0) {
            throw new InvalidFieldException("file", "Nenhum lancamento encontrado no arquivo OFX");
        }
        return new ParsedStatement(entries, invalid, problems);
    }

    private static String description(String name, String memo) {
        Set<String> parts = new LinkedHashSet<>();
        for (String raw : new String[]{name, memo}) {
            String cleaned = StatementText.clean(raw, MAX_DESCRIPTION);
            if (!cleaned.isEmpty()) {
                parts.add(cleaned);
            }
        }
        String joined = String.join(" - ", parts);
        if (joined.isEmpty()) {
            return "Sem descricao";
        }
        return joined.length() > MAX_DESCRIPTION ? joined.substring(0, MAX_DESCRIPTION).trim() : joined;
    }

    private static int blockEnd(String lower, int from) {
        int end = lower.length();
        for (String closer : new String[]{"</stmttrn>", "<stmttrn>", "</banktranlist>"}) {
            int index = lower.indexOf(closer, from);
            if (index >= 0 && index < end) {
                end = index;
            }
        }
        return end;
    }

    private static String tag(String block, String name) {
        Matcher m = Pattern.compile("(?i)<" + name + ">([^<\\r\\n]*)").matcher(block);
        return m.find() ? unescape(m.group(1).trim()) : null;
    }

    private static String valueAt(String text, int from) {
        int end = from;
        while (end < text.length() && text.charAt(end) != '<' && text.charAt(end) != '\r' && text.charAt(end) != '\n') {
            end++;
        }
        return unescape(text.substring(from, end).trim());
    }

    private static String unescape(String value) {
        return value.replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"")
                .replace("&apos;", "'").replace("&#39;", "'").replace("&amp;", "&");
    }
}

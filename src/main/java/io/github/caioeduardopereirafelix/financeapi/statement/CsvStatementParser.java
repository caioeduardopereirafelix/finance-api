package io.github.caioeduardopereirafelix.financeapi.statement;

import io.github.caioeduardopereirafelix.financeapi.exceptions.InvalidFieldException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
public class CsvStatementParser {

    private static final int MAX_PROBLEMS = 10;
    private static final int HEADER_SEARCH_ROWS = 30;
    private static final int MAX_DESCRIPTION = 250;
    private static final char[] DELIMITERS = {';', ',', '\t'};

    private record Columns(int date, int amount, int debit, int credit, int category, List<Integer> description) {

        boolean usable() {
            return date >= 0 && (amount >= 0 || debit >= 0 || credit >= 0);
        }
    }

    public ParsedStatement parse(String text, boolean invertSign) {
        char delimiter = detectDelimiter(text);
        List<List<String>> rows = read(text, delimiter);

        Columns columns = null;
        int headerRow = -1;
        for (int i = 0; i < Math.min(rows.size(), HEADER_SEARCH_ROWS); i++) {
            Columns candidate = map(rows.get(i));
            if (candidate.usable()) {
                columns = candidate;
                headerRow = i;
                break;
            }
        }
        if (columns == null) {
            throw new InvalidFieldException("file",
                    "Nao reconhecemos as colunas do CSV. Ele precisa de uma linha de cabecalho com data e valor "
                            + "(ou debito e credito), por exemplo: Data;Descricao;Valor");
        }

        List<StatementEntry> entries = new ArrayList<>();
        List<String> problems = new ArrayList<>();
        StatementFingerprint fingerprint = new StatementFingerprint();
        int invalid = 0;

        for (int i = headerRow + 1; i < rows.size(); i++) {
            List<String> row = rows.get(i);
            if (row.stream().allMatch(String::isBlank)) {
                continue;
            }
            String rawDate = cell(row, columns.date());
            try {
                BigDecimal amount = amountOf(row, columns);
                if (amount == null && rawDate.isBlank()) {
                    continue;
                }
                if (amount == null) {
                    continue;
                }
                Instant date = StatementDate.csv(rawDate);
                if (invertSign) {
                    amount = amount.negate();
                }
                String description = description(row, columns);
                String category = columns.category() >= 0 ? cell(row, columns.category()) : null;
                entries.add(new StatementEntry("csv:" + fingerprint.of(date, amount, description),
                        description, amount, date, category));
            } catch (RuntimeException e) {
                invalid++;
                if (problems.size() < MAX_PROBLEMS) {
                    problems.add("Linha " + (i + 1) + ": " + e.getMessage());
                }
            }
        }

        if (entries.isEmpty() && invalid == 0) {
            throw new InvalidFieldException("file", "Nenhum lancamento encontrado no arquivo CSV");
        }
        return new ParsedStatement(entries, invalid, problems);
    }

    private static BigDecimal amountOf(List<String> row, Columns columns) {
        if (columns.amount() >= 0) {
            BigDecimal value = StatementAmount.parse(cell(row, columns.amount()));
            if (value != null) {
                return value;
            }
        }
        if (columns.debit() < 0 && columns.credit() < 0) {
            return null;
        }
        BigDecimal debit = columns.debit() >= 0 ? StatementAmount.parse(cell(row, columns.debit())) : null;
        BigDecimal credit = columns.credit() >= 0 ? StatementAmount.parse(cell(row, columns.credit())) : null;
        if (debit == null && credit == null) {
            return null;
        }
        BigDecimal in = credit == null ? BigDecimal.ZERO : credit.abs();
        BigDecimal out = debit == null ? BigDecimal.ZERO : debit.abs();
        return in.subtract(out);
    }

    private static String description(List<String> row, Columns columns) {
        Set<String> parts = new LinkedHashSet<>();
        for (int index : columns.description()) {
            String cleaned = StatementText.clean(cell(row, index), MAX_DESCRIPTION);
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

    private static String cell(List<String> row, int index) {
        return index >= 0 && index < row.size() ? row.get(index).trim() : "";
    }

    private static Columns map(List<String> header) {
        int date = -1;
        int amount = -1;
        int debit = -1;
        int credit = -1;
        int category = -1;
        List<Integer> description = new ArrayList<>();

        for (int i = 0; i < header.size(); i++) {
            String h = normalize(header.get(i));
            if (h.isEmpty()) {
                continue;
            }
            if (date < 0 && (h.equals("data") || h.startsWith("data ") || h.equals("date") || h.startsWith("date ") || h.equals("dt"))) {
                date = i;
            } else if (h.contains("saldo") || h.contains("balance")) {
                continue;
            } else if (amount < 0 && (h.startsWith("valor") || h.equals("amount") || h.equals("montante") || h.startsWith("value"))) {
                amount = i;
            } else if (debit < 0 && (h.contains("debito") || h.equals("saida") || h.equals("saidas") || h.equals("debit"))) {
                debit = i;
            } else if (credit < 0 && (h.contains("credito") || h.equals("entrada") || h.equals("entradas") || h.equals("credit"))) {
                credit = i;
            } else if (category < 0 && (h.equals("categoria") || h.equals("category"))) {
                category = i;
            } else if (isDescription(h)) {
                description.add(i);
            }
        }
        return new Columns(date, amount, debit, credit, category, description);
    }

    private static boolean isDescription(String h) {
        for (String key : new String[]{"descricao", "historico", "lancamento", "description", "memo", "estabelecimento",
                "titulo", "title", "detalhe", "favorecido", "nome"}) {
            if (h.contains(key)) {
                return true;
            }
        }
        return false;
    }

    private static String normalize(String header) {
        return Normalizer.normalize(header == null ? "" : header, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9 ]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static char detectDelimiter(String text) {
        String[] lines = text.split("\\r\\n|\\n|\\r", HEADER_SEARCH_ROWS + 1);
        char best = ';';
        int bestLines = 0;
        for (char candidate : DELIMITERS) {
            int linesWithColumns = 0;
            for (int i = 0; i < Math.min(lines.length, HEADER_SEARCH_ROWS); i++) {
                if (countOutsideQuotes(lines[i], candidate) >= 2) {
                    linesWithColumns++;
                }
            }
            if (linesWithColumns > bestLines) {
                best = candidate;
                bestLines = linesWithColumns;
            }
        }
        return best;
    }

    private static int countOutsideQuotes(String line, char delimiter) {
        int count = 0;
        boolean quoted = false;
        for (char c : line.toCharArray()) {
            if (c == '"') {
                quoted = !quoted;
            } else if (c == delimiter && !quoted) {
                count++;
            }
        }
        return count;
    }

    static List<List<String>> read(String text, char delimiter) {
        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (quoted) {
                if (c == '"') {
                    if (i + 1 < text.length() && text.charAt(i + 1) == '"') {
                        field.append('"');
                        i++;
                    } else {
                        quoted = false;
                    }
                } else {
                    field.append(c);
                }
            } else if (c == '"') {
                quoted = true;
            } else if (c == delimiter) {
                row.add(field.toString());
                field.setLength(0);
            } else if (c == '\n' || c == '\r') {
                if (c == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') {
                    i++;
                }
                row.add(field.toString());
                field.setLength(0);
                rows.add(row);
                row = new ArrayList<>();
            } else {
                field.append(c);
            }
        }
        if (field.length() > 0 || !row.isEmpty()) {
            row.add(field.toString());
            rows.add(row);
        }
        return rows;
    }
}

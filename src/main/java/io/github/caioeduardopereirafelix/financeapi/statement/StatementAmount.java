package io.github.caioeduardopereirafelix.financeapi.statement;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.regex.Pattern;

final class StatementAmount {

    private static final Pattern THOUSANDS_ONLY = Pattern.compile("^-?\\d{1,3}(\\.\\d{3})+$");

    private StatementAmount() {
    }

    static BigDecimal parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String text = raw.replace('−', '-').replace("R$", "").replace("r$", "")
                .replaceAll("[\\s\u00A0]", "");

        boolean negative = false;
        if (text.startsWith("(") && text.endsWith(")")) {
            negative = true;
            text = text.substring(1, text.length() - 1);
        }
        if (text.endsWith("-")) {
            negative = true;
            text = text.substring(0, text.length() - 1);
        }
        if (text.startsWith("+")) {
            text = text.substring(1);
        }
        if (text.isEmpty() || !text.matches("-?[0-9.,]+")) {
            throw new NumberFormatException("valor invalido: " + raw);
        }

        int lastDot = text.lastIndexOf('.');
        int lastComma = text.lastIndexOf(',');
        String normalized;
        if (lastDot >= 0 && lastComma >= 0) {
            char decimal = lastComma > lastDot ? ',' : '.';
            normalized = decimal == ','
                    ? text.replace(".", "").replace(',', '.')
                    : text.replace(",", "");
        } else if (lastComma >= 0) {
            normalized = text.replace(".", "").replace(',', '.');
        } else if (lastDot >= 0 && THOUSANDS_ONLY.matcher(text).matches()) {
            normalized = text.replace(".", "");
        } else {
            normalized = text;
        }

        BigDecimal value = new BigDecimal(normalized).setScale(2, RoundingMode.HALF_UP);
        return negative ? value.negate() : value;
    }
}

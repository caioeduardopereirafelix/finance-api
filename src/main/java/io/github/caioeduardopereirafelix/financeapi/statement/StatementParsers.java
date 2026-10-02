package io.github.caioeduardopereirafelix.financeapi.statement;

import io.github.caioeduardopereirafelix.financeapi.exceptions.InvalidFieldException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
@RequiredArgsConstructor
public class StatementParsers {

    private static final int SNIFF_CHARS = 4000;

    private final OfxStatementParser ofx;
    private final CsvStatementParser csv;

    public ParsedStatement parse(byte[] content, boolean invertSign) {
        if (content == null || content.length == 0) {
            throw new InvalidFieldException("file", "O arquivo esta vazio");
        }
        String text = StatementText.decode(content);
        if (text.isBlank()) {
            throw new InvalidFieldException("file", "O arquivo esta vazio");
        }

        String head = text.substring(0, Math.min(text.length(), SNIFF_CHARS)).toLowerCase(Locale.ROOT);
        if (head.contains("<ofx") || head.contains("ofxheader")) {
            return ofx.parse(text, invertSign);
        }
        return csv.parse(text, invertSign);
    }
}

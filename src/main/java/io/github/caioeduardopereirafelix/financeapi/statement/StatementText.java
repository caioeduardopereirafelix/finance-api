package io.github.caioeduardopereirafelix.financeapi.statement;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

final class StatementText {

    private static final Charset WINDOWS_1252 = Charset.forName("windows-1252");

    private StatementText() {
    }

    static String decode(byte[] content) {
        String text;
        try {
            text = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(content))
                    .toString();
        } catch (CharacterCodingException e) {
            text = new String(content, WINDOWS_1252);
        }
        if (!text.isEmpty() && text.charAt(0) == '﻿') {
            text = text.substring(1);
        }
        return text;
    }

    static String clean(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        String cleaned = value.replaceAll("\\p{Cntrl}", " ").replaceAll("\\s+", " ").trim();
        return cleaned.length() > maxLength ? cleaned.substring(0, maxLength).trim() : cleaned;
    }
}

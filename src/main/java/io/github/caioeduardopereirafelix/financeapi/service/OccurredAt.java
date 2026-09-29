package io.github.caioeduardopereirafelix.financeapi.service;

import io.github.caioeduardopereirafelix.financeapi.exceptions.InvalidFieldException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

/**
 * Transforma o dia que o usuario escolheu no instante em que o gasto ocorreu.
 *
 * O usuario informa so o dia, no fuso da aplicacao. Para nao mudar a hora de quem so edita a
 * descricao, o instante ja gravado e mantido enquanto o dia nao mudar. Um dia anterior a hoje
 * fica ao meio-dia: qualquer fuso proximo continua enxergando o mesmo dia.
 */
public final class OccurredAt {

    /** Antes disso quase certamente e erro de digitacao (0026 em vez de 2026). */
    static final LocalDate EARLIEST = LocalDate.of(2000, 1, 1);

    private OccurredAt() {
    }

    /**
     * @param requested o dia pedido; nulo = nao mexer
     * @param current   o instante ja gravado; nulo ao criar
     */
    public static Instant resolve(LocalDate requested, Instant current, ZoneId zone, Instant now) {
        if (requested == null) {
            return current != null ? current : now;
        }

        LocalDate today = now.atZone(zone).toLocalDate();
        if (requested.isAfter(today)) {
            throw new InvalidFieldException("occurredOn", "A data nao pode ser futura");
        }
        if (requested.isBefore(EARLIEST)) {
            throw new InvalidFieldException("occurredOn", "A data deve ser a partir de 2000");
        }

        if (current != null && dayOf(current, zone).equals(requested)) {
            return current;
        }
        if (requested.equals(today)) {
            return now;
        }
        return requested.atTime(LocalTime.NOON).atZone(zone).toInstant();
    }

    public static LocalDate dayOf(Instant instant, ZoneId zone) {
        return instant.atZone(zone).toLocalDate();
    }
}

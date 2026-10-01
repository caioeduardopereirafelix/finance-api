package io.github.caioeduardopereirafelix.financeapi.migration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


class MigrationImmutabilityTest {

    static final Map<String, Integer> APPLIED = Map.of(
            "1", 782388289,
            "2", -479692910,
            "3", -128057212,
            "4", -1957963145,
            "5", 301407458,
            "6", 435866665,
            "7", 1638335374,
            "8", 666689705,
            "9", 1881932073,
            "10", -1954798502
    );

    private static Map<String, Integer> checksumsOnDisk() {
        Flyway flyway = Flyway.configure()
                .dataSource("jdbc:h2:mem:migration_guard;DB_CLOSE_DELAY=-1", "sa", "")
                .locations("classpath:db/migration")
                .load();

        return Arrays.stream(flyway.info().all())
                .collect(Collectors.toMap(
                        info -> info.getVersion().getVersion(),
                        MigrationInfo::getChecksum,
                        (a, b) -> a,
                        TreeMap::new));
    }

    static List<String> problems(Map<String, Integer> applied, Map<String, Integer> onDisk) {
        List<String> problems = new ArrayList<>();

        applied.forEach((version, checksum) -> {
            if (!onDisk.containsKey(version)) {
                problems.add("A migration V" + version + " foi removida ou renomeada. Ela ja rodou em algum banco"
                        + " e precisa continuar existindo.");
            } else if (!checksum.equals(onDisk.get(version))) {
                problems.add("A migration V" + version + " foi alterada depois de aplicada (checksum registrado "
                        + checksum + ", atual " + onDisk.get(version) + "). O Flyway se recusaria a subir nos bancos"
                        + " onde ela ja rodou, mesmo que a mudanca seja so de comentario. Restaure o arquivo"
                        + " (git checkout <commit> -- caminho) e ponha a mudanca numa migration nova.");
            }
        });

        onDisk.forEach((version, checksum) -> {
            if (!applied.containsKey(version)) {
                problems.add("A migration V" + version + " ainda nao esta registrada. Se ela esta pronta, acrescente"
                        + " em APPLIED: \"" + version + "\", " + checksum + ",");
            }
        });

        return problems;
    }

    @Test
    void migrationsJaAplicadasNaoForamEditadas() {
        List<String> problems = problems(APPLIED, checksumsOnDisk());

        assertTrue(problems.isEmpty(), () -> "\n" + String.join("\n", problems));
    }

    @Test
    void encontraAsMigrationsDoProjeto() {
        assertTrue(checksumsOnDisk().size() >= APPLIED.size(),
                "O Flyway nao achou as migrations em classpath:db/migration");
    }

    private static final Map<String, Integer> BASE = Map.of("1", 100, "2", 200);

    @Test
    void tudoIgualNaoTemProblema() {
        assertEquals(List.of(), problems(BASE, Map.of("1", 100, "2", 200)));
    }

    @Test
    void migrationEditadaEApontada() {
        List<String> problems = problems(BASE, Map.of("1", 100, "2", 999));

        assertEquals(1, problems.size());
        assertTrue(problems.get(0).contains("V2 foi alterada"));
        assertTrue(problems.get(0).contains("registrado 200, atual 999"));
    }

    @Test
    void migrationRemovidaEApontada() {
        List<String> problems = problems(BASE, Map.of("1", 100));

        assertEquals(1, problems.size());
        assertTrue(problems.get(0).contains("V2 foi removida ou renomeada"));
    }

    @Test
    void migrationNovaPedeRegistroEJaSugereALinha() {
        List<String> problems = problems(BASE, Map.of("1", 100, "2", 200, "3", 300));

        assertEquals(1, problems.size());
        assertTrue(problems.get(0).contains("V3 ainda nao esta registrada"));
        assertTrue(problems.get(0).contains("\"3\", 300,"));
    }
}

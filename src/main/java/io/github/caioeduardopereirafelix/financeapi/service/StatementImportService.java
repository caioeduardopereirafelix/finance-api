package io.github.caioeduardopereirafelix.financeapi.service;

import io.github.caioeduardopereirafelix.financeapi.bank.BankCategoryMapper;
import io.github.caioeduardopereirafelix.financeapi.exceptions.InvalidFieldException;
import io.github.caioeduardopereirafelix.financeapi.model.entity.Transaction;
import io.github.caioeduardopereirafelix.financeapi.model.entity.User;
import io.github.caioeduardopereirafelix.financeapi.model.enums.CategoryName;
import io.github.caioeduardopereirafelix.financeapi.model.enums.TransactionSource;
import io.github.caioeduardopereirafelix.financeapi.repository.TransactionRepository;
import io.github.caioeduardopereirafelix.financeapi.statement.ParsedStatement;
import io.github.caioeduardopereirafelix.financeapi.statement.StatementEntry;
import io.github.caioeduardopereirafelix.financeapi.statement.StatementParsers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class StatementImportService {

    static final int MAX_ENTRIES = 5000;
    private static final int LOOKUP_CHUNK = 1000;
    private static final String PREFIX = "file:";

    public record Result(int total, int imported, int skipped, int invalid, List<String> problems) {
    }

    private final StatementParsers parsers;
    private final BankCategoryMapper categoryMapper;
    private final CategoryRuleService categoryRules;
    private final TransactionRepository transactions;

    @Transactional
    public Result importFile(User user, byte[] content, boolean invertSign) {
        ParsedStatement parsed = parsers.parse(content, invertSign);

        if (parsed.entries().size() > MAX_ENTRIES) {
            throw new InvalidFieldException("file",
                    "O arquivo tem mais de " + MAX_ENTRIES + " lancamentos. Divida por periodo e envie de novo");
        }

        int skipped = 0;
        Map<String, StatementEntry> candidates = new LinkedHashMap<>();
        for (StatementEntry entry : parsed.entries()) {
            String externalId = PREFIX + entry.id();
            if (entry.amount().signum() == 0 || candidates.containsKey(externalId)) {
                skipped++;
                continue;
            }
            candidates.put(externalId, entry);
        }

        Set<String> existing = existingIds(user, new ArrayList<>(candidates.keySet()));
        Map<String, CategoryName> userRules = categoryRules.rulesOf(user);
        List<Transaction> toSave = new ArrayList<>();

        for (Map.Entry<String, StatementEntry> candidate : candidates.entrySet()) {
            if (existing.contains(candidate.getKey())) {
                skipped++;
                continue;
            }
            StatementEntry entry = candidate.getValue();
            BankCategoryMapper.Mapped mapped = categoryMapper.map(entry.amount(), entry.category());
            CategoryName category = userRules.getOrDefault(
                    CategoryRuleService.lookupKey(mapped.type(), DescriptionKey.of(entry.description())),
                    mapped.category());

            Transaction transaction = new Transaction();
            transaction.setUser(user);
            transaction.setCreatedBy(user.getId().toString());
            transaction.setDescription(entry.description());
            transaction.setAmount(entry.amount().abs());
            transaction.setType(mapped.type());
            transaction.setCategory(category);
            transaction.setSource(TransactionSource.FILE);
            transaction.setExternalId(candidate.getKey());
            transaction.setOccurredAt(entry.date());
            toSave.add(transaction);
        }
        transactions.saveAll(toSave);

        int total = parsed.entries().size() + parsed.invalid();
        return new Result(total, toSave.size(), skipped, parsed.invalid(), parsed.problems());
    }

    private Set<String> existingIds(User user, List<String> ids) {
        Set<String> found = new HashSet<>();
        for (int from = 0; from < ids.size(); from += LOOKUP_CHUNK) {
            List<String> chunk = ids.subList(from, Math.min(ids.size(), from + LOOKUP_CHUNK));
            found.addAll(transactions.findExistingExternalIds(user, chunk));
        }
        return found;
    }
}

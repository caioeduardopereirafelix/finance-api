package io.github.caioeduardopereirafelix.financeapi.service;

import io.github.caioeduardopereirafelix.financeapi.model.entity.CategoryRule;
import io.github.caioeduardopereirafelix.financeapi.model.entity.Transaction;
import io.github.caioeduardopereirafelix.financeapi.model.entity.User;
import io.github.caioeduardopereirafelix.financeapi.model.enums.CategoryName;
import io.github.caioeduardopereirafelix.financeapi.model.enums.TransactionSource;
import io.github.caioeduardopereirafelix.financeapi.model.enums.TransactionalType;
import io.github.caioeduardopereirafelix.financeapi.repository.CategoryRuleRepository;
import io.github.caioeduardopereirafelix.financeapi.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CategoryRuleService {

    private final CategoryRuleRepository rules;
    private final TransactionRepository transactions;

    @Transactional(readOnly = true)
    public Map<String, CategoryName> rulesOf(User user) {
        Map<String, CategoryName> byKey = new HashMap<>();
        for (CategoryRule rule : rules.findByUser(user)) {
            byKey.put(lookupKey(rule.getType(), rule.getMatchKey()), rule.getCategory());
        }
        return byKey;
    }

    public static String lookupKey(TransactionalType type, String matchKey) {
        return type + "|" + matchKey;
    }

    @Transactional
    public int remember(User user, Transaction origin, CategoryName category) {
        String key = DescriptionKey.of(origin.getDescription());
        if (key.isEmpty()) {
            return 0;
        }

        CategoryRule rule = rules.findByUserAndMatchKeyAndType(user, key, origin.getType())
                .orElseGet(() -> {
                    CategoryRule created = new CategoryRule();
                    created.setUser(user);
                    created.setMatchKey(key);
                    created.setType(origin.getType());
                    created.setCreatedAt(Instant.now());
                    return created;
                });
        rule.setCategory(category);
        rules.save(rule);

        int changed = 0;
        for (Transaction other : transactions.findByUserAndSourceNotAndType(user, TransactionSource.MANUAL, origin.getType())) {
            boolean sameSpot = !other.getId().equals(origin.getId())
                    && key.equals(DescriptionKey.of(other.getDescription()));
            if (sameSpot && other.getCategory() != category) {
                other.setCategory(category);
                transactions.save(other);
                changed++;
            }
        }
        return changed;
    }
}

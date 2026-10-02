package io.github.caioeduardopereirafelix.financeapi.service;

import io.github.caioeduardopereirafelix.financeapi.config.SecurityUtils;
import io.github.caioeduardopereirafelix.financeapi.exceptions.InvalidFieldException;
import io.github.caioeduardopereirafelix.financeapi.exceptions.TransactionNotFound;
import io.github.caioeduardopereirafelix.financeapi.model.dto.transaction.CategoryTotalDTO;
import io.github.caioeduardopereirafelix.financeapi.model.dto.transaction.CreateTransactionRequestDTO;
import io.github.caioeduardopereirafelix.financeapi.model.dto.transaction.SummaryResponseDTO;
import io.github.caioeduardopereirafelix.financeapi.model.dto.transaction.UpdateTransactionDTO;
import io.github.caioeduardopereirafelix.financeapi.model.entity.Transaction;
import io.github.caioeduardopereirafelix.financeapi.model.entity.User;
import io.github.caioeduardopereirafelix.financeapi.model.enums.CategoryName;
import io.github.caioeduardopereirafelix.financeapi.model.enums.TransactionalType;
import io.github.caioeduardopereirafelix.financeapi.repository.TransactionRepository;
import io.github.caioeduardopereirafelix.financeapi.repository.TransactionSummaryProjection;
import io.github.caioeduardopereirafelix.financeapi.service.validator.TransactionValidator;
import io.github.caioeduardopereirafelix.financeapi.specification.TransactionSpecification;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final TransactionValidator validator;
    private final SecurityUtils securityUtils;
    private final CategoryRuleService categoryRules;

    @Value("${app.zone:America/Sao_Paulo}")
    private ZoneId zone;

    public record CategoryChange(Transaction transaction, int updated) {
    }

    public Transaction create(CreateTransactionRequestDTO transaction){

        validator.validateCategoryByType(transaction.category(), transaction.type());
        validator.validateAmount(transaction.amount());

        User user = securityUtils.getAuthenticatedUser();

        var transactionSave = new Transaction();
        transactionSave.setDescription(transaction.description());
        transactionSave.setCategory(transaction.category());
        transactionSave.setAmount(transaction.amount());
        transactionSave.setType(transaction.type());
        transactionSave.setUser(user);
        transactionSave.setOccurredAt(OccurredAt.resolve(transaction.occurredOn(), null, zone, Instant.now()));

        return transactionRepository.save(transactionSave);
    }

    public Transaction findByIdForAuthenticatedUser(UUID id){

        User user = securityUtils.getAuthenticatedUser();

        return transactionRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new TransactionNotFound("Transaction not found"));
    }

    public Transaction deleteTransaction(UUID id){

        User user = securityUtils.getAuthenticatedUser();

        var transaction = transactionRepository.findByIdAndUser(id, user)
                        .orElseThrow(() -> new TransactionNotFound("Transaction Not Found"));

        transactionRepository.delete(transaction);

        return transaction;
    }

    public Transaction updateTransaction(UUID id, UpdateTransactionDTO transactionDTO) {

        User user = securityUtils.getAuthenticatedUser();
        var transaction = transactionRepository
                .findByIdAndUser(id, user).orElseThrow(() -> new TransactionNotFound("Transaction not found"));

        validator.validateAmount(transactionDTO.amount());
        validator.validateCategoryByType(transactionDTO.category(), transactionDTO.type());
        validator.validateImportedOnlyChangesCategory(transaction, transactionDTO);
        requireSameDayIfImported(transaction, transactionDTO);

        transaction.setType(transactionDTO.type());
        transaction.setDescription(transactionDTO.description());
        transaction.setCategory(transactionDTO.category());
        transaction.setAmount(transactionDTO.amount());
        transaction.setOccurredAt(OccurredAt.resolve(
                transactionDTO.occurredOn(), transaction.getOccurredAt(), zone, Instant.now()));

        return transactionRepository.save(transaction);
    }

    private void requireSameDayIfImported(Transaction transaction, UpdateTransactionDTO changes) {
        boolean imported = transaction.getSource().imported();
        if (imported && changes.occurredOn() != null
                && !changes.occurredOn().equals(OccurredAt.dayOf(transaction.getOccurredAt(), zone))) {
            throw new InvalidFieldException("occurredOn",
                    "Transacao importada: a data vem do extrato e nao pode ser alterada");
        }
    }

    public CategoryChange updateCategory(UUID id, CategoryName category, boolean applyToSimilar) {

        User user = securityUtils.getAuthenticatedUser();
        var transaction = transactionRepository
                .findByIdAndUser(id, user).orElseThrow(() -> new TransactionNotFound("Transaction not found"));

        validator.validateCategoryByType(category, transaction.getType());

        transaction.setCategory(category);
        transactionRepository.save(transaction);

        int updated = 1;
        if (applyToSimilar) {
            updated += categoryRules.remember(user, transaction, category);
        }
        return new CategoryChange(transaction, updated);
    }

    private static final Instant FIRST_INSTANT = Instant.EPOCH;
    private static final Instant LAST_INSTANT = Instant.parse("3000-01-01T00:00:00Z");

    public SummaryResponseDTO getSummary(Instant from, Instant before){

        User user = securityUtils.getAuthenticatedUser();

        Map<TransactionalType, BigDecimal> totals = transactionRepository
                .summarizeByType(user, from != null ? from : FIRST_INSTANT, before != null ? before : LAST_INSTANT)
                .stream()
                .collect(Collectors.toMap(
                        TransactionSummaryProjection::getType,
                        TransactionSummaryProjection::getTotal));

        BigDecimal cashEntry = totals.getOrDefault(TransactionalType.CASH_ENTRY, BigDecimal.ZERO);
        BigDecimal expenses = totals.getOrDefault(TransactionalType.EXPENSES, BigDecimal.ZERO);

        return new SummaryResponseDTO(cashEntry, expenses, cashEntry.subtract(expenses));
    }

    public List<CategoryTotalDTO> getTotalsByCategory(Instant from, Instant before){

        User user = securityUtils.getAuthenticatedUser();

        return transactionRepository
                .totalsByCategory(user, from != null ? from : FIRST_INSTANT, before != null ? before : LAST_INSTANT)
                .stream()
                .map(row -> new CategoryTotalDTO(row.getCategory(), row.getType(), row.getTotal(), row.getCount()))
                .toList();
    }

    public Page<Transaction> findTransactionsWithFilters(
            TransactionalType type,
            CategoryName category,
            String description,
            BigDecimal minAmount,
            BigDecimal maxAmount,
            Instant occurredFrom,
            Instant occurredBefore,
            Pageable pageable
    ) {
        User user = securityUtils.getAuthenticatedUser();

        Specification<Transaction> specification = Specification
                .where(TransactionSpecification.belongsToUser(user))
                .and(TransactionSpecification.hasType(type))
                .and(TransactionSpecification.hasCategory(category))
                .and(TransactionSpecification.descriptionContains(description))
                .and(TransactionSpecification.amountGreaterThanOrEqual(minAmount))
                .and(TransactionSpecification.amountLessThanOrEqual(maxAmount))
                .and(TransactionSpecification.occurredFrom(occurredFrom))
                .and(TransactionSpecification.occurredBefore(occurredBefore));

        return transactionRepository.findAll(specification, pageable);
    }

}

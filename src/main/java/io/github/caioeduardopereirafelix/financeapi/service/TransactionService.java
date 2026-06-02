package io.github.caioeduardopereirafelix.financeapi.service;

import io.github.caioeduardopereirafelix.financeapi.config.SecurityUtils;
import io.github.caioeduardopereirafelix.financeapi.exceptions.TransactionNotFound;
import io.github.caioeduardopereirafelix.financeapi.model.dto.transaction.CreateTransactionRequestDTO;
import io.github.caioeduardopereirafelix.financeapi.model.dto.transaction.SummaryResponseDTO;
import io.github.caioeduardopereirafelix.financeapi.model.dto.transaction.UpdateTransactionDTO;
import io.github.caioeduardopereirafelix.financeapi.model.entity.Transaction;
import io.github.caioeduardopereirafelix.financeapi.model.entity.User;
import io.github.caioeduardopereirafelix.financeapi.model.enums.TransactionalType;
import io.github.caioeduardopereirafelix.financeapi.repository.TransactionRepository;
import io.github.caioeduardopereirafelix.financeapi.service.validator.TransactionValidator;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final TransactionValidator validator;
    private final SecurityUtils securityUtils;

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
        transactionSave.setCreatedBy(user.getId().toString());
        transactionSave.setCreatedAt(LocalDate.now());
        transactionSave.setLastModifiedBy(user.getId().toString());

        return transactionRepository.save(transactionSave);
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

        transaction.setType(transactionDTO.type());
        transaction.setDescription(transactionDTO.description());
        transaction.setCategory(transactionDTO.category());
        transaction.setAmount(transactionDTO.amount());
        transaction.setLastModifiedBy(user.getId().toString());

        return transactionRepository.save(transaction);
    }

    public Page <Transaction> findAllTransactionsForAuthenticatedUser(Pageable pageable){
        User user = securityUtils.getAuthenticatedUser();

        return transactionRepository.findByUser(user, pageable);
    }

    public SummaryResponseDTO getSummary(){

        User user = securityUtils.getAuthenticatedUser();

        List<Transaction> transactions = transactionRepository.findByUser(user);

        BigDecimal cashEntry = transactions.stream()
                .filter(t -> t.getType() == TransactionalType.CASH_ENTRY)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal expenses = transactions.stream()
                .filter( t -> t.getType() == TransactionalType.EXPENSES)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal balance = cashEntry.subtract(expenses);

        return new SummaryResponseDTO(cashEntry, expenses, balance);

    }

}

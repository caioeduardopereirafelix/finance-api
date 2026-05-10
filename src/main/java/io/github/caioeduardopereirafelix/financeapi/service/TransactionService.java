package io.github.caioeduardopereirafelix.financeapi.service;

import io.github.caioeduardopereirafelix.financeapi.model.dto.transaction.CreateTransactionRequestDTO;
import io.github.caioeduardopereirafelix.financeapi.model.entity.Transaction;
import io.github.caioeduardopereirafelix.financeapi.model.entity.User;
import io.github.caioeduardopereirafelix.financeapi.repository.TransactionRepository;
import io.github.caioeduardopereirafelix.financeapi.repository.UserRepository;
import io.github.caioeduardopereirafelix.financeapi.service.validator.TransactionValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionValidator validator;

    public Transaction create(CreateTransactionRequestDTO transaction){

        validator.validateCategoryByType(transaction.category(), transaction.type());
        validator.validateAmount(transaction.amount());

        User user = userRepository.findById(transaction.id())
                .orElseThrow(() -> new RuntimeException("Usuario nao encontrado"));

        var transactionSave = new Transaction();
        transactionSave.setDescription(transaction.description());
        transactionSave.setCategory(transaction.category());
        transactionSave.setAmount(transaction.amount());
        transactionSave.setType(transaction.type());
        transactionSave.setUser(user);
        transactionSave.setCreatedBy(user.getId().toString());
        transactionSave.setLastModifiedBy(user.getId().toString());

        return transactionRepository.save(transactionSave);
    }

    public Optional<Transaction> findById(UUID id){
        return transactionRepository.findById(id);
    }

    public void deleteTransaction(Transaction transactionDelete){
        transactionRepository.delete(transactionDelete);
    }
}

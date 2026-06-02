package io.github.caioeduardopereirafelix.financeapi.controller;

import io.github.caioeduardopereirafelix.financeapi.model.dto.transaction.CreateTransactionRequestDTO;
import io.github.caioeduardopereirafelix.financeapi.model.dto.transaction.ResponseTransactionDTO;
import io.github.caioeduardopereirafelix.financeapi.model.dto.transaction.SummaryResponseDTO;
import io.github.caioeduardopereirafelix.financeapi.model.dto.transaction.UpdateTransactionDTO;
import io.github.caioeduardopereirafelix.financeapi.model.entity.Transaction;
import io.github.caioeduardopereirafelix.financeapi.model.mapper.TransactionMapper;
import io.github.caioeduardopereirafelix.financeapi.repository.TransactionRepository;
import io.github.caioeduardopereirafelix.financeapi.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/transaction")
public class TransactionController {

    private final TransactionMapper transactionMapper;
    private final TransactionService service;
    private final TransactionRepository repository;

    @PostMapping
    public ResponseEntity<ResponseTransactionDTO> create(@Valid @RequestBody CreateTransactionRequestDTO requestTransaction){

        var transaction = service.create(requestTransaction);

        var response = new ResponseTransactionDTO(transaction.getId(), transaction.getDescription(), transaction.getAmount(), transaction.getCategory(), transaction.getType());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity delete(@PathVariable("id") String id){

        var transaction = service.deleteTransaction(UUID.fromString(id));

        var response = new ResponseTransactionDTO(transaction.getId(), transaction.getDescription(), transaction.getAmount(), transaction.getCategory(), transaction.getType());

        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<ResponseTransactionDTO> putTransaction
            (@PathVariable("id") String id,
             @RequestBody UpdateTransactionDTO transactionDTO){

        var idTransaction = UUID.fromString(id);

        Transaction transactionUpdate = service.updateTransaction(idTransaction, transactionDTO);

        return ResponseEntity.ok(new ResponseTransactionDTO(
                transactionUpdate.getId(),
                transactionUpdate.getDescription(),
                transactionUpdate.getAmount(),
                transactionUpdate.getCategory(),
                transactionUpdate.getType()));
    }


    @GetMapping
    public ResponseEntity<Page<ResponseTransactionDTO>> findAllTransactions(
            @PageableDefault(size = 10,
                             sort = "createdBy",
                             direction = Sort.Direction.DESC)Pageable pageable){
        Page<Transaction> transactions = service.findAllTransactionsForAuthenticatedUser(pageable);

        Page<ResponseTransactionDTO> response = transactions
                .map(transactionMapper::toResponse);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/summary")
    public ResponseEntity<SummaryResponseDTO> summary(){

        return ResponseEntity.ok(service.getSummary());
    }
}

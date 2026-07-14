package io.github.caioeduardopereirafelix.financeapi.controller;

import io.github.caioeduardopereirafelix.financeapi.model.dto.transaction.CreateTransactionRequestDTO;
import io.github.caioeduardopereirafelix.financeapi.model.dto.transaction.ResponseTransactionDTO;
import io.github.caioeduardopereirafelix.financeapi.model.dto.transaction.SummaryResponseDTO;
import io.github.caioeduardopereirafelix.financeapi.model.dto.transaction.UpdateTransactionDTO;
import io.github.caioeduardopereirafelix.financeapi.model.entity.Transaction;
import io.github.caioeduardopereirafelix.financeapi.model.enums.CategoryName;
import io.github.caioeduardopereirafelix.financeapi.model.enums.TransactionalType;
import io.github.caioeduardopereirafelix.financeapi.model.mapper.TransactionMapper;
import io.github.caioeduardopereirafelix.financeapi.repository.TransactionRepository;
import io.github.caioeduardopereirafelix.financeapi.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

        var response = new ResponseTransactionDTO(transaction.getDescription(), transaction.getAmount(), transaction.getCategory(), transaction.getType());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity delete(@PathVariable("id") String id){

        var transaction = service.deleteTransaction(UUID.fromString(id));

        var response = new ResponseTransactionDTO(transaction.getDescription(), transaction.getAmount(), transaction.getCategory(), transaction.getType());

        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<ResponseTransactionDTO> putTransaction
            (@PathVariable("id") String id,
             @RequestBody UpdateTransactionDTO transactionDTO){

        var idTransaction = UUID.fromString(id);

        Transaction transactionUpdate = service.updateTransaction(idTransaction, transactionDTO);

        return ResponseEntity.ok(new ResponseTransactionDTO(
                transactionUpdate.getDescription(),
                transactionUpdate.getAmount(),
                transactionUpdate.getCategory(),
                transactionUpdate.getType()));
    }


    @GetMapping
    public ResponseEntity<Page<ResponseTransactionDTO>> findAllTransactions(
            @RequestParam(required = false) TransactionalType type,
            @RequestParam(required = false) CategoryName category,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate,

            @PageableDefault(size = 10,
                             sort = "createdDate",
                             direction = Sort.Direction.DESC)Pageable pageable){

        LocalDateTime startDateTime = startDate != null
                ? startDate.atStartOfDay()
                : null;

        LocalDateTime endDateTime = endDate != null
                ? endDate.atTime(23, 59, 59)
                : null;

        Page<Transaction> transactions = service.findTransactionsWithFilters(
                type,
                category,
                description,
                minAmount,
                maxAmount,
                startDateTime,
                endDateTime,
                pageable
        );

        Page<ResponseTransactionDTO> response = transactions
                .map(transactionMapper::toResponse);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/summary")
    public ResponseEntity<SummaryResponseDTO> summary(){

        return ResponseEntity.ok(service.getSummary());
    }
}

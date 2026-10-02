package io.github.caioeduardopereirafelix.financeapi.controller;

import io.github.caioeduardopereirafelix.financeapi.exceptions.InvalidFieldException;
import io.github.caioeduardopereirafelix.financeapi.model.dto.transaction.CategoryTotalDTO;
import io.github.caioeduardopereirafelix.financeapi.model.dto.transaction.CategoryChangeResponseDTO;
import io.github.caioeduardopereirafelix.financeapi.model.dto.transaction.CreateTransactionRequestDTO;
import io.github.caioeduardopereirafelix.financeapi.model.dto.transaction.UpdateCategoryRequestDTO;
import io.github.caioeduardopereirafelix.financeapi.model.dto.transaction.ResponseTransactionDTO;
import io.github.caioeduardopereirafelix.financeapi.model.dto.transaction.SummaryResponseDTO;
import io.github.caioeduardopereirafelix.financeapi.model.dto.transaction.UpdateTransactionDTO;
import io.github.caioeduardopereirafelix.financeapi.model.entity.Transaction;
import io.github.caioeduardopereirafelix.financeapi.model.enums.CategoryName;
import io.github.caioeduardopereirafelix.financeapi.model.enums.TransactionalType;
import io.github.caioeduardopereirafelix.financeapi.model.mapper.TransactionMapper;
import io.github.caioeduardopereirafelix.financeapi.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/transaction")
public class TransactionController {

    private final TransactionMapper transactionMapper;
    private final TransactionService service;

    @Value("${app.zone:America/Sao_Paulo}")
    private ZoneId zone;

    @PostMapping
    public ResponseEntity<ResponseTransactionDTO> create(@Valid @RequestBody CreateTransactionRequestDTO requestTransaction){

        var transaction = service.create(requestTransaction);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transactionMapper.toResponse(transaction));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResponseTransactionDTO> findById(@PathVariable("id") UUID id){

        var transaction = service.findByIdForAuthenticatedUser(id);

        return ResponseEntity.ok(transactionMapper.toResponse(transaction));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") UUID id){

        service.deleteTransaction(id);

        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<ResponseTransactionDTO> putTransaction
            (@PathVariable("id") UUID id,
             @Valid @RequestBody UpdateTransactionDTO transactionDTO){

        Transaction transactionUpdate = service.updateTransaction(id, transactionDTO);

        return ResponseEntity.ok(transactionMapper.toResponse(transactionUpdate));
    }

    @PatchMapping("/{id}/category")
    public ResponseEntity<CategoryChangeResponseDTO> updateCategory(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateCategoryRequestDTO request){

        var change = service.updateCategory(id, request.category(), request.applyToSimilar());

        return ResponseEntity.ok(new CategoryChangeResponseDTO(
                transactionMapper.toResponse(change.transaction()), change.updated()));
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
                             sort = "occurredAt",
                             direction = Sort.Direction.DESC)Pageable pageable){

        Instant occurredFrom = startOf(startDate);
        Instant occurredBefore = endOfInclusive(endDate);

        Page<Transaction> transactions = service.findTransactionsWithFilters(
                type,
                category,
                description,
                minAmount,
                maxAmount,
                occurredFrom,
                occurredBefore,
                pageable
        );

        Page<ResponseTransactionDTO> response = transactions
                .map(transactionMapper::toResponse);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/summary")
    public ResponseEntity<SummaryResponseDTO> summary(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate){

        requireOrderedPeriod(startDate, endDate);

        return ResponseEntity.ok(service.getSummary(startOf(startDate), endOfInclusive(endDate)));
    }

    @GetMapping("/summary/by-category")
    public ResponseEntity<List<CategoryTotalDTO>> summaryByCategory(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate){

        requireOrderedPeriod(startDate, endDate);

        return ResponseEntity.ok(service.getTotalsByCategory(startOf(startDate), endOfInclusive(endDate)));
    }

    private Instant startOf(LocalDate date) {
        return date != null ? date.atStartOfDay(zone).toInstant() : null;
    }

    private Instant endOfInclusive(LocalDate date) {
        return date != null ? date.plusDays(1).atStartOfDay(zone).toInstant() : null;
    }

    private void requireOrderedPeriod(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new InvalidFieldException("startDate", "A data inicial deve ser anterior ou igual a final");
        }
    }
}

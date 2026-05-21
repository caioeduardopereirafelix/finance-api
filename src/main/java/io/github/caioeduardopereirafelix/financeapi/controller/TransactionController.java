package io.github.caioeduardopereirafelix.financeapi.controller;

import io.github.caioeduardopereirafelix.financeapi.model.dto.transaction.CreateTransactionRequestDTO;
import io.github.caioeduardopereirafelix.financeapi.model.dto.transaction.ResponseTransactionDTO;
import io.github.caioeduardopereirafelix.financeapi.model.dto.transaction.SummaryResponseDTO;
import io.github.caioeduardopereirafelix.financeapi.model.dto.transaction.UpdateTransactionDTO;
import io.github.caioeduardopereirafelix.financeapi.model.entity.Transaction;
import io.github.caioeduardopereirafelix.financeapi.model.mapper.TransactionMapper;
import io.github.caioeduardopereirafelix.financeapi.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/transaction")
public class TransactionController {

    private final TransactionMapper transactionMapper;
    private final TransactionService service;

    @PostMapping
    public ResponseEntity<ResponseTransactionDTO> create(@Valid @RequestBody CreateTransactionRequestDTO requestTransaction){

        var transaction = service.create(requestTransaction);

        var response = new ResponseTransactionDTO(transaction.getId(), transaction.getDescription(), transaction.getAmount(), transaction.getCategory(), transaction.getType());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseTransactionDTO> delete(@PathVariable("id") String id){

        Optional<Transaction> findTransactional = service.findById(UUID.fromString(id));

        if (findTransactional.isEmpty()){
            throw new RuntimeException("transaction not found");
        }

        var transactionFound = findTransactional.get();
        service.deleteTransaction(transactionFound);

        var response = new ResponseTransactionDTO(transactionFound.getId(), transactionFound.getDescription(), transactionFound.getAmount(), transactionFound.getCategory(), transactionFound.getType());

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ResponseTransactionDTO> putTransaction
            (@PathVariable("id") String id,
             @RequestBody UpdateTransactionDTO transactionDTO){

        var idTransaction = UUID.fromString(id);

        Transaction transactionUpdate = service.updateTrasaction(idTransaction, transactionDTO);

        return ResponseEntity.ok(new ResponseTransactionDTO(
                transactionUpdate.getId(),
                transactionUpdate.getDescription(),
                transactionUpdate.getAmount(),
                transactionUpdate.getCategory(),
                transactionUpdate.getType()));
    }

    //criar botao para editar transacao no front, ver porque mesmo sem despesas saldo nao atualiza

    @GetMapping
    public ResponseEntity<List<ResponseTransactionDTO>> findAllTransactions(){
        List<Transaction> transactions = service.findAllTransactionsForAuthenticatedUser();

        List<ResponseTransactionDTO> response = transactions
                .stream()
                .map(transactionMapper::toResponse)
                .toList();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/summary")
    public ResponseEntity<SummaryResponseDTO> summary(){

        return ResponseEntity.ok(service.getSummary());
    }
}

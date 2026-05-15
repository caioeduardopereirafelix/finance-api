package io.github.caioeduardopereirafelix.financeapi.model.mapper;

import io.github.caioeduardopereirafelix.financeapi.model.dto.transaction.ResponseTransactionDTO;
import io.github.caioeduardopereirafelix.financeapi.model.entity.Transaction;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TransactionMapper {

    ResponseTransactionDTO toResponse(Transaction transaction);
}

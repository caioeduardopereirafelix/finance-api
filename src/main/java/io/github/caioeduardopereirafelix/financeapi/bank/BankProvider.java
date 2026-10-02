package io.github.caioeduardopereirafelix.financeapi.bank;

import java.time.Instant;
import java.util.List;

public interface BankProvider {

    String name();

    String createConnectToken(String userReference);

    String createUpdateToken(String externalId, String userReference);

    ExternalConnection describeConnection(String externalId, String userReference);

    void disconnect(String externalId);

    List<ExternalTransaction> fetchTransactions(String externalId, Instant since);
}

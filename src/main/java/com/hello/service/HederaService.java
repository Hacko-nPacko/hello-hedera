package com.hello.service;

import com.hedera.hashgraph.sdk.*;
import io.github.cdimascio.dotenv.Dotenv;
import lombok.extern.log4j.Log4j2;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HederaService {
    private final Client client;

    public HederaService() {
        AccountId myAccountId = AccountId.fromString(Dotenv.load().get("MY_ACCOUNT_ID"));
        PublicKey myPublicKey = PublicKey.fromString(Dotenv.load().get("MY_PUBLIC_KEY"));
        log.info("Account {} {}", myAccountId, myPublicKey);

        PrivateKey myPrivateKey = PrivateKey.fromString(Dotenv.load().get("MY_PRIVATE_KEY"));

        client = Client.forTestnet();
        client.setOperator(myAccountId, myPrivateKey);
    }

    public AccountId createAccount() {
        try {
            PrivateKey newAccountPrivateKey = PrivateKey.generateED25519();
            PublicKey newAccountPublicKey = newAccountPrivateKey.getPublicKey();
            TransactionResponse newAccount = new AccountCreateTransaction()
                    .setKey(newAccountPublicKey)
                    .setInitialBalance(Hbar.fromTinybars(1000))
                    .execute(client);
            return newAccount.getReceipt(client).accountId;
        } catch (Exception e) {
            throw new RuntimeException("Couldn't perform the operation: " + e.getMessage(), e);
        }
    }

    public AccountBalance getBalance(AccountId accountId) {
        try {
            return new AccountBalanceQuery()
                    .setAccountId(accountId)
                    .execute(client);
        } catch (Exception e) {
            throw new RuntimeException("Couldn't perform the operation: " + e.getMessage(), e);
        }
    }
}

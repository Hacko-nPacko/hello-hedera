package com.hello.service;

import com.hedera.hashgraph.sdk.*;
import io.github.cdimascio.dotenv.Dotenv;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Callable;

@Slf4j
public class HederaService {

    @Getter
    private final AccountId mainAccount;

    @Getter
    private final Client client;

    public HederaService() {
        Dotenv env = Dotenv.load();
        mainAccount = AccountId.fromString(env.get("MY_ACCOUNT_ID"));
        PublicKey myPublicKey = PublicKey.fromString(env.get("MY_PUBLIC_KEY"));
        log.info("Account {} {}", mainAccount, myPublicKey);

        PrivateKey myPrivateKey = PrivateKey.fromString(env.get("MY_PRIVATE_KEY"));

        client = Client.forTestnet();
        client.setOperator(mainAccount, myPrivateKey);
    }

    public AccountId createAccount() {
        return run(() -> {
            PrivateKey newAccountPrivateKey = PrivateKey.generateED25519();
            PublicKey newAccountPublicKey = newAccountPrivateKey.getPublicKey();
            TransactionResponse newAccount = new AccountCreateTransaction()
                    .setKey(newAccountPublicKey)
                    .setInitialBalance(Hbar.fromTinybars(1000))
                    .execute(client);
            return newAccount.getReceipt(client).accountId;
        });
    }

    public AccountId createAccount(PublicKey publicKey) {
        return run(() -> {
            TransactionResponse newAccount = new AccountCreateTransaction()
                    .setKey(publicKey)
                    .setInitialBalance(Hbar.from(10))
                    .execute(client);
            return newAccount.getReceipt(client).accountId;
        });
    }

    public AccountBalance getBalance(AccountId accountId) {
        return run(() -> new AccountBalanceQuery()
                .setAccountId(accountId)
                .execute(client));
    }

    public TransactionReceipt transfer(AccountId from, AccountId to, long hbars) {
        return run(() -> {
            Hbar value = Hbar.fromTinybars(hbars);
            return new TransferTransaction()
                    .addHbarTransfer(from, value.negated()) //Sending account
                    .addHbarTransfer(to, value) //Receiving account
                    .execute(client)
                    .getReceipt(client);
        });
    }

    public Hbar getBalanceCost() {
        return run(() -> new AccountBalanceQuery()
                .setAccountId(getMainAccount())
                .getCost(client));
    }

    public <T> T run(Callable<T> callable) {
        try {
            return callable.call();
        } catch (Exception e) {
            throw new RuntimeException("Couldn't perform the operation: " + e.getMessage(), e);
        }
    }

    public void close() {
        run((Callable<Void>) () -> {
            client.close();
            return null;
        });
    }

    public AccountInfo info(AccountId accountId) {
        return run(() -> new AccountInfoQuery()
                .setAccountId(accountId)
                .execute(client)
        );
    }

}

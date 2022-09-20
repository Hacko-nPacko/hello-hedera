package com.hello.service;

import com.hedera.hashgraph.sdk.*;
import io.github.cdimascio.dotenv.Dotenv;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

@Slf4j
public class HederaService {

    @Getter
    private final AccountId mainAccount;

    @Getter
    private final PublicKey mainPublicKey;
    
    @Getter
    private final PrivateKey mainPrivateKey;
    
    @Getter
    private final Client client;

    public HederaService() {
        Dotenv env = Dotenv.load();
        mainAccount = AccountId.fromString(env.get("MY_ACCOUNT_ID"));
        mainPublicKey = PublicKey.fromString(env.get("MY_PUBLIC_KEY"));
        log.info("Account {} {}", mainAccount, mainPublicKey);

        mainPrivateKey = PrivateKey.fromString(env.get("MY_PRIVATE_KEY"));

        client = Client.forTestnet();
        client.setOperator(mainAccount, mainPrivateKey);
    }

    public AccountId createAccount() {
        return run(() -> {
            PrivateKey newAccountPrivateKey = PrivateKey.generateED25519();
            PublicKey newAccountPublicKey = newAccountPrivateKey.getPublicKey();

            AccountId newAccountIdPublicKey = newAccountPublicKey.toAccountId(0, 0);
            log.info("New account id (pub key): {}", newAccountIdPublicKey);
            log.info("New account priv key: {}", newAccountPrivateKey);
            log.info("New account pub key: {}", newAccountPublicKey);
            log.info("New account priv key (RAW EVM): 0x{}", newAccountPrivateKey.toStringRaw());
            log.info("New account pub key (RAW): 0x{}", newAccountPublicKey.toStringRaw());
            log.info("New account pub key (DER): 0x{}", newAccountPublicKey.toStringDER());

            TransactionResponse newAccount = new AccountCreateTransaction()
                    .setKey(newAccountPublicKey)
                    .setInitialBalance(Hbar.fromTinybars(1000))
                    .execute(client);
            AccountId accountId = newAccount.getReceipt(client).accountId;
            log.info("New account id: {}", accountId);
            return accountId;
        });
    }

    public Triple<AccountId, KeyList, List<PrivateKey>> createAccount(int size, int threshold) {
        return run(() -> {
            ArrayList<PrivateKey> keys = new ArrayList<>();
            KeyList keyList = KeyList.withThreshold(threshold);
            for (int i = 0; i < size; i++) {
                PrivateKey key = PrivateKey.generateED25519();
                keys.add(key);
                keyList.add(key);
            }

            TransactionResponse newAccount = new AccountCreateTransaction()
                    .setKey(keyList)
                    .setInitialBalance(Hbar.fromTinybars(1000))
                    .execute(client);
            AccountId accountId = newAccount.getReceipt(client).accountId;


            return Triple.of(accountId, keyList, keys);
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

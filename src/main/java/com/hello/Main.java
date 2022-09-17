package com.hello;

import com.hedera.hashgraph.sdk.AccountId;
import com.hello.service.HederaService;
import io.github.cdimascio.dotenv.Dotenv;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Main {

    @SneakyThrows
    public static void main(String[] args) {
        log.info("{}", Dotenv.load());
        HederaService service = new HederaService();
        AccountId accountId = service.createAccount();
        log.debug("accountId: {}", accountId);
//        AccountBalance balance = service.getBalance(accountId);
//        log.info("balance: {}", balance);

        // https://docs.hedera.com/guides/getting-started/create-an-account
        // https://github.com/hashgraph/hedera-sdk-java/tree/main/examples/src/main/java
    }
}

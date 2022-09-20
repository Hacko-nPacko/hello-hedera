package com.hello;

import com.hedera.hashgraph.sdk.*;
import com.hello.service.HederaService;
import io.github.cdimascio.dotenv.Dotenv;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;

@Slf4j
public class Main {

    private static final Dotenv env = Dotenv.load();

    public static void main(String[] args) throws Exception {
        HederaService service = new HederaService();

        // https://github.com/hashgraph/hedera-sdk-java/tree/main/examples/src/main/java
        // https://docs.hedera.com/guides/getting-started/try-examples

//        Examples.nftContract(service);

        AccountId account = service.createAccount();

    }
}

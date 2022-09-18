package com.hello;

import com.hedera.hashgraph.sdk.*;
import com.hello.service.HederaService;
import io.github.cdimascio.dotenv.Dotenv;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

@Slf4j
public class Examples {

    public static void accounts(HederaService service, Dotenv env) {
        AccountId account1 = AccountId.fromString(env.get("ACCOUNT_1"));
        AccountId account2 = AccountId.fromString(env.get("ACCOUNT_2"));

        AccountId accountId = service.createAccount();
        log.debug("accountId: {}", accountId);
        AccountBalance balance = service.getBalance(accountId);
        log.info("balance: {}", balance);
        TransactionReceipt transfer1 = service.transfer(service.getMainAccount(), account1, 100);
        TransactionReceipt transfer2 = service.transfer(service.getMainAccount(), account2, 100);
        log.info("transfer1: {}", transfer1.status);
        log.info("transfer2: {}", transfer2.status);


        log.info("balance main: {}", service.getBalance(service.getMainAccount()));
        log.info("balance 1: {}", service.getBalance(account1));
        log.info("balance 2: {}", service.getBalance(account2));

        log.info("balance cost: {}", service.getBalanceCost());


        PrivateKey privateKey = PrivateKey.generateED25519();
        PublicKey publicKey = privateKey.getPublicKey();

        // Assuming that the target shard and realm are known.
        // For now they are virtually always 0 and 0.
        AccountId aliasAccountId = publicKey.toAccountId(0, 0);
        log.info("New account ID: {}", aliasAccountId);
        log.info("Just the aliasKey: {}", aliasAccountId.aliasKey);

        AccountId newAccountId = service.createAccount(aliasAccountId.aliasKey);
        service.transfer(service.getMainAccount(), newAccountId, 100);
        log.info("new account balance: {}", service.getBalance(newAccountId));
        AccountInfo info = service.info(newAccountId);
        log.info("info {}", info);

        AccountId.fromString("0.0.302a300506032b6570032100caf4523b2db9045aed2a4b2e41a7fcc7b15e48d0c757cc8e53ea687e29726a58");

    }

    public static void channels(HederaService service) throws Exception {
        Client client = service.getClient();

        //Create a new topic
        TransactionResponse txResponse = new TopicCreateTransaction()
                .execute(client);

        //Get the receipt
        TransactionReceipt receipt = txResponse.getReceipt(client);

        //Get the topic ID
        TopicId topicId = receipt.topicId;

        //Log the topic ID
        log.info("Your topic ID is: {}", topicId);

        // Wait 5 seconds between consensus topic creation and subscription creation
        Thread.sleep(5000);

        SubscriptionHandle subscribe = new TopicMessageQuery()
                .setTopicId(topicId)
                .subscribe(client, resp -> {
                    String messageAsString = new String(resp.contents, StandardCharsets.UTF_8);
                    log.info(resp.consensusTimestamp + " received topic message: " + messageAsString);
                });

        //Submit a message to a topic
        for (int i = 0; i < 3; i++) {
            TransactionResponse submitMessage = new TopicMessageSubmitTransaction()
                    .setTopicId(topicId)
                    .setMessage("hello, HCS! " + i)
                    .execute(client);
            //Get the receipt of the transaction
            TransactionReceipt receipt2 = submitMessage.getReceipt(client);

            log.info("submit: {}", receipt2);
        }

        //Prevent the main thread from existing so the topic message can be returned and printed to the console
        Thread.sleep(10000);

        log.info("unsubscribing");
        subscribe.unsubscribe();
        Thread.sleep(5000);
        log.info("closing client");
        service.close();
    }

    public static void nft(HederaService service) throws Exception {
        Client client = service.getClient();

        //Treasury Key
        PrivateKey treasuryKey = PrivateKey.generateED25519();
        PublicKey treasuryPublicKey = treasuryKey.getPublicKey();

        //Create treasury account
        AccountId treasuryId = service.createAccount(treasuryPublicKey);

        //Alice Key
        PrivateKey aliceKey = PrivateKey.generateED25519();
        PublicKey alicePublicKey = aliceKey.getPublicKey();

        AccountId aliceAccountId = service.createAccount(alicePublicKey);

        //Supply Key
        PrivateKey supplyKey = PrivateKey.generateED25519();
        PublicKey supplyPublicKey = supplyKey.getPublicKey();

        //Create the NFT
        TokenCreateTransaction nftCreate = new TokenCreateTransaction()
                .setTokenName("diploma")
                .setTokenSymbol("GRAD")
                .setTokenType(TokenType.NON_FUNGIBLE_UNIQUE)
                .setDecimals(0)
                .setInitialSupply(0)
                .setTreasuryAccountId(treasuryId)
                .setSupplyType(TokenSupplyType.FINITE)
                .setMaxSupply(250)
                .setSupplyKey(supplyKey)
                .freezeWith(client);


        //Sign the transaction with the treasury key
        TokenCreateTransaction nftCreateTxSign = nftCreate.sign(treasuryKey);

        //Submit the transaction to a Hedera network
        TransactionResponse nftCreateSubmit = nftCreateTxSign.execute(client);

        //Get the transaction receipt
        TransactionReceipt nftCreateRx = nftCreateSubmit.getReceipt(client);

        //Get the token ID
        TokenId tokenId = nftCreateRx.tokenId;

        //Log the token ID
        log.info("Created NFT with token ID {}", nftCreateRx);

        // IPFS CONTENT IDENTIFIERS FOR WHICH WE WILL CREATE NFT
        String CID = ("QmXZpiASmJKGkuSPLU8rqug3187zPjWmwboYR1xpqwnm4m");

        // MINT NEW NFT
        for (int i = 0; i < 10; i++) {
            TokenMintTransaction mintTx = new TokenMintTransaction()
                    .setTokenId(tokenId)
                    .addMetadata(CID.getBytes())
                    .freezeWith(client);

            //Sign with the supply key
            TokenMintTransaction mintTxSign = mintTx.sign(supplyKey);

            //Submit the transaction to a Hedera network
            TransactionResponse mintTxSubmit = mintTxSign.execute(client);

            //Get the transaction receipt
            TransactionReceipt mintRx = mintTxSubmit.getReceipt(client);

            //Log the serial number
            log.info("Created NFT " + tokenId + "with serial: " + mintRx.serials);
        }
        // assoc with alice!

        //Create the associate transaction and sign with Alice's key
        TokenAssociateTransaction associateAliceTx = new TokenAssociateTransaction()
                .setAccountId(aliceAccountId)
                .setTokenIds(Collections.singletonList(tokenId))
                .freezeWith(client)
                .sign(aliceKey);

        //Submit the transaction to a Hedera network
        TransactionResponse associateAliceTxSubmit = associateAliceTx.execute(client);

        //Get the transaction receipt
        TransactionReceipt associateAliceRx = associateAliceTxSubmit.getReceipt(client);

        //Confirm the transaction was successful
        log.info("NFT association with Alice's account: " + associateAliceRx.status);

        // check balances before
        log.info("Treasury balance: {}", service.getBalance(treasuryId));
        log.info("Alice's balance: {}", service.getBalance(aliceAccountId));

        // Transfer NFT from treasury to Alice
        // Sign with the treasury key to authorize the transfer
        TransferTransaction tokenTransferTx = new TransferTransaction()
                .addNftTransfer(new NftId(tokenId, 1), treasuryId, aliceAccountId)
                .freezeWith(client)
                .sign(treasuryKey);

        TransactionResponse tokenTransferSubmit = tokenTransferTx.execute(client);
        TransactionReceipt tokenTransferRx = tokenTransferSubmit.getReceipt(client);

        log.info("NFT transfer from Treasury to Alice: {}", tokenTransferRx);

        // check balances after
        log.info("Treasury balance: {}", service.getBalance(treasuryId));
        log.info("Alice's balance: {}", service.getBalance(aliceAccountId));

        List<TokenNftInfo> infos = new TokenNftInfoQuery()
                .setNftId(new NftId(tokenId, 1))
                .execute(client);
        log.info("nft infos: {}", infos);
    }

    public static void token(HederaService service) throws Exception {
        Client client = service.getClient();

        //Treasury Key
        PrivateKey treasuryKey = PrivateKey.generateED25519();
        PublicKey treasuryPublicKey = treasuryKey.getPublicKey();

        //Create treasury account
        AccountId treasuryId = service.createAccount(treasuryPublicKey);

        //Alice Key
        PrivateKey aliceKey = PrivateKey.generateED25519();
        PublicKey alicePublicKey = aliceKey.getPublicKey();

        AccountId aliceAccountId = service.createAccount(alicePublicKey);

        //Supply Key
        PrivateKey supplyKey = PrivateKey.generateED25519();
        PublicKey supplyPublicKey = supplyKey.getPublicKey();


        // CREATE FUNGIBLE TOKEN (STABLECOIN)
        TokenCreateTransaction tokenCreateTx = new TokenCreateTransaction()
                .setTokenName("USD Bar")
                .setTokenSymbol("USDB")
                .setTokenType(TokenType.FUNGIBLE_COMMON)
                .setDecimals(2)
                .setInitialSupply(10000)
                .setTreasuryAccountId(treasuryId)
                .setSupplyType(TokenSupplyType.INFINITE)
                .setSupplyKey(supplyKey)
                .freezeWith(client);

        //Sign with the treasury key
        TokenCreateTransaction tokenCreateSign = tokenCreateTx.sign(treasuryKey);

        //Submit the transaction
        TransactionResponse tokenCreateSubmit = tokenCreateSign.execute(client);

        //Get the transaction receipt
        TransactionReceipt tokenCreateRx = tokenCreateSubmit.getReceipt(client);

        //Get the token ID
        TokenId tokenId = tokenCreateRx.tokenId;

        //Log the token ID to the console
        log.info("Created token with ID: {}", tokenCreateRx);

        // TOKEN ASSOCIATION WITH ALICE's ACCOUNT
        TokenAssociateTransaction associateAliceTx = new TokenAssociateTransaction()
                .setAccountId(aliceAccountId)
                .setTokenIds(Collections.singletonList(tokenId))
                .freezeWith(client)
                .sign(aliceKey);

        //Submit the transaction
        TransactionResponse associateAliceTxSubmit = associateAliceTx.execute(client);

        //Get the receipt of the transaction
        TransactionReceipt associateAliceRx = associateAliceTxSubmit.getReceipt(client);

        //Get the transaction status
        log.info("Token association with Alice's account: {}", associateAliceRx);

        // BALANCE CHECK
        log.info("Treasury balance {}: {}", tokenId, service.getBalance(treasuryId));
        log.info("Alice's  balance {}: {}", tokenId, service.getBalance(aliceAccountId));

        // TRANSFER STABLECOIN FROM TREASURY TO ALICE
        TransferTransaction tokenTransferTx = new TransferTransaction()
                .addTokenTransferWithDecimals(tokenId, treasuryId, -2500, 2)
                .addTokenTransferWithDecimals(tokenId, aliceAccountId, 2500, 2)
                .freezeWith(client)
                .sign(treasuryKey);

        //SUBMIT THE TRANSACTION
        TransactionResponse tokenTransferSubmit = tokenTransferTx.execute(client);

        //GET THE RECEIPT OF THE TRANSACTION
        TransactionReceipt tokenTransferRx = tokenTransferSubmit.getReceipt(client);

        //LOG THE TRANSACTION STATUS
        log.info("Stablecoin transfer from Treasury to Alice: {}, {}", tokenTransferRx.status, tokenTransferSubmit.transactionId);

        // BALANCE CHECK
        log.info("Treasury balance {}: {}", tokenId, service.getBalance(treasuryId));
        log.info("Alice's  balance {}: {}", tokenId, service.getBalance(aliceAccountId));

        log.debug("DONE");
    }
}

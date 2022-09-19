package com.hello;

import com.google.gson.JsonObject;
import com.google.protobuf.ByteString;
import com.hedera.hashgraph.sdk.*;
import com.hello.service.HederaService;
import com.hello.util.FileUtil;
import io.github.cdimascio.dotenv.Dotenv;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Triple;

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

    public static void schedule(HederaService service) throws Exception {
        Client client = service.getClient();

        Triple<AccountId, KeyList, List<PrivateKey>> senderAccountPair = service.createAccount(5, 3);
        AccountId senderAccount = senderAccountPair.getLeft();
        AccountId recipientAccount = service.createAccount();

        PrivateKey ownerKey = senderAccountPair.getRight().get(0);
        PrivateKey signerKey1 = senderAccountPair.getRight().get(1);
        PrivateKey signerKey2 = senderAccountPair.getRight().get(2);

        //Create a transaction to schedule
        TransferTransaction transaction = new TransferTransaction()
                .addHbarTransfer(senderAccount, Hbar.fromTinybars(-100))
                .addHbarTransfer(recipientAccount, Hbar.fromTinybars(100));

        //Schedule a transaction
        TransactionResponse scheduleTransaction = new ScheduleCreateTransaction()
                .setScheduledTransaction(transaction)
                .freezeWith(client)
                .sign(ownerKey)
                .execute(client);

        //Get the receipt of the transaction
        TransactionReceipt receipt = scheduleTransaction.getReceipt(client);

        //Get the schedule ID
        ScheduleId scheduleId = receipt.scheduleId;
        log.info("The schedule ID is {}", scheduleId);

        //Get the scheduled transaction ID
        TransactionId scheduledTxId = receipt.scheduledTransactionId;
        log.info("The scheduled transaction ID is {}", scheduledTxId);

        //Submit the first signatures
        TransactionResponse signature1 = new ScheduleSignTransaction()
                .setScheduleId(scheduleId)
                .freezeWith(client)
                .sign(signerKey1)
                .execute(client);

        //Verify the transaction was successful and submit a schedule info request
        TransactionReceipt receipt1 = signature1.getReceipt(client);
        log.info("The transaction status is {}", receipt1.status);

        ScheduleInfo query1 = new ScheduleInfoQuery()
                .setScheduleId(scheduleId)
                .execute(client);

        //Confirm the signature was added to the schedule
        log.info("q1 {}", query1);


        //Submit the second signature
        TransactionResponse signature2 = new ScheduleSignTransaction()
                .setScheduleId(scheduleId)
                .freezeWith(client)
                .sign(signerKey2)
                .execute(client);

        //Verify the transaction was successful
        TransactionReceipt receipt2 = signature2.getReceipt(client);
        log.info("The transaction status {}", receipt2.status);


        //Get the schedule info
        ScheduleInfo query2 = new ScheduleInfoQuery()
                .setScheduleId(scheduleId)
                .execute(client);

        log.info("q2 {}", query2);


        //Get the scheduled transaction record
        TransactionRecord scheduledTxRecord = TransactionId.fromString(scheduledTxId.toString()).getRecord(client);
        log.info("The scheduled transaction record is: {}", scheduledTxRecord);

        log.debug("DONE");
    }

    public static void contract(HederaService service) throws Exception {
        Client client = service.getClient();
        JsonObject jsonObject = FileUtil.json("solcjs/hello_hedera.json");

        //Store the "object" field from the HelloHedera.json file as hex-encoded bytecode
        String object = jsonObject.getAsJsonObject("data")
                .getAsJsonObject("bytecode")
                .get("object")
                .getAsString();
        byte[] bytecode = object.getBytes(StandardCharsets.UTF_8);

        //Create a file on Hedera and store the hex-encoded bytecode
        FileCreateTransaction fileCreateTx = new FileCreateTransaction()
                //Set the bytecode of the contract
                .setContents(bytecode);

        //Submit the file to the Hedera test network signing with the transaction fee payer key specified with the client
        TransactionResponse submitTx = fileCreateTx.execute(client);

        //Get the receipt of the file create transaction
        TransactionReceipt fileReceipt = submitTx.getReceipt(client);

        //Get the file ID from the receipt
        FileId bytecodeFileId = fileReceipt.fileId;

        //Log the file ID
        log.info("The smart contract bytecode file ID is {}", bytecodeFileId);


        // Instantiate the contract instance
        ContractCreateTransaction contractTx = new ContractCreateTransaction()
                //Set the file ID of the Hedera file storing the bytecode
                .setBytecodeFileId(bytecodeFileId)
                //Set the gas to instantiate the contract
                .setGas(100_000)
                //Provide the constructor parameters for the contract
                .setConstructorParameters(new ContractFunctionParameters().addString("Hello from Hedera!"));

        //Submit the transaction to the Hedera test network
        TransactionResponse contractResponse = contractTx.execute(client);

        //Get the receipt of the file create transaction
        TransactionReceipt contractReceipt = contractResponse.getReceipt(client);

        //Get the smart contract ID
        ContractId newContractId = contractReceipt.contractId;

        //Log the smart contract ID
        log.info("The smart contract ID is {}", newContractId);

        // Calls a function of the smart contract
        ContractCallQuery contractQuery = new ContractCallQuery()
                //Set the gas for the query
                .setGas(100000)
                //Set the contract ID to return the request for
                .setContractId(newContractId)
                //Set the function of the contract to call
                .setFunction("get_message")
                //Set the query payment for the node returning the request
                //This value must cover the cost of the request otherwise will fail
                .setQueryPayment(new Hbar(2));

        //Submit to a Hedera network
        ContractFunctionResult getMessage = contractQuery.execute(client);
        //Get the message
        String message = getMessage.getString(0);

        //Log the message
        log.info("The contract message: {}", message);


        //Create the transaction to update the contract message
        ContractExecuteTransaction contractExecTx = new ContractExecuteTransaction()
                //Set the ID of the contract
                .setContractId(newContractId)
                //Set the gas for the call
                .setGas(100_000)
                //Set the function of the contract to call
                .setFunction("set_message", new ContractFunctionParameters().addString("Hello from Hedera again!"));

        //Submit the transaction to a Hedera network and store the response
        TransactionResponse submitExecTx = contractExecTx.execute(client);

        //Get the receipt of the transaction
        TransactionReceipt receipt2 = submitExecTx.getReceipt(client);

        //Confirm the transaction was executed successfully
        log.info("The transaction status is: {}", receipt2.status);

        //Query the contract for the contract message
        ContractCallQuery contractCallQuery = new ContractCallQuery()
                //Set ID of the contract to query
                .setContractId(newContractId)
                //Set the gas to execute the contract call
                .setGas(100_000)
                //Set the contract function
                .setFunction("get_message")
                //Set the query payment for the node returning the request
                //This value must cover the cost of the request otherwise will fail
                .setQueryPayment(new Hbar(2));

        //Submit the query to a Hedera network
        ContractFunctionResult contractUpdateResult = contractCallQuery.execute(client);

        //Get the updated message
        String message2 = contractUpdateResult.getString(0);

        //Log the updated message
        log.info("The contract updated message: {}", message2);
    }

    public static void hts(HederaService service) throws Exception {

        Client client = service.getClient();

        //Treasury Key
        PrivateKey treasuryKey = PrivateKey.generateED25519();
        PublicKey treasuryPublicKey = treasuryKey.getPublicKey();

        //Create treasury account
        AccountId treasuryId = service.createAccount(treasuryPublicKey);
        log.info("The treasury account ID is {}", treasuryId);

        //Create a token to interact with
        TokenCreateTransaction createToken = new TokenCreateTransaction()
                .setTokenName("HSCS demo")
                .setTokenSymbol("H")
                .setTokenType(TokenType.FUNGIBLE_COMMON)
                .setTreasuryAccountId(treasuryId)
                .setInitialSupply(500);

        //Submit the token create transaction
        TransactionResponse submitTokenTx = createToken.freezeWith(client).sign(treasuryKey).execute(client);

        //Get the token ID
        TokenId tokenId = submitTokenTx.getReceipt(client).tokenId;
        log.info("The new token ID is {}", tokenId);

        //Log the smart contract ID
        ContractId newContractId = contractId1(service);
        log.info("The smart contract ID is {}", newContractId);

        //Associate the token to an account using the HTS contract
        ContractExecuteTransaction associateToken = new ContractExecuteTransaction()
                //The contract to call
                .setContractId(newContractId)
                //The gas for the transaction
                .setGas(2_000_000)
                //The contract function to call and parameters to pass
                .setFunction("tokenAssociate", new ContractFunctionParameters()
                        //The account ID to associate the token to
                        .addAddress(service.getMainAccount().toSolidityAddress())
                        //The token ID to associate to the account
                        .addAddress(tokenId.toSolidityAddress()));

        //Sign with the account key to associate and submit to the Hedera network
        TransactionResponse associateTokenResponse = associateToken
                .freezeWith(client)
                .sign(service.getMainPrivateKey())
                .execute(client);

        log.info("The transaction status: {}", associateTokenResponse.getReceipt(client).status);

        //Get the child token associate transaction record
        TransactionRecord childRecords = new TransactionRecordQuery()
                //Set the bool flag equal to true
                .setIncludeChildren(true)
                //The transaction ID of th parent contract execute transaction
                .setTransactionId(associateTokenResponse.transactionId)
                .execute(client);

        log.info("The transaction record for the associate transaction {}", childRecords.children);

        //The balance of the account
        log.info("The {} should now be associated to my account: {}", tokenId, service.getBalance(service.getMainAccount()));

        //Transfer the new token to the account
        //Contract function params need to be in the order of the paramters provided in the tokenTransfer contract function
        ContractExecuteTransaction tokenTransfer = new ContractExecuteTransaction()
                .setContractId(newContractId)
                .setGas(2_000_000)
                .setFunction("tokenTransfer", new ContractFunctionParameters()
                        //The ID of the token
                        .addAddress(tokenId.toSolidityAddress())
                        //The account to transfer the tokens from
                        .addAddress(treasuryId.toSolidityAddress())
                        //The account to transfer the tokens to
                        .addAddress(service.getMainAccount().toSolidityAddress())
                        //The number of tokens to transfer
                        .addInt64(100));

        //Sign the token transfer transaction with the treasury account to authorize the transfer and submit
        ContractExecuteTransaction signTokenTransfer = tokenTransfer
                .freezeWith(client)
                .sign(treasuryKey);

        //Submit transfer transaction
        TransactionResponse submitTransfer = signTokenTransfer.execute(client);

        //Get transaction status
        Status txStatus = submitTransfer.getReceipt(client).status;

        //Get the transaction status
        log.info("The transfer transaction status {}", txStatus);

        //Verify your account received the 100 tokens
        log.info("My new account balance is {}", service.getBalance(service.getMainAccount()));
    }

    private static ContractId contractId1(HederaService service) throws Exception {
        Client client = service.getClient();
        byte[] bytecode = FileUtil.contents("solcjs/bin/HTS.bin").getBytes(StandardCharsets.UTF_8);

        //Create a file on Hedera and store the hex-encoded bytecode
        TransactionReceipt fileReceipt = new FileCreateTransaction()
                .setKeys(service.getMainPrivateKey())
                .execute(client)

                .getReceipt(client);

        //Get the file ID
        FileId newFileId = fileReceipt.fileId;

        //Log the file ID
        log.info("The smart contract byte code file ID is {}", newFileId);

        TransactionReceipt receipt = new FileAppendTransaction()
                .setFileId(newFileId)
                .setContents(bytecode)
                .execute(client)
                .getReceipt(client);


        log.info("File append receipt: {}", receipt);

        ByteString execute = new FileContentsQuery()
                .setFileId(newFileId)
                .execute(client);

        log.info("file contents now: {}", execute.size());

        //Deploy the contract
        ContractCreateTransaction contractTx = new ContractCreateTransaction()
                //The contract bytecode file
                .setBytecodeFileId(newFileId)
                //The max gas to reserve for this transaction
                .setGas(2_000_000);

        //Submit the transaction to the Hedera test network
        TransactionReceipt contractReceipt = contractTx.execute(client).getReceipt(client);

        //Get the smart contract ID
        return contractReceipt.contractId;
    }

    private static ContractId contractId2(HederaService service) throws Exception {
        Client client = service.getClient();

        return null;

    }

    public static void nftContract(HederaService service) throws Exception {
        Client client = service.getClient();

        // ipfs URI
        // QmPMxuj5cBmaiwaYJb1X7PkQA3gWFsKNCXvE2YcCCuoS9m nft_contract_metadata.json
        String metadata = ("ipfs://bafybeiapghmnad5b7szsr45lt4otk75dvjnqdl74nhjozqrdytgbnhb4ca/nft_contract_metadata.json");
        byte[][] byteArray = new byte[1][metadata.length()];
        byteArray[0] = metadata.getBytes();

        PrivateKey aliceKey = PrivateKey.generateED25519();
        AccountId aliceId = service.createAccount(aliceKey.getPublicKey());
        log.info("alice account: {}", aliceId);

//        String bytecode = FileUtil.contents("solcjs/bin/NFTCreator.bin");
        String bytecode = FileUtil.contents("solcjs/bin/NFTCreator.bin");

        // Create contract
        ContractCreateFlow createContract = new ContractCreateFlow()
                .setBytecode(bytecode) // Contract bytecode
                .setGas(150_000); // Increase if revert

        TransactionResponse createContractTx = createContract.execute(client);
        TransactionReceipt createContractRx = createContractTx.getReceipt(client);
        // Get the new contract ID
        ContractId newContractId = createContractRx.contractId;

        log.info("Contract created with ID: {}", newContractId);

        // Create NFT using contract
        ContractExecuteTransaction createToken = new ContractExecuteTransaction()
                .setContractId(newContractId) // Contract id
                .setGas(1_000_000) // Increase if revert
                .setPayableAmount(new Hbar(500)) // Increase if revert
                .setFunction("createNft", new ContractFunctionParameters()
                        .addString("Fall Collection") // NFT Name
                        .addString("LEAF") // NFT Symbol
                        .addString("Just a memo") // NFT Memo
                        .addInt64(10) // NFT max supply
                        .addUint32(7_000_000)); // Expiration: Needs to be between 6999999 and 8000001

        TransactionResponse createTokenTx = createToken.execute(client);
        TransactionRecord createTokenRx = createTokenTx.getRecord(client);

        String tokenIdSolidityAddr = createTokenRx.contractFunctionResult.getAddress(0);
        AccountId tokenId = AccountId.fromSolidityAddress(tokenIdSolidityAddr);

        log.info("Token created with ID: {}", tokenId);

        // Mint NFT
        ContractExecuteTransaction mintToken = new ContractExecuteTransaction()
                .setContractId(newContractId)
                .setGas(1_500_000)
                .setFunction("mintNft", new ContractFunctionParameters()
                        .addAddress(tokenIdSolidityAddr) // Token address
                        .addBytesArray(byteArray)); // Metadata

        TransactionResponse mintTokenTx = mintToken.execute(client);
        TransactionRecord mintTokenRx = mintTokenTx.getRecord(client);
        // NFT serial number
        long serial = mintTokenRx.contractFunctionResult.getInt64(0);

        log.info("Minted NFT with serial: " + serial);

        // Transfer NFT to Alice
        ContractExecuteTransaction transferToken = new ContractExecuteTransaction()
                .setContractId(newContractId)
                .setGas(1_500_000)
                .setFunction("transferNft", new ContractFunctionParameters()
                        .addAddress(tokenIdSolidityAddr) // Token id
                        .addAddress(aliceId.toSolidityAddress()) // Token receiver (Alice)
                        .addInt64(serial)) // Serial number
                .freezeWith(client) // Freeze transaction using client
                .sign(aliceKey); //Sign using Alice Private Key

        TransactionResponse transferTokenTx = transferToken.execute(client);
        TransactionReceipt transferTokenRx = transferTokenTx.getReceipt(client);

        log.info("Transfer status: {}", transferTokenRx.status);
    }
}

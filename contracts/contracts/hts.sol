// SPDX-License-Identifier: Apache-2.0
pragma solidity ^0.8.0;

import "./lib/HederaTokenService.sol";
import "./lib/HederaResponseCodes.sol";


contract HTS is HederaTokenService {

    function tokenAssociate(address sender, address tokenAddress) external {
        int response = HederaTokenService.associateToken(sender, tokenAddress);

        if (response != HederaResponseCodes.SUCCESS) {
            revert ("HTS: Associate Failed");
        }
    }

    function tokenTransfer(address tokenId, address fromAccountId , address toAccountId , int64 tokenAmount) external {
        int response = HederaTokenService.transferToken(tokenId, fromAccountId, toAccountId, tokenAmount);

        if (response != HederaResponseCodes.SUCCESS) {
            revert ("HTS: Transfer Failed");
        }
    }

    function tokenDissociate(address sender, address tokenAddress) external {
        int response = HederaTokenService.dissociateToken(sender, tokenAddress);

        if (response != HederaResponseCodes.SUCCESS) {
            revert ("HTS: Dissociate Failed");
        }
    }
}

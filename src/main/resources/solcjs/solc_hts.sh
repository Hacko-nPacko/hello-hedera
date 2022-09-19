#!/bin/bash

solc --bin --combined-json abi,asm,ast,bin --include-path lib --base-path . --output-dir bin --overwrite HTS.sol

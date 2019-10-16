/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.eth.sidechain

import com.d3.commons.sidechain.ChainHandler
import com.d3.commons.sidechain.SideChainEvent
import com.d3.commons.sidechain.iroha.util.IrohaQueryHelper
import com.d3.commons.sidechain.provider.ChainAddressProvider
import com.d3.eth.provider.ETH_DOMAIN
import com.d3.eth.provider.ETH_NAME
import com.d3.eth.provider.ETH_PRECISION
import com.d3.eth.provider.EthTokensProvider
import com.d3.eth.sidechain.util.DeployHelper
import com.github.kittinunf.result.fanout
import mu.KLogging
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.methods.response.EthBlock
import org.web3j.protocol.core.methods.response.Transaction
import java.math.BigDecimal
import java.math.BigInteger

/**
 * Implementation of [ChainHandler] for Ethereum side chain.
 * Extract interesting transactions from Ethereum block.
 * @param web3 - notary.endpoint of Ethereum client
 * @param ethRelayProvider - provider of observable wallets
 * @param ethTokensProvider - provider of observable tokens
 */
class EthChainHandler(
    val web3: Web3j,
    val masterAddres: String,
    val ethRelayProvider: ChainAddressProvider,
    val ethTokensProvider: EthTokensProvider,
    deployHelper: DeployHelper,
    val queryHelper: IrohaQueryHelper
) :
    ChainHandler<EthBlock> {

    private val master = deployHelper.loadMasterContract(masterAddres)

    init {
        logger.info { "Initialization of EthChainHandler with master $masterAddres" }
    }

    /**
     * Process Master contract transactions
     * @param tx - Ethereum transaction
     * @param time - time of transaction
     * @return side chain events flow
     *
     */
    private fun handleMasterCall(
        tx: Transaction,
        time: BigInteger
    ): List<SideChainEvent.PrimaryBlockChainEvent> {
        // get receipt that contains data about solidity function execution
        val receipt = web3.ethGetTransactionReceipt(tx.hash).send()
        return if (receipt.transactionReceipt.get().isStatusOK) {
            logger.info { "Master contract call" }
            receipt.transactionReceipt.get().logs
                // encoded abi method signature of register(
                //        address clientEthereumAddress,
                //        bytes memory clientIrohaAccountId,
                //        bytes32 txHash,
                //        uint8[] memory v,
                //        bytes32[] memory r,
                //        bytes32[] memory s
                //    )
                .filter { logs ->
                    logs.topics[0] == "0x6a70775b447c720635e28c6ecca0cec2b8917a93dc40135739e28dd2299ea5ab"
                }.map { log ->
                    val ethAddress = tx.from
                    val accountId = String(master.registeredClients(ethAddress).send())
                    Pair(accountId, ethAddress)
                }.map { (accountId, ethAddress) ->
                    logger.info { "Registration event txHash=${tx.hash}, time=$time, account=$accountId, ethAddress=$ethAddress" }
                    SideChainEvent.PrimaryBlockChainEvent.Registration(
                        tx.hash,
                        time,
                        accountId,
                        ethAddress
                    )
                }
        } else {
            return listOf()
        }
    }

    /**
     * Process Ethereum ERC20 tokens
     * @param tx transaction in block
     * @return list of notary events on ERC20 deposit
     */
    private fun handleErc20(
        tx: Transaction,
        time: BigInteger,
        wallets: Map<String, String>,
        tokenName: String,
        isIrohaAnchored: Boolean
    ): List<SideChainEvent.PrimaryBlockChainEvent> {
        logger.info { "Handle ERC20 tx ${tx.hash}" }

        // get receipt that contains data about solidity function execution
        val receipt = web3.ethGetTransactionReceipt(tx.hash).send()

        // if tx is committed successfully
        if (receipt.transactionReceipt.get().isStatusOK) {
            return receipt.transactionReceipt.get().logs
                .filter {
                    // filter out transfer
                    // the first topic is a hashed representation of a transfer signature call (the scary string)
                    val to = "0x" + it.topics[2].drop(26).toLowerCase()
                    it.topics[0] == "0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef" &&
                            wallets.containsKey(to)
                }
                .filter {
                    // check if amount > 0
                    if (BigInteger(it.data.drop(2), 16).compareTo(BigInteger.ZERO) > 0) {
                        true
                    } else {
                        logger.warn { "Transaction ${tx.hash} from Ethereum with 0 ERC20 amount" }
                        false
                    }
                }
                .map {
                    ethTokensProvider.getTokenPrecision(tokenName)
                        .fold(
                            { precision ->
                                // second and third topics are addresses from and to
                                val from = "0x" + it.topics[1].drop(26).toLowerCase()
                                val to = "0x" + it.topics[2].drop(26).toLowerCase()
                                // amount of transfer is stored in data
                                val amount = BigInteger(it.data.drop(2), 16)

                                if (isIrohaAnchored)
                                    SideChainEvent.PrimaryBlockChainEvent.IrohaAnchoredOnPrimaryChainDeposit(
                                        tx.hash,
                                        time,
                                        wallets[to]!!,
                                        tokenName,
                                        BigDecimal(amount, precision).toPlainString(),
                                        from
                                    )
                                else
                                    SideChainEvent.PrimaryBlockChainEvent.ChainAnchoredOnPrimaryChainDeposit(
                                        tx.hash,
                                        time,
                                        wallets[to]!!,
                                        tokenName,
                                        BigDecimal(amount, precision).toPlainString(),
                                        from
                                    )
                            },
                            { throw it }
                        )
                }
        } else {
            return listOf()
        }
    }

    /**
     * Process Ether deposit
     * @param tx transaction in block
     * @return list of notary events on Ether deposit
     */
    private fun handleEther(
        tx: Transaction,
        time: BigInteger,
        wallets: Map<String, String>
    ): List<SideChainEvent.PrimaryBlockChainEvent> {
        logger.info { "Handle Ethereum tx ${tx.hash}" }

        val receipt = web3.ethGetTransactionReceipt(tx.hash).send()

        return if (!receipt.transactionReceipt.get().isStatusOK) {
            logger.warn { "Transaction ${tx.hash} from Ethereum has FAIL status" }
            listOf()
        } else if (tx.value <= BigInteger.ZERO) {
            logger.warn { "Transaction ${tx.hash} from Ethereum with 0 ETH amount" }
            listOf()
        } else {
            // if tx amount > 0 and is committed successfully
            listOf(
                SideChainEvent.PrimaryBlockChainEvent.ChainAnchoredOnPrimaryChainDeposit(
                    tx.hash,
                    time,
                    // all non-existent keys were filtered out in parseBlock
                    wallets[tx.to]!!,
                    "$ETH_NAME#$ETH_DOMAIN",
                    BigDecimal(tx.value, ETH_PRECISION).toPlainString(),
                    tx.from
                )
            )
        }
    }

    /**
     * Parse [EthBlock] for transactions.
     * @return List of transation we are interested in
     */
    override fun parseBlock(block: EthBlock): List<SideChainEvent.PrimaryBlockChainEvent> {
        logger.info { "Ethereum chain handler for block ${block.block.number}" }

        return ethRelayProvider.getAddresses().fanout {
            ethTokensProvider.getEthAnchoredTokens().fanout {
                ethTokensProvider.getIrohaAnchoredTokens()
            }
        }.fold(
            { (wallets, tokens) ->
                val (ethAnchoredTokens, irohaAnchoredTokens) = tokens
                // Eth time in seconds, convert ot milliseconds
                val time = block.block.timestamp.multiply(BigInteger.valueOf(1000))
                block.block.transactions
                    .map { it.get() as Transaction }
                    .flatMap {
                        if (masterAddres == it.to)
                            handleMasterCall(it, time)
                        else if (wallets.containsKey(it.to))
                            handleEther(it, time, wallets)
                        else if (ethAnchoredTokens.containsKey(it.to))
                            handleErc20(it, time, wallets, ethAnchoredTokens[it.to]!!, false)
                        else if (irohaAnchoredTokens.containsKey(it.to))
                            handleErc20(it, time, wallets, irohaAnchoredTokens[it.to]!!, true)
                        else
                            listOf()
                    }
            }, { ex ->
                logger.error("Cannot parse block", ex)
                listOf()
            }
        )
    }

    /**
     * Logger
     */
    companion object : KLogging()
}

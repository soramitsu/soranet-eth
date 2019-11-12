/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

@file:JvmName("EthDepositMain")

package com.d3.eth.deposit

import com.d3.chainadapter.client.RMQConfig
import com.d3.commons.config.loadLocalConfigs
import com.d3.commons.config.loadRawLocalConfigs
import com.d3.commons.model.IrohaCredential
import com.d3.commons.sidechain.iroha.consumer.IrohaConsumerImpl
import com.d3.commons.sidechain.iroha.util.impl.IrohaQueryHelperImpl
import com.d3.eth.provider.ETH_RELAY
import com.d3.eth.provider.ETH_WALLET
import com.d3.eth.provider.EthAddressProviderIrohaImpl
import com.d3.eth.provider.EthTokensProviderImpl
import com.d3.eth.registration.EthRegistrationConfig
import com.d3.eth.registration.wallet.EthereumWalletRegistrationHandler
import com.github.kittinunf.result.*
import integration.eth.config.EthereumPasswords
import integration.eth.config.loadEthPasswords
import jp.co.soramitsu.iroha.java.IrohaAPI
import jp.co.soramitsu.iroha.java.Utils
import mu.KLogging
import kotlin.system.exitProcess

private val logger = KLogging().logger

const val REFUND_OPERATION = "Ethereum refund"
const val ETH_DEPOSIT_SERVICE_NAME = "eth-deposit"

/**
 * Application entry point
 */
fun main() {
    loadLocalConfigs("eth-deposit", EthDepositConfig::class.java, "deposit.properties")
        .fanout { loadEthPasswords("eth-deposit", "/eth/ethereum_password.properties") }
        .map { (depositConfig, ethereumPasswords) ->
            val rmqConfig = loadRawLocalConfigs(
                "rmq",
                RMQConfig::class.java,
                "rmq.properties"
            )
            val ehRegistrtaionConfig = loadLocalConfigs(
                "eth-registration",
                EthRegistrationConfig::class.java,
                "registration.properties"
            ).get()
            executeDeposit(ethereumPasswords, depositConfig, rmqConfig, ehRegistrtaionConfig)
        }
        .failure { ex ->
            logger.error("Cannot run eth deposit", ex)
            exitProcess(1)
        }
}

fun executeDeposit(
    ethereumPasswords: EthereumPasswords,
    depositConfig: EthDepositConfig,
    rmqConfig: RMQConfig,
    registrationConfig: EthRegistrationConfig
) {
    Result.of {
        val keypair = Utils.parseHexKeypair(
            depositConfig.notaryCredential.pubkey,
            depositConfig.notaryCredential.privkey
        )
        IrohaCredential(depositConfig.notaryCredential.accountId, keypair)
    }.flatMap { irohaCredential ->
        executeDeposit(irohaCredential, ethereumPasswords, depositConfig, rmqConfig, registrationConfig)
    }.failure { ex ->
        logger.error("Cannot run eth deposit", ex)
        System.exit(1)
    }
}

/** Run deposit instance with particular [irohaCredential] */
fun executeDeposit(
    irohaCredential: IrohaCredential,
    ethereumPasswords: EthereumPasswords,
    depositConfig: EthDepositConfig,
    rmqConfig: RMQConfig,
    registrationConfig: EthRegistrationConfig
): Result<Unit, Exception> {
    logger.info { "Run ETH deposit" }

    val irohaAPI = IrohaAPI(
        depositConfig.iroha.hostname,
        depositConfig.iroha.port
    )

    val queryHelper = IrohaQueryHelperImpl(
        irohaAPI,
        irohaCredential.accountId,
        irohaCredential.keyPair
    )

    val ethWalletProvider = EthAddressProviderIrohaImpl(
        queryHelper,
        depositConfig.ethereumWalletStorageAccount,
        depositConfig.ethereumWalletSetterAccount,
        ETH_WALLET
    )
    val ethRelayProvider = EthAddressProviderIrohaImpl(
        queryHelper,
        depositConfig.ethereumRelayStorageAccount,
        depositConfig.ethereumRelaySetterAccount,
        ETH_RELAY
    )
    val ethTokensProvider = EthTokensProviderImpl(
        queryHelper,
        depositConfig.ethAnchoredTokenStorageAccount,
        depositConfig.ethAnchoredTokenSetterAccount,
        depositConfig.irohaAnchoredTokenStorageAccount,
        depositConfig.irohaAnchoredTokenSetterAccount
    )

    val registrationHandler = EthereumWalletRegistrationHandler(
        IrohaConsumerImpl(irohaCredential, irohaAPI),
        registrationConfig.registrationCredential.accountId,
        depositConfig.ethereumWalletStorageAccount,
        ethWalletProvider,
        ethRelayProvider
    )

    return EthDepositInitialization(
        irohaCredential,
        irohaAPI,
        depositConfig,
        ethereumPasswords,
        rmqConfig,
        ethWalletProvider,
        ethRelayProvider,
        ethTokensProvider,
        registrationHandler
    ).init()
}

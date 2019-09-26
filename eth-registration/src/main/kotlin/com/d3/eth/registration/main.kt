/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

@file:JvmName("EthRegistrationMain")

package com.d3.eth.registration

import com.d3.commons.config.loadLocalConfigs
import com.github.kittinunf.result.failure
import com.github.kittinunf.result.fanout
import com.github.kittinunf.result.map
import integration.eth.config.EthereumPasswords
import integration.eth.config.loadEthPasswords
import jp.co.soramitsu.iroha.java.IrohaAPI
import mu.KLogging
import kotlin.system.exitProcess

private val logger = KLogging().logger
const val REGISTRATION_OPERATION = "Ethereum user registration"
/**
 * Entry point for Registration Service
 */
fun main() {
    loadEthPasswords(
        "relay-registration",
        "/eth/ethereum_password.properties"
    ).fanout {
        loadLocalConfigs(
            "eth-registration",
            EthRegistrationConfig::class.java,
            "registration.properties"
        )
    }.map { (ethPasswordConfig, registrationConfig) ->
        executeRegistration(ethPasswordConfig, registrationConfig)
    }
}

fun executeRegistration(
    ethPasswordConfig: EthereumPasswords,
    ethRegistrationConfig: EthRegistrationConfig
) {
    logger.info("Run ETH registration service")
    val irohaNetwork =
        IrohaAPI(ethRegistrationConfig.iroha.hostname, ethRegistrationConfig.iroha.port)

    EthRegistrationServiceInitialization(
        ethPasswordConfig,
        ethRegistrationConfig,
        irohaNetwork
    ).init()
        .failure { ex ->
            logger.error("cannot run eth registration", ex)
            irohaNetwork.close()
            exitProcess(1)
        }
}

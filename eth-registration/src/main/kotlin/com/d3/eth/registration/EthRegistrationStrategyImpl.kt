/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.eth.registration

import com.d3.commons.model.D3ErrorException
import com.d3.commons.registration.RegistrationStrategy
import com.d3.commons.registration.SideChainRegistrator
import com.d3.commons.sidechain.iroha.consumer.IrohaConsumer
import com.d3.eth.provider.ETH_WALLET
import com.d3.eth.provider.EthFreeClientAddressProvider
import com.d3.eth.provider.EthAddressProvider
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.flatMap
import mu.KLogging

/**
 * Effective implementation of [RegistrationStrategy]
 */
class EthRegistrationStrategyImpl(
    private val ethFreeRelayProvider: EthFreeClientAddressProvider,
    private val ethRelayProvider: EthAddressProvider,
    private val irohaConsumer: IrohaConsumer,
    private val notaryIrohaAccount: String
) : RegistrationStrategy {

    init {
        logger.info { "Init EthRegistrationStrategyImpl with irohaCreator=${irohaConsumer.creator}, notaryIrohaAccount=$notaryIrohaAccount" }
    }

    private val ethereumAccountRegistrator =
        SideChainRegistrator(
            irohaConsumer,
            notaryIrohaAccount,
            ETH_WALLET
        )

    /**
     * Register new notary client
     * @param accountName - client name
     * @param domainId - client domain
     * @param publicKey - client public key
     * @return ethereum wallet has been registered
     */
    @Synchronized
    override fun register(
        accountName: String,
        domainId: String,
        publicKey: String
    ): Result<String, Exception> {
        // check that client hasn't been registered yet
        return ethRelayProvider.getRelayByAccountId("$accountName@$domainId")
            .flatMap { assignedRelays ->
                if (assignedRelays.isPresent)
                    throw D3ErrorException.warning(
                        failedOperation = REGISTRATION_OPERATION,
                        description = "Client $accountName@$domainId has already been registered with relay: ${assignedRelays.get()}"
                    )
                ethFreeRelayProvider.getAddress()
            }.flatMap { freeEthWallet ->
                // register with relay in Iroha
                ethereumAccountRegistrator.register(
                    freeEthWallet,
                    "$accountName@$domainId"
                ) { "$accountName@$domainId" }
            }
    }

    /**
     * Return number of free relays.
     */
    override fun getFreeAddressNumber(): Result<Int, Exception> {
        return ethFreeRelayProvider.getAddressCount()
    }

    /**
     * Logger
     */
    companion object : KLogging()
}

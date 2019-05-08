package com.d3.eth.registration

import com.d3.commons.config.EthereumPasswords
import com.d3.commons.registration.IrohaAccountRegistrator
import com.d3.commons.registration.RegistrationStrategy
import com.d3.commons.sidechain.iroha.consumer.IrohaConsumer
import com.d3.eth.provider.EthFreeRelayProvider
import com.d3.eth.provider.EthRelayProvider
import com.d3.eth.sidechain.util.DeployHelper
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.flatMap
import com.github.kittinunf.result.map
import mu.KLogging

/**
 * Effective implementation of [RegistrationStrategy]
 */
class EthRegistrationStrategyImpl(
    private val ethFreeRelayProvider: EthFreeRelayProvider,
    private val ethRelayProvider: EthRelayProvider,
    ethRegistrationConfig: EthRegistrationConfig,
    ethPasswordConfig: EthereumPasswords,
    private val irohaConsumer: IrohaConsumer,
    private val notaryIrohaAccount: String
) : RegistrationStrategy {

    init {
        logger.info { "Init EthRegistrationStrategyImpl with irohaCreator=${irohaConsumer.creator}, notaryIrohaAccount=$notaryIrohaAccount" }
    }

    private val CURRENCY_WALLET = "ethereum_wallet"

    private val ethereumAccountRegistrator =
        IrohaAccountRegistrator(
            irohaConsumer,
            notaryIrohaAccount,
            CURRENCY_WALLET
        )

    private val deployHelper = DeployHelper(ethRegistrationConfig.ethereum, ethPasswordConfig)

    /**
     * Register new notary client
     * @param accountName - client name
     * @param domainId - client domain
     * @param publicKey - client public key
     * @return ethereum wallet has been registered
     */
    override fun register(
        accountName: String,
        domainId: String,
        publicKey: String
    ): Result<String, Exception> {
        return ethFreeRelayProvider.getRelay()
            .flatMap { freeEthWallet ->
                ethRelayProvider.getRelayByAccountId("$accountName@$domainId")
                    .flatMap { assignedRelays ->
                        //TODO maybe check it before calling ethFreeRelayProvider.getRelay()?
                        // check that client hasn't been registered yet
                        if (assignedRelays.isPresent)
                            throw IllegalArgumentException("Client $accountName@$domainId has already been registered with relay: ${assignedRelays.get()}")

                        // register relay to Iroha
                        ethereumAccountRegistrator.register(
                            freeEthWallet,
                            accountName,
                            domainId,
                            publicKey
                        ) { "$accountName@$domainId" }
                    }
            }
    }

    /**
     * Return number of free relays.
     */
    override fun getFreeAddressNumber(): Result<Int, Exception> {
        return ethFreeRelayProvider.getRelays()
            .map { freeRelays ->
                freeRelays.size
            }
    }

    /**
     * Logger
     */
    companion object : KLogging()
}

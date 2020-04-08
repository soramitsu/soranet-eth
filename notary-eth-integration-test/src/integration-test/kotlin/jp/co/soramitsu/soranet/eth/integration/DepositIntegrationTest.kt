/*
 * Copyright Soramitsu Co., Ltd. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package jp.co.soramitsu.soranet.eth.integration

import com.d3.commons.registration.registrationConfig
import com.d3.commons.util.getRandomString
import com.d3.commons.util.toHexString
import integration.helper.D3_DOMAIN
import integration.helper.IrohaConfigHelper
import integration.registration.RegistrationServiceTestEnvironment
import jp.co.soramitsu.crypto.ed25519.Ed25519Sha3
import jp.co.soramitsu.soranet.eth.integration.helper.EthIntegrationHelperUtil
import jp.co.soramitsu.soranet.eth.provider.ETH_PRECISION
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.web3j.crypto.Credentials
import org.web3j.crypto.ECKeyPair
import org.web3j.crypto.Keys
import java.math.BigDecimal
import java.math.BigInteger
import java.security.KeyPair
import java.time.Duration

/**
 * Integration tests for deposit case.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DepositIntegrationTest {
    /** Utility functions for integration tests */
    private val integrationHelper = EthIntegrationHelperUtil()

    /** Ethereum assetId in Iroha */
    private val etherAssetId = "ether#ethereum"

    private val registrationTestEnvironment = RegistrationServiceTestEnvironment(integrationHelper)
    private val ethDeposit: Job

    init {
        // run notary
        ethDeposit = GlobalScope.launch {
            integrationHelper.runEthDeposit(
                ethDepositConfig = integrationHelper.configHelper.createEthDepositConfig()
            )
        }
        registrationTestEnvironment.registrationInitialization.init()
        Thread.sleep(5_000)
    }

    private val timeoutDuration = Duration.ofMinutes(IrohaConfigHelper.timeoutMinutes)

    private fun registerClient(accountName: String, keypair: KeyPair, ecKeyPair: ECKeyPair) {
        // register client in Iroha
        val res = integrationHelper.sendRegistrationRequest(
            accountName,
            keypair.public.toHexString(),
            registrationConfig.port
        )
        Assertions.assertEquals(200, res.statusCode)

        // register Ethereum wallet
        integrationHelper.registerEthereumWallet("$accountName@$D3_DOMAIN", keypair, ecKeyPair)
        Thread.sleep(3_000)
    }

    @AfterAll
    fun dropDown() {
        ethDeposit.cancel()
        integrationHelper.close()
    }

    /**
     * Test US-001 Deposit of ETH
     * Note: Ethereum and Iroha must be deployed to pass the test.
     * @given Ethereum and Iroha networks running and two ethereum wallets and "fromAddress" with at least
     * 1234000000000 Wei and notary running and user registered with ethereum wallet
     * @when user transfers 1234000000000 Wei to the master contract
     * @then Associated Iroha account balance is increased on 1234000000000 Wei
     */
    @Test
    fun depositOfETH() {
        Assertions.assertTimeoutPreemptively(timeoutDuration) {
            integrationHelper.nameCurrentThread(this::class.simpleName!!)

            val clientIrohaAccount = String.getRandomString(9)
            val irohaKeyPair = Ed25519Sha3().generateKeypair()
            val ethKeyPair = Keys.createEcKeyPair()

            registerClient(
                clientIrohaAccount,
                irohaKeyPair,
                ethKeyPair
            )

            val clientIrohaAccountId = "$clientIrohaAccount@$D3_DOMAIN"
            val ethAddress = Keys.getAddress(ethKeyPair.publicKey)

            val initialAmount = integrationHelper.getIrohaAccountBalance(clientIrohaAccountId, etherAssetId)
            val amount = BigInteger.valueOf(1_234_000_000_000)
            // send ETH
            integrationHelper.purgeAndwaitOneIrohaBlock {
                integrationHelper.sendEth(amount, ethAddress)
            }
            integrationHelper.purgeAndwaitOneIrohaBlock {
                integrationHelper.sendEth(
                    amount,
                    integrationHelper.masterContract.contractAddress,
                    Credentials.create(ethKeyPair)
                )
            }
            Thread.sleep(3_000)

            Assertions.assertEquals(
                BigDecimal(amount, ETH_PRECISION).add(BigDecimal(initialAmount)),
                BigDecimal(
                    integrationHelper.getIrohaAccountBalance(
                        clientIrohaAccountId,
                        etherAssetId
                    )
                )
            )
        }
    }

    /**
     * Test US-002 Deposit of ETH
     * Note: Ethereum and Iroha must be deployed to pass the test.
     * @given Ethereum and Iroha networks running and two ethereum wallets and "fromAddress" with at least 51 coin
     * (51 coin) and notary running
     * @when "fromAddress" transfers 51 coin to "relayWallet"
     * @then Associated Iroha account balance is increased on 51 coin
     */
    @Test
    fun depositOfERC20() {
        Assertions.assertTimeoutPreemptively(timeoutDuration) {
            Thread.currentThread().name = this::class.simpleName
            val (tokenInfo, tokenAddress) = integrationHelper.deployRandomERC20Token(2)
            val assetId = "${tokenInfo.name}#ethereum"

            val clientIrohaAccount = String.getRandomString(9)
            val irohaKeyPair = Ed25519Sha3().generateKeypair()
            val ethKeyPair = Keys.createEcKeyPair()

            registerClient(
                clientIrohaAccount,
                irohaKeyPair,
                ethKeyPair
            )

            val clientIrohaAccountId = "$clientIrohaAccount@$D3_DOMAIN"
            val ethAddress = Keys.getAddress(ethKeyPair.publicKey)
            val initialAmount = integrationHelper.getIrohaAccountBalance(clientIrohaAccountId, assetId)
            val amount = BigInteger.valueOf(51)

            // send ERC20
            integrationHelper.purgeAndwaitOneIrohaBlock {
                integrationHelper.sendERC20Token(tokenAddress, amount, ethAddress)
            }
            integrationHelper.purgeAndwaitOneIrohaBlock {
                integrationHelper.sendEth(
                    amount,
                    integrationHelper.masterContract.contractAddress,
                    Credentials.create(ethKeyPair)
                )
            }
            Thread.sleep(7_000)

            Assertions.assertEquals(
                BigDecimal(amount, tokenInfo.precision).add(BigDecimal(initialAmount)),
                BigDecimal(integrationHelper.getIrohaAccountBalance(clientIrohaAccountId, assetId))
            )
        }
    }
}

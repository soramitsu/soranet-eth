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
import jp.co.soramitsu.crypto.ed25519.Ed25519Sha3
import jp.co.soramitsu.soranet.eth.integration.helper.EthIntegrationHelperUtil
import jp.co.soramitsu.soranet.eth.provider.ETH_WALLET
import jp.co.soramitsu.soranet.eth.provider.EthAddressProviderIrohaImpl
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.fail
import org.web3j.crypto.Keys
import java.time.Duration

/**
 * Requires Iroha is running
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EthAddressProviderIrohaTest {

    val integrationHelper = EthIntegrationHelperUtil()

    /** Iroha account that holds details */
    private val relayStorage = integrationHelper.accountHelper.notaryAccount.accountId

    /** Iroha account that has set details */
    private val relaySetter = integrationHelper.accountHelper.registrationAccount.accountId

    private val timeoutDuration = Duration.ofMinutes(IrohaConfigHelper.timeoutMinutes)

    @AfterAll
    fun dropDown() {
        integrationHelper.close()
    }

    /**
     * @given ethereum relay wallets are stored in the system
     * @when getAddresses() is called
     * @then not free wallets are returned in a map
     */
    @Test
    fun testStorage() {
        assertTimeoutPreemptively(timeoutDuration) {
            integrationHelper.nameCurrentThread(this::class.simpleName!!)

            val clientIrohaAccount = String.getRandomString(9)
            val irohaKeyPair = Ed25519Sha3().generateKeypair()
            val ethKeyPair = Keys.createEcKeyPair()
            val res = integrationHelper.sendRegistrationRequest(
                clientIrohaAccount,
                irohaKeyPair.public.toHexString(),
                registrationConfig.port
            )
            assertEquals(200, res.statusCode)
            val clientId = "$clientIrohaAccount@$D3_DOMAIN"
            integrationHelper.registerEthereumWallet(
                clientId,
                irohaKeyPair,
                ethKeyPair
            )

            EthAddressProviderIrohaImpl(
                integrationHelper.queryHelper,
                relayStorage,
                relaySetter,
                ETH_WALLET
            ).getAddresses()
                .fold(
                    { assertTrue(it.entries.any { it.key == Keys.getAddress(ethKeyPair) && it.value == clientId }) },
                    { ex -> fail("cannot get addresses", ex) }
                )
        }
    }

    /**
     * @given There is no relay accounts registered (we use test accountId as relay holder)
     * @when getAddresses() is called
     * @then empty map is returned
     */
    @Test
    fun testEmptyStorage() {
        assertTimeoutPreemptively(timeoutDuration) {
            integrationHelper.nameCurrentThread(this::class.simpleName!!)
            EthAddressProviderIrohaImpl(
                integrationHelper.queryHelper,
                integrationHelper.testCredential.accountId,
                relaySetter,
                ETH_WALLET
            ).getAddresses()
                .fold(
                    { assert(it.isEmpty()) },
                    { ex -> fail("result has exception", ex) }
                )
        }
    }

    @Test
    fun testGetByAccountNotFound() {
        val res = EthAddressProviderIrohaImpl(
            integrationHelper.queryHelper,
            integrationHelper.testCredential.accountId,
            relaySetter,
            ETH_WALLET
        ).getAddressByAccountId("nonexist@domain")
        assertFalse(res.get().isPresent)
    }
}

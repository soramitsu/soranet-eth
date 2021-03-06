/*
 * Copyright Soramitsu Co., Ltd. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package jp.co.soramitsu.soranet.eth.integration.tests

import com.d3.commons.util.getRandomString
import com.github.kittinunf.result.success
import integration.helper.IrohaConfigHelper
import jp.co.soramitsu.soranet.eth.integration.helper.EthIntegrationHelperUtil
import jp.co.soramitsu.soranet.eth.integration.helper.EthIntegrationTestEnvironment
import jp.co.soramitsu.soranet.eth.provider.ETH_ADDRESS
import jp.co.soramitsu.soranet.eth.provider.ETH_DOMAIN
import jp.co.soramitsu.soranet.eth.provider.ETH_NAME
import jp.co.soramitsu.soranet.eth.provider.ETH_PRECISION
import jp.co.soramitsu.soranet.eth.token.EthTokenInfo
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.fail
import java.time.Duration

/**
 * Test Iroha Ethereum ERC20 tokens provider
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EthTokensProviderTest {
    private val ethIntegrationTestEnvironment = EthIntegrationTestEnvironment

    private val integrationHelper = ethIntegrationTestEnvironment.integrationHelper

    private val timeoutDuration = Duration.ofMinutes(IrohaConfigHelper.timeoutMinutes)

    init {
        ethIntegrationTestEnvironment.init()
    }

    @AfterAll
    fun dropDown() {
        ethIntegrationTestEnvironment.close()
    }

    /**
     * Test US-001 Listing all the tokens from tokensProvider
     * Note: Iroha must be deployed to pass the test.
     * @given Iroha network is running and master account added few tokens
     * @when tokensProvider lists all the tokens
     * @then returned list must contain all previously added tokens
     */
    @Test
    fun testGetTokens() {
        assertTimeoutPreemptively(timeoutDuration) {
            integrationHelper.nameCurrentThread(this::class.simpleName!!)
            val tokensToAdd = 3
            val expectedTokens = mutableMapOf<String, EthTokenInfo>()
            (1..tokensToAdd).forEach { precision ->
                val ethWallet = "0x$precision"
                val tokenInfo = EthTokenInfo(String.getRandomString(9), ETH_DOMAIN, precision)
                expectedTokens[ethWallet] = tokenInfo
                integrationHelper.addEthAnchoredERC20Token(ethWallet, tokenInfo)
            }
            val ethTokensProvider = integrationHelper.ethTokensProvider()
            ethTokensProvider.getEthAnchoredTokens()
                .fold(
                    { tokens ->
                        assertFalse(tokens.isEmpty())
                        expectedTokens.forEach { (expectedEthWallet, expectedTokenInfo) ->
                            val expectedName = expectedTokenInfo.name
                            val expectedDomain = expectedTokenInfo.domain
                            val expectedPrecision = expectedTokenInfo.precision
                            val assetId = "$expectedName#$expectedDomain"
                            assertEquals(
                                "$expectedName#$expectedDomain",
                                tokens.get(expectedEthWallet)
                            )
                            assertEquals(
                                expectedPrecision,
                                ethTokensProvider.getTokenPrecision(assetId).get()
                            )
                            assertEquals(
                                expectedEthWallet,
                                ethTokensProvider.getTokenAddress(assetId).get()
                            )
                        }
                    },
                    { ex -> fail("Cannot get tokens", ex) })
        }
    }

    /**
     * @given Iroha network is running
     * @when tokenProvider is queried with some nonexistent asset
     * @then failure result is returned
     */
    @Test
    fun getNonexistentToken() {
        val nonexistAssetId = "nonexist#token"
        assertTimeoutPreemptively(timeoutDuration) {
            integrationHelper.nameCurrentThread(this::class.simpleName!!)
            val ethTokensProvider = integrationHelper.ethTokensProvider()
            ethTokensProvider.getTokenPrecision(nonexistAssetId)
                .fold(
                    { fail("Result returned success while failure is expected.") },
                    { Unit }
                )

            integrationHelper.ethTokensProvider().getTokenAddress(nonexistAssetId)
                .fold(
                    { fail("Result returned success while failure is expected.") },
                    { assertEquals("Token $nonexistAssetId not found", it.message) }
                )
        }
    }

    /**
     * Test predefined asset ethereum.
     * @given Iroha network is running
     * @when tokenProvider is queried with "ether"
     * @then predefined parameters are returned
     */
    @Test
    fun getEthereum() {
        assertTimeoutPreemptively(timeoutDuration) {
            integrationHelper.nameCurrentThread(this::class.simpleName!!)
            val ethTokensProvider = integrationHelper.ethTokensProvider()
            ethTokensProvider.getTokenPrecision("$ETH_NAME#$ETH_DOMAIN")
                .success { assertEquals(ETH_PRECISION, it) }

            ethTokensProvider.getTokenAddress("$ETH_NAME#$ETH_DOMAIN")
                .success { assertEquals(ETH_ADDRESS, it) }
        }
    }
}

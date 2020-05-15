/*
 * Copyright Soramitsu Co., Ltd. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package jp.co.soramitsu.soranet.eth.integration.tests

import jp.co.soramitsu.soranet.eth.bridge.XOR_LIMITS_TIME_KEY
import jp.co.soramitsu.soranet.eth.bridge.XOR_LIMITS_VALUE_KEY
import jp.co.soramitsu.soranet.eth.integration.helper.EthIntegrationTestEnvironment
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigInteger
import kotlin.test.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class XorWithdrawalLimitsIntegrationTest {
    private val ethIntegrationTestEnvironment = EthIntegrationTestEnvironment
    private val integrationHelper = ethIntegrationTestEnvironment.integrationHelper

    @BeforeAll
    fun init() {
        ethIntegrationTestEnvironment.init()
    }

    @AfterAll
    fun dropDown() {
        ethIntegrationTestEnvironment.close()
    }

    /**
     * @given notary service is running and no data for withdrawal limits assigned
     * @when an ethereum block appears
     * @then new xor limit and its time saved to Iroha
     */
    @Test
    fun correctXorWithdrawalLimitsUpdate() {
        // generate an eth block
        integrationHelper.sendEth(
            BigInteger("200000000000"),
            integrationHelper.contractTestHelper.xorAddress
        )
        Thread.sleep(5000)
        assertTrue(
            integrationHelper.queryHelper.getAccountDetails(
                integrationHelper.accountHelper.xorLimitsStorageAccount.accountId,
                integrationHelper.accountHelper.withdrawalAccount.accountId,
                XOR_LIMITS_VALUE_KEY
            ).get().isPresent
        )
        val toLong = integrationHelper.queryHelper.getAccountDetails(
            integrationHelper.accountHelper.xorLimitsStorageAccount.accountId,
            integrationHelper.accountHelper.withdrawalAccount.accountId,
            XOR_LIMITS_TIME_KEY
        ).get().get().toLong()
        assertTrue(toLong > 0)
        assertTrue(toLong < System.currentTimeMillis())
    }
}

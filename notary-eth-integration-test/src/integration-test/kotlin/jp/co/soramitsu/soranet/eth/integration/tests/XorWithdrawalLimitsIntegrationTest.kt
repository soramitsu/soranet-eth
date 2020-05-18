/*
 * Copyright Soramitsu Co., Ltd. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package jp.co.soramitsu.soranet.eth.integration.tests

import com.d3.commons.sidechain.iroha.consumer.IrohaConsumerImpl
import com.d3.commons.sidechain.iroha.util.ModelUtil
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
        // generate xor supply to the contract address
        integrationHelper.sendEth(
            BigInteger("20000"),
            integrationHelper.masterContract.contractAddress
        )
        ModelUtil.setAccountDetail(
            IrohaConsumerImpl(integrationHelper.accountHelper.withdrawalAccount, integrationHelper.irohaAPI),
            integrationHelper.accountHelper.xorLimitsStorageAccount.accountId,
            XOR_LIMITS_TIME_KEY,
            (System.currentTimeMillis() - 10000).toString()
        )
        Thread.sleep(5000)
        integrationHelper.waitOneEtherBlock {
            integrationHelper.deployRandomERC20Token()
        }
        Thread.sleep(3000)
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

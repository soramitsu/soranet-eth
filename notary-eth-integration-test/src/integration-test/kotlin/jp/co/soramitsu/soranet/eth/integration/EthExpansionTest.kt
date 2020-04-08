/*
 * Copyright Soramitsu Co., Ltd. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package jp.co.soramitsu.soranet.eth.integration

import com.d3.commons.util.getRandomString
import com.d3.commons.util.toHexString
import jp.co.soramitsu.crypto.ed25519.Ed25519Sha3
import jp.co.soramitsu.soranet.eth.integration.helper.EthIntegrationHelperUtil
import jp.co.soramitsu.soranet.eth.sidechain.util.DeployHelper
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EthExpansionTest {
    private val integrationHelper = EthIntegrationHelperUtil()
    private val depositService: Job

    init {
        depositService = GlobalScope.launch {
            integrationHelper.runEthDeposit()
        }
        Thread.sleep(10_000)
    }

    @AfterAll
    fun dropDown() {
        depositService.cancel()
        integrationHelper.close()
    }

    /**
     * @given deposit and withdrawal services are running
     * @when expansion transaction is triggered
     * @then
     */
    @Test
    fun expansionTest() {
        val publicKey = Ed25519Sha3().generateKeypair().public.toHexString()
        val ethAddress = "0x0000000000000000000000000000000000000000"
        val notaryName = "notary_name_" + String.getRandomString(5)
        val notaryEndpointAddress = "http://localhost:20001"

        val masterContract = DeployHelper(
            integrationHelper.configHelper.createEthereumConfig(),
            integrationHelper.configHelper.ethPasswordConfig
        ).loadMasterContract(integrationHelper.masterContract.contractAddress)
        assertFalse { masterContract.isPeer(ethAddress).send() }

        val hash = integrationHelper.triggerExpansion(
            integrationHelper.accountHelper.notaryAccount.accountId,
            publicKey,
            2,
            ethAddress,
            notaryName,
            notaryEndpointAddress
        )

        Thread.sleep(5_000)

        assertTrue { masterContract.isPeer(ethAddress).send() }
        assertEquals(notaryEndpointAddress, integrationHelper.getNotaries()[notaryName])
        assertTrue {
            integrationHelper.getSignatories(integrationHelper.accountHelper.notaryAccount.accountId)
                .map { it.toLowerCase() }.contains(publicKey.toLowerCase())
        }

    }
}

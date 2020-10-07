/*
 * Copyright Soramitsu Co., Ltd. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package jp.co.soramitsu.soranet.eth.integration.tests

import com.d3.commons.sidechain.iroha.util.ModelUtil
import com.d3.commons.util.GsonInstance
import com.d3.commons.util.getRandomId
import com.d3.commons.util.irohaUnEscape
import jp.co.soramitsu.soranet.eth.bridge.ReferendumHashProof
import jp.co.soramitsu.soranet.eth.bridge.ReferendumHashProofHandler.Companion.PROOF_KEY
import jp.co.soramitsu.soranet.eth.bridge.SORA_PROOF_DOMAIN
import jp.co.soramitsu.soranet.eth.integration.helper.EthIntegrationTestEnvironment
import org.junit.jupiter.api.*
import org.web3j.crypto.Hash
import org.web3j.crypto.WalletUtils
import java.math.BigInteger
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Integration tests for referendum proof signing.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ReferendumProofIntegrationTest {
    private val ethIntegrationTestEnvironment = EthIntegrationTestEnvironment

    /** Integration tests util */
    private val integrationHelper = ethIntegrationTestEnvironment.integrationHelper

    /** Notary account in Iroha */
    private val withdrawalAccountId =
        ethIntegrationTestEnvironment.ethDepositConfig.withdrawalCredential.accountId

    private val gson = GsonInstance.get()

    private lateinit var accountId: String

    @BeforeAll
    fun setup() {
        ethIntegrationTestEnvironment.init()
        integrationHelper.nameCurrentThread(this::class.simpleName!!)
    }

    @BeforeEach
    fun registerReferendumAccount() {
        val randomId = String.getRandomId()
        ModelUtil.createAccount(
            integrationHelper.irohaConsumer,
            randomId,
            SORA_PROOF_DOMAIN.replace("@", ""),
            ModelUtil.generateKeypair().public,
            emptyList()
        )

        accountId = "$randomId$SORA_PROOF_DOMAIN"
    }

    @AfterAll
    fun dropDown() {
        ethIntegrationTestEnvironment.close()
    }

    /**
     * @given all services running and there is a referendum account
     * @when new proof is set properly
     * @then corresponding notary proofs appear at the account
     */
    @Test
    fun testSunnyDayHashProof() {
        //send some hash for a referendum
        val hash = Hash.sha3(String.format("%064x", BigInteger.valueOf(1234567891012302385)))
        ModelUtil.setAccountDetail(
            integrationHelper.proofSetterIrohaConsumer,
            accountId,
            PROOF_KEY,
            hash
        )

        Thread.sleep(5_000)

        // gather proof
        val proofsMap = integrationHelper.queryHelper.getAccountDetails(
            accountId,
            withdrawalAccountId
        ).get()

        assertEquals(
            1,
            proofsMap.entries.size
        )
        proofsMap.entries.forEach {
            assertTrue(
                WalletUtils.isValidAddress(it.key)
            )
            val proof = gson.fromJson(it.value.irohaUnEscape(), ReferendumHashProof::class.java)
            assertEquals(
                hash,
                proof.hash
            )
            assertNotNull(
                proof.signature
            )
        }
    }
}

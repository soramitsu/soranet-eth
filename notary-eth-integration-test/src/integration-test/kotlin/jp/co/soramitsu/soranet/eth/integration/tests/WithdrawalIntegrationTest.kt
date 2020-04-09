/*
 * Copyright Soramitsu Co., Ltd. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package jp.co.soramitsu.soranet.eth.integration.tests

import com.d3.commons.sidechain.iroha.util.ModelUtil
import com.d3.commons.util.getRandomString
import com.d3.commons.util.toHexString
import com.squareup.moshi.Moshi
import integration.helper.D3_DOMAIN
import integration.helper.IrohaConfigHelper
import integration.registration.RegistrationServiceTestEnvironment
import jp.co.soramitsu.soranet.eth.bridge.endpoint.BigIntegerMoshiAdapter
import jp.co.soramitsu.soranet.eth.bridge.endpoint.EthNotaryResponse
import jp.co.soramitsu.soranet.eth.bridge.endpoint.EthNotaryResponseMoshiAdapter
import jp.co.soramitsu.soranet.eth.integration.helper.EthIntegrationHelperUtil
import jp.co.soramitsu.soranet.eth.provider.ETH_PRECISION
import jp.co.soramitsu.soranet.eth.provider.ETH_WALLET
import jp.co.soramitsu.soranet.eth.provider.EthAddressProviderIrohaImpl
import jp.co.soramitsu.soranet.eth.sidechain.util.DeployHelper
import jp.co.soramitsu.soranet.eth.sidechain.util.ENDPOINT_ETHEREUM
import jp.co.soramitsu.soranet.eth.sidechain.util.hashToWithdraw
import jp.co.soramitsu.soranet.eth.sidechain.util.signUserData
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTimeoutPreemptively
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.web3j.crypto.Keys
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Duration

/**
 * Class for Ethereum sidechain infrastructure deployment and communication.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class WithdrawalIntegrationTest {

    /** Integration tests util */
    private val integrationHelper = EthIntegrationHelperUtil

    /** Test Deposit configuration */
    private val depositConfig = integrationHelper.configHelper.createEthDepositConfig()

    private val timeoutDuration = Duration.ofMinutes(IrohaConfigHelper.timeoutMinutes)

    private val registrationTestEnvironment = RegistrationServiceTestEnvironment(integrationHelper)
    private val ethDeposit: Job

    init {
        ethDeposit = GlobalScope.launch {
            integrationHelper.runEthDeposit(ethDepositConfig = depositConfig)
        }
        registrationTestEnvironment.registrationInitialization.init()
    }

    /** Ethereum private key **/
    private val keypair = DeployHelper(
        depositConfig.ethereum,
        integrationHelper.configHelper.ethPasswordConfig
    ).credentials.ecKeyPair

    @AfterAll
    fun dropDown() {
        ethDeposit.cancel()
    }

    /**
     * Test US-003 Withdrawal of ETH token
     * Note: Iroha must be deployed to pass the test.
     * @given Iroha networks running and user has sent 64203 Ether to master
     * @when withdrawal service queries notary
     * @then notary replies with refund information and signature
     */
    @Test
    fun testRefund() {
        assertTimeoutPreemptively(timeoutDuration) {
            integrationHelper.nameCurrentThread(this::class.simpleName!!)
            val withdrawalAccountId = depositConfig.notaryCredential.accountId
            val amount = "64203"
            val decimalAmount = BigDecimal(amount).scaleByPowerOfTen(ETH_PRECISION)
            val assetId = "ether#ethereum"
            val ethWallet = "0x1334"
            val ethKeyPair = Keys.createEcKeyPair()

            // create
            val client = String.getRandomString(9)
            // register client in Iroha
            var res = registrationTestEnvironment.register(
                client,
                ModelUtil.generateKeypair().public.toHexString()
            )
            assertEquals(200, res.statusCode)
            val clientId = "$client@$D3_DOMAIN"
            integrationHelper.registerEthereumWallet(
                clientId,
                integrationHelper.testCredential.keyPair,
                ethKeyPair
            )

            integrationHelper.addIrohaAssetTo(clientId, assetId, decimalAmount)

            Thread.sleep(5_000)
            val relay = EthAddressProviderIrohaImpl(
                integrationHelper.queryHelper,
                integrationHelper.accountHelper.ethereumWalletStorageAccount.accountId,
                integrationHelper.accountHelper.notaryAccount.accountId,
                ETH_WALLET
            ).getAddresses().get().filter {
                it.value == clientId
            }.keys.first()

            // transfer assets from user to withdrawal account
            val hash = integrationHelper.transferAssetIrohaFromClient(
                clientId,
                integrationHelper.testCredential.keyPair,
                clientId,
                withdrawalAccountId,
                assetId,
                ethWallet,
                amount
            )

            // query
            res = khttp.get("http://127.0.0.1:${depositConfig.refund.port}/$ENDPOINT_ETHEREUM/$hash")

            val moshi = Moshi
                .Builder()
                .add(EthNotaryResponseMoshiAdapter())
                .add(BigInteger::class.java, BigIntegerMoshiAdapter())
                .build()!!
            val ethNotaryAdapter = moshi.adapter(EthNotaryResponse::class.java)!!
            val response = ethNotaryAdapter.fromJson(res.jsonObject.toString())

            assert(response is EthNotaryResponse.Successful)
            response as EthNotaryResponse.Successful

            assertEquals(
                signUserData(
                    keypair,
                    hashToWithdraw(
                        "0x0000000000000000000000000000000000000000",
                        decimalAmount.toPlainString(),
                        ethWallet,
                        hash,
                        relay
                    )
                ), response.ethSignature
            )
        }
    }
}

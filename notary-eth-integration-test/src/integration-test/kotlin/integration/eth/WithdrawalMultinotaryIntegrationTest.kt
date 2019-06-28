/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package integration.eth

import com.d3.commons.config.loadEthPasswords
import com.d3.commons.config.loadLocalConfigs
import com.d3.commons.sidechain.iroha.CLIENT_DOMAIN
import com.d3.commons.sidechain.iroha.util.ModelUtil
import com.d3.commons.util.getRandomString
import com.d3.commons.util.toHexString
import com.d3.eth.deposit.EthDepositConfig
import com.d3.eth.deposit.endpoint.BigIntegerMoshiAdapter
import com.d3.eth.deposit.endpoint.EthNotaryResponse
import com.d3.eth.deposit.endpoint.EthNotaryResponseMoshiAdapter
import com.d3.eth.provider.ETH_PRECISION
import com.d3.eth.provider.EthRelayProviderIrohaImpl
import com.d3.eth.sidechain.util.DeployHelper
import com.d3.eth.sidechain.util.ENDPOINT_ETHEREUM
import com.d3.eth.sidechain.util.hashToWithdraw
import com.d3.eth.sidechain.util.signUserData
import com.squareup.moshi.Moshi
import integration.helper.EthIntegrationHelperUtil
import integration.helper.IrohaConfigHelper
import integration.registration.RegistrationServiceTestEnvironment
import khttp.get
import kotlinx.coroutines.*
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.web3j.crypto.ECKeyPair
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Duration
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class WithdrawalMultinotaryIntegrationTest {
    /** Utility functions for integration tests */
    private val integrationHelper = EthIntegrationHelperUtil()

    private val keyPair2 = ModelUtil.generateKeypair()

    private val depositConfig1: EthDepositConfig

    private val depositConfig2: EthDepositConfig

    private val keypair1: ECKeyPair

    private val ethKeyPair2: ECKeyPair

    private val ethereumPasswords =
        loadEthPasswords("eth-deposit", "/eth/ethereum_password.properties").get()

    private val timeoutDuration = Duration.ofMinutes(IrohaConfigHelper.timeoutMinutes)

    private val registrationTestEnvironment = RegistrationServiceTestEnvironment(integrationHelper)
    private val ethRegistrationService: Job
    val context: CoroutineContext = EmptyCoroutineContext

    fun CoroutineScope.runDeposit(depositConfig: EthDepositConfig) {
        launch {
            integrationHelper.runEthDeposit(ethDepositConfig = depositConfig)
        }
    }

    init {
        val notaryConfig = loadLocalConfigs(
            "eth-deposit",
            EthDepositConfig::class.java,
            "deposit.properties"
        ).get()
        val ethKeyPath = notaryConfig.ethereum.credentialsPath

        // create 1st deposit config
        val ethereumConfig1 = integrationHelper.configHelper.createEthereumConfig(ethKeyPath)

        keypair1 = DeployHelper(ethereumConfig1, ethereumPasswords).credentials.ecKeyPair

        // run 1st instance of deposit
        depositConfig1 =
            integrationHelper.configHelper.createEthDepositConfig(ethereumConfig = ethereumConfig1)
        CoroutineScope(context).runDeposit(depositConfig1)

        // create 2nd deposit config
        val ethereumConfig2 =
            integrationHelper.configHelper.createEthereumConfig(ethKeyPath.split(".key").first() + "2.key")
        depositConfig2 =
            integrationHelper.configHelper.createEthDepositConfig(ethereumConfig = ethereumConfig2)

        val notary2IrohaPublicKey = keyPair2.public.toHexString()
        val notary2EthereumCredentials =
            DeployHelper(ethereumConfig2, ethereumPasswords).credentials
        val notary2EthereumAddress = notary2EthereumCredentials.address
        ethKeyPair2 = notary2EthereumCredentials.ecKeyPair
        val notary2Name = "notary_name_" + String.getRandomString(5)
        val notary2EndpointAddress = "http://127.0.0.1:${depositConfig2.refund.port}"

        integrationHelper.triggerExpansion(
            integrationHelper.accountHelper.notaryAccount.accountId,
            notary2IrohaPublicKey,
            2,
            notary2EthereumAddress,
            notary2Name,
            notary2EndpointAddress
        )

        Thread.sleep(5_000)

        // run 2nd instance of deposit
        CoroutineScope(context).runDeposit(depositConfig2)

        // run registration
        registrationTestEnvironment.registrationInitialization.init()
        ethRegistrationService = GlobalScope.launch {
            integrationHelper.runEthRegistrationService(integrationHelper.ethRegistrationConfig)
        }
    }

    @AfterAll
    fun dropDown() {
        context.cancel()
        ethRegistrationService.cancel()
        integrationHelper.close()
    }

    /**
     * Test US-003 Withdrawal of ETH token
     * Note: Iroha must be deployed to pass the test.
     * @given Iroha is running, 2 notaries are running and user has sent 64203 Ether to master
     * @when withdrawal service queries notary1 and notary2
     * @then both notaries reply with valid refund information and signature
     */
    @Test
    fun testRefundEndpoints() {
        Assertions.assertTimeoutPreemptively(timeoutDuration) {
            integrationHelper.nameCurrentThread(this::class.simpleName!!)
            val masterAccount = integrationHelper.accountHelper.notaryAccount.accountId
            val amount = "64203"
            val decimalAmount = BigDecimal(amount).scaleByPowerOfTen(ETH_PRECISION)
            val assetId = "ether#ethereum"
            val ethWallet = "0x1334"

            // create
            val client = String.getRandomString(9)
            // register client in Iroha
            val res = integrationHelper.sendRegistrationRequest(
                client,
                ModelUtil.generateKeypair().public.toHexString(),
                registrationTestEnvironment.registrationConfig.port
            )
            Assertions.assertEquals(200, res.statusCode)
            val clientId = "$client@$CLIENT_DOMAIN"
            integrationHelper.registerClientInEth(
                client,
                integrationHelper.testCredential.keyPair
            )
            integrationHelper.addIrohaAssetTo(clientId, assetId, decimalAmount)
            val relay = EthRelayProviderIrohaImpl(
                integrationHelper.queryHelper,
                masterAccount,
                integrationHelper.accountHelper.registrationAccount.accountId
            ).getRelays().get().filter {
                it.value == clientId
            }.keys.first()

            // transfer assets from user to notary master account
            val hash = integrationHelper.transferAssetIrohaFromClient(
                clientId,
                integrationHelper.testCredential.keyPair,
                clientId,
                masterAccount,
                assetId,
                ethWallet,
                amount
            )

            // query 1
            val res1 =
                get("http://127.0.0.1:${depositConfig1.refund.port}/$ENDPOINT_ETHEREUM/$hash")

            val moshi = Moshi
                .Builder()
                .add(EthNotaryResponseMoshiAdapter())
                .add(BigInteger::class.java, BigIntegerMoshiAdapter())
                .build()!!
            val ethNotaryAdapter = moshi.adapter(EthNotaryResponse::class.java)!!
            val response1 = ethNotaryAdapter.fromJson(res1.jsonObject.toString())

            assert(response1 is EthNotaryResponse.Successful)
            response1 as EthNotaryResponse.Successful

            Assertions.assertEquals(
                signUserData(
                    keypair1,
                    hashToWithdraw(
                        "0x0000000000000000000000000000000000000000",
                        decimalAmount.toPlainString(),
                        ethWallet,
                        hash,
                        relay
                    )
                ), response1.ethSignature
            )

            // query 2
            val res2 =
                get("http://127.0.0.1:${depositConfig2.refund.port}/$ENDPOINT_ETHEREUM/$hash")

            val response2 = ethNotaryAdapter.fromJson(res2.jsonObject.toString())

            assert(response2 is EthNotaryResponse.Successful)
            response2 as EthNotaryResponse.Successful

            Assertions.assertEquals(
                signUserData(
                    ethKeyPair2,
                    hashToWithdraw(
                        "0x0000000000000000000000000000000000000000",
                        decimalAmount.toPlainString(),
                        ethWallet,
                        hash,
                        relay
                    )
                ), response2.ethSignature
            )
        }
    }
}

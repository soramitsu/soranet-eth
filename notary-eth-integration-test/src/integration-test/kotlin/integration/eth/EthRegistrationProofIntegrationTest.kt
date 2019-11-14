/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package integration.eth

import com.d3.commons.model.IrohaCredential
import com.d3.commons.sidechain.iroha.consumer.IrohaConsumerImpl
import com.d3.commons.sidechain.iroha.util.ModelUtil
import com.d3.commons.util.getRandomString
import com.d3.commons.util.toHexString
import com.d3.commons.util.irohaEscape
import com.d3.eth.deposit.endpoint.EthSignature
import com.d3.eth.provider.ETH_WALLET
import com.d3.eth.registration.wallet.ETH_FAILED_REGISTRATION_KEY
import com.d3.eth.registration.wallet.ETH_REGISTRATION_KEY
import com.d3.eth.registration.wallet.EthereumRegistrationProof
import com.d3.eth.registration.wallet.createRegistrationProof
import com.d3.eth.sidechain.util.DeployHelper
import com.d3.eth.sidechain.util.extractVRS
import integration.helper.ContractTestHelper
import integration.helper.EthIntegrationHelperUtil
import integration.helper.IrohaConfigHelper
import integration.registration.RegistrationServiceTestEnvironment
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.jupiter.api.*
import org.web3j.crypto.Keys
import org.web3j.utils.Numeric
import java.math.BigInteger
import java.time.Duration
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EthRegistrationProofIntegrationTest {
    private lateinit var cth: ContractTestHelper
    private val integrationHelper = EthIntegrationHelperUtil()
    private val registrationTestEnvironment = RegistrationServiceTestEnvironment(integrationHelper)
    private val ethDeposit: Job
    private val depositConfig = integrationHelper.configHelper.createEthDepositConfig()
    private val deployHelper =
        DeployHelper(depositConfig.ethereum, integrationHelper.configHelper.ethPasswordConfig)

    init {
        // run notary
        ethDeposit = GlobalScope.launch {
            integrationHelper.runEthDeposit(ethDepositConfig = depositConfig)
        }
        registrationTestEnvironment.registrationInitialization.init()
        Thread.sleep(10_000)
    }

    private val timeoutDuration = Duration.ofMinutes(IrohaConfigHelper.timeoutMinutes)

    @BeforeEach
    fun setup() {
        cth = ContractTestHelper()
    }

    @AfterAll
    fun dropDown() {
        ethDeposit.cancel()
        integrationHelper.close()
    }

    /**
     * Check whether ethereum wallet was registered for client
     * @param clientId - iroha account id
     * @param ethereumAddress - ethereum address as "0x123..."
     */
    private fun checkRegisteredEthereumAddress(clientId: String, ethereumAddress: String) {
        val registeredAddress = integrationHelper.queryHelper.getAccountDetails(
            clientId,
            integrationHelper.accountHelper.notaryAccount.accountId,
            ETH_WALLET
        ).get().get()
        assertEquals(ethereumAddress, registeredAddress)

        val registeredAccount = integrationHelper.queryHelper.getAccountDetails(
            integrationHelper.accountHelper.ethereumWalletStorageAccount.accountId,
            integrationHelper.accountHelper.notaryAccount.accountId,
            ethereumAddress
        ).get().get()
        assertEquals(clientId, registeredAccount)
    }

    /**
     * @given notary service is running and client with iroha account and ethereum wallet
     * @when the client sends ethereum wallet registration request
     * @then ethereum address is registered for client
     */
    @Test
    fun correctWalletRegistration() {
        Assertions.assertTimeoutPreemptively(timeoutDuration) {
            val keyPair = ModelUtil.generateKeypair()
            val clientIrohaAccount = String.getRandomString(5)
            val ethKeypair = Keys.createEcKeyPair()
            val clientId = "$clientIrohaAccount@d3"
            val ethereumAddress = "0x${Keys.getAddress(ethKeypair.publicKey)}"

            // register iroha account
            val res = integrationHelper.sendRegistrationRequest(
                clientIrohaAccount,
                keyPair.public.toHexString(),
                registrationTestEnvironment.registrationConfig.port
            )
            assertEquals(200, res.statusCode)

            // register in Ethereum
            integrationHelper.registerEthereumWallet(clientId, keyPair, ethKeypair)

            Thread.sleep(3_000)
            checkRegisteredEthereumAddress(clientId, ethereumAddress)
        }
    }

    /**
     * @given notary service is running and client with iroha account and ethereum wallet
     * @when the client sends ethereum wallet registration request with wrong proof
     * @then failed registration record is sent to iroha,
     */
    @Test
    fun correctAfterIncorrectWalletRegistration() {
        Assertions.assertTimeoutPreemptively(timeoutDuration) {
            val keyPair = ModelUtil.generateKeypair()
            val clientIrohaAccount = String.getRandomString(5)
            val ethKeypair = Keys.createEcKeyPair()
            val clientId = "$clientIrohaAccount@d3"
            val ethereumAddress = "0x${Keys.getAddress(ethKeypair.publicKey)}"

            // register iroha account
            val res = integrationHelper.sendRegistrationRequest(
                clientIrohaAccount,
                keyPair.public.toHexString(),
                registrationTestEnvironment.registrationConfig.port
            )
            assertEquals(200, res.statusCode)

            // send wrong signature
            val clientIrohaConsumer = IrohaConsumerImpl(
                IrohaCredential(clientId, keyPair),
                integrationHelper.irohaAPI
            )
            val signature = createRegistrationProof(ethKeypair)
            val wrongSignature = EthereumRegistrationProof(signature.signature, BigInteger.ZERO)
            integrationHelper.setAccountDetail(
                clientIrohaConsumer,
                integrationHelper.accountHelper.registrationAccount.accountId,
                ETH_REGISTRATION_KEY,
                wrongSignature.toJson().irohaEscape(),
                quorum = 2
            )
            Thread.sleep(3_000)

            // check wrong registration
            assertTrue {
                integrationHelper.queryHelper.getAccountDetails(
                    clientId,
                    integrationHelper.accountHelper.notaryAccount.accountId,
                    ETH_FAILED_REGISTRATION_KEY
                ).get().isPresent
            }

            // register in Ethereum again
            integrationHelper.registerEthereumWallet(clientId, keyPair, ethKeypair)

            Thread.sleep(3_000)
            checkRegisteredEthereumAddress(clientId, ethereumAddress)
        }
    }

    /**
     * @given notary service is running and client with iroha account and ethereum wallet
     * @when the client sends ethereum wallet registration request with invalid message
     * @then failed registration record is sent to iroha,
     */
    @Test
    fun correctAfterInvalidWalletRegistration() {
        Assertions.assertTimeoutPreemptively(timeoutDuration) {
            val keyPair = ModelUtil.generateKeypair()
            val clientIrohaAccount = String.getRandomString(5)
            val ethKeypair = Keys.createEcKeyPair()
            val clientId = "$clientIrohaAccount@d3"
            val ethereumAddress = "0x${Keys.getAddress(ethKeypair.publicKey)}"

            // register iroha account
            val res = integrationHelper.sendRegistrationRequest(
                clientIrohaAccount,
                keyPair.public.toHexString(),
                registrationTestEnvironment.registrationConfig.port
            )
            assertEquals(200, res.statusCode)

            // send wrong signature
            val clientIrohaConsumer = IrohaConsumerImpl(
                IrohaCredential(clientId, keyPair),
                integrationHelper.irohaAPI
            )
            integrationHelper.setAccountDetail(
                clientIrohaConsumer,
                integrationHelper.accountHelper.registrationAccount.accountId,
                ETH_REGISTRATION_KEY,
                "some wrong value",
                quorum = 2
            )
            Thread.sleep(3_000)

            // check wrong registration
            assertTrue {
                integrationHelper.queryHelper.getAccountDetails(
                    clientId,
                    integrationHelper.accountHelper.notaryAccount.accountId,
                    ETH_FAILED_REGISTRATION_KEY
                ).get().isPresent
            }

            // register in Ethereum again
            integrationHelper.registerEthereumWallet(clientId, keyPair, ethKeypair)

            Thread.sleep(3_000)
            checkRegisteredEthereumAddress(clientId, ethereumAddress)
        }
    }

    /**
     * @given two registered clients and one ethereum wallet
     * @when registration with occupied wallet send
     * @then the second registration fails
     */
    @Test
    fun doubleWalletRegistration() {
        Assertions.assertTimeoutPreemptively(timeoutDuration) {
            val ethKeypair = Keys.createEcKeyPair()
            val ethereumAddress = "0x${Keys.getAddress(ethKeypair.publicKey)}"

            val keyPair1 = ModelUtil.generateKeypair()
            val clientIrohaAccount1 = String.getRandomString(5)
            val clientId1 = "$clientIrohaAccount1@d3"

            val keyPair2 = ModelUtil.generateKeypair()
            val clientIrohaAccount2 = String.getRandomString(5)
            val clientId2 = "$clientIrohaAccount2@d3"

            // register iroha accounts
            val res1 = integrationHelper.sendRegistrationRequest(
                clientIrohaAccount1,
                keyPair1.public.toHexString(),
                registrationTestEnvironment.registrationConfig.port
            )
            assertEquals(200, res1.statusCode)

            val res2 = integrationHelper.sendRegistrationRequest(
                clientIrohaAccount2,
                keyPair2.public.toHexString(),
                registrationTestEnvironment.registrationConfig.port
            )
            assertEquals(200, res2.statusCode)

            // register in Ethereum client1
            integrationHelper.registerEthereumWallet(clientId1, keyPair1, ethKeypair)
            Thread.sleep(3_000)

            checkRegisteredEthereumAddress(clientId1, ethereumAddress)

            // register in Ethereum client2 with the same Ethereum address
            integrationHelper.registerEthereumWallet(clientId2, keyPair2, ethKeypair)
            Thread.sleep(3_000)

            // check wrong registration
            assertTrue {
                integrationHelper.queryHelper.getAccountDetails(
                    clientId2,
                    integrationHelper.accountHelper.notaryAccount.accountId,
                    ETH_FAILED_REGISTRATION_KEY
                ).get().isPresent
            }
        }
    }
}

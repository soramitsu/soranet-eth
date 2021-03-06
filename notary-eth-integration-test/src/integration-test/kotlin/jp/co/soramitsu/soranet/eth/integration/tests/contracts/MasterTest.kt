/*
 * Copyright Soramitsu Co., Ltd. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package jp.co.soramitsu.soranet.eth.integration.tests.contracts

import integration.helper.IrohaConfigHelper
import jp.co.soramitsu.soranet.eth.contract.BasicCoin
import jp.co.soramitsu.soranet.eth.contract.Master
import jp.co.soramitsu.soranet.eth.contract.MasterToken
import jp.co.soramitsu.soranet.eth.helper.hexStringToByteArray
import jp.co.soramitsu.soranet.eth.integration.helper.ContractTestHelper
import jp.co.soramitsu.soranet.eth.integration.helper.ContractTestHelper.Companion.TOKEN_DECIMALS
import jp.co.soramitsu.soranet.eth.integration.helper.ContractTestHelper.Companion.TOKEN_NAME
import jp.co.soramitsu.soranet.eth.integration.helper.ContractTestHelper.Companion.TOKEN_REWARD
import jp.co.soramitsu.soranet.eth.integration.helper.ContractTestHelper.Companion.TOKEN_SUPPLY
import jp.co.soramitsu.soranet.eth.integration.helper.ContractTestHelper.Companion.TOKEN_SUPPLY_BENEFICIARY
import jp.co.soramitsu.soranet.eth.integration.helper.ContractTestHelper.Companion.TOKEN_SYMBOL
import jp.co.soramitsu.soranet.eth.sidechain.util.hashToAddAndRemovePeer
import jp.co.soramitsu.soranet.eth.sidechain.util.hashToMint
import jp.co.soramitsu.soranet.eth.sidechain.util.hashToWithdraw
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.web3j.crypto.ECKeyPair
import org.web3j.crypto.Hash
import org.web3j.crypto.Keys
import org.web3j.protocol.exceptions.TransactionException
import java.math.BigInteger
import java.time.Duration
import kotlin.test.assertEquals

class MasterTest {
    private lateinit var cth: ContractTestHelper
    private lateinit var master: Master
    private lateinit var masterToken: MasterToken
    private lateinit var token: BasicCoin
    private lateinit var accMain: String
    private lateinit var accGreen: String
    private lateinit var keypair: ECKeyPair
    private lateinit var etherAddress: String

    private val timeoutDuration = Duration.ofMinutes(IrohaConfigHelper.timeoutMinutes)

    @BeforeEach
    fun setup() {
        cth = ContractTestHelper()
        master = cth.master
        masterToken = cth.masterToken
        token = cth.token
        accMain = cth.accMain
        accGreen = cth.accGreen
        keypair = cth.keypair
        etherAddress = cth.etherAddress
    }

    /**
     * @given deployed master and token contracts
     * @when the transaction committed
     * @then the supply is minted to the beneficiary
     */
    @Test
    fun correctDeployment() {
        Assertions.assertTimeoutPreemptively(timeoutDuration) {
            Assertions.assertEquals(
                TOKEN_SUPPLY,
                masterToken.totalSupply().send()
            )
            Assertions.assertEquals(
                TOKEN_SUPPLY,
                masterToken.balanceOf(TOKEN_SUPPLY_BENEFICIARY).send()
            )
        }
    }

    /**
     * @given deployed master and token contracts
     * @when a correct proof is submitted
     * @then the transaction applied and the reward is given out
     */
    @Test
    fun correctProofSubmission() {
        Assertions.assertTimeoutPreemptively(timeoutDuration) {
            val balanceBeforeProof = masterToken.balanceOf(accMain).send()
            cth.supplyProof()
            Assertions.assertEquals(
                balanceBeforeProof.add(TOKEN_REWARD),
                masterToken.balanceOf(accMain).send()
            )
        }
    }

    /**
     * @given deployed master and token contracts
     * @when a proof is applied for the second time
     * @then the transaction fails
     */
    @Test
    fun doubleProofSubmission() {
        Assertions.assertTimeoutPreemptively(timeoutDuration) {
            cth.supplyProof()
            Assertions.assertThrows(TransactionException::class.java) {
                cth.supplyProof()
            }
        }
    }

    /**
     * @given master account deployed
     * @when transfer 300_000_000 WEIs to master account
     * @then balance of master account increased by 300_000_000
     */
    @Test
    fun canAcceptEther() {
        Assertions.assertTimeoutPreemptively(timeoutDuration) {
            val initialBalance = cth.getETHBalance(master.contractAddress)
            cth.sendEthereum(BigInteger.valueOf(300_000_000), master.contractAddress)
            Assertions.assertEquals(
                initialBalance + BigInteger.valueOf(300_000_000),
                cth.getETHBalance(master.contractAddress)
            )
        }
    }

    /**
     * @given deployed master and token contracts
     * @when one peer added to master, 5 tokens transferred to master,
     * request to withdraw 1 token is sent to master
     * @then call to withdraw succeeded
     */
    @Test
    fun singleCorrectSignatureTokenTest() {
        Assertions.assertTimeoutPreemptively(timeoutDuration) {
            master.addToken(token.contractAddress).send()
            cth.transferTokensToMaster(BigInteger.valueOf(5))
            cth.supplyProof()
            cth.withdraw(BigInteger.valueOf(1))
            Assertions.assertEquals(
                BigInteger.valueOf(4),
                token.balanceOf(master.contractAddress).send()
            )
        }
    }

    /**
     * @given deployed master and token contracts
     * @when two peers added to master, 5 tokens transferred to master,
     * request to withdraw 1 token is sent to master
     * @then call to withdraw fails
     */
    @Test
    fun singleNotEnoughSignaturesTokenTest() {
        Assertions.assertTimeoutPreemptively(timeoutDuration) {
            cth.transferTokensToMaster(BigInteger.valueOf(5))
            cth.supplyProof()
            Assertions.assertThrows(TransactionException::class.java) {
                cth.withdraw(
                    BigInteger.valueOf(
                        1
                    )
                )
            }
        }
    }

    /**
     * @given deployed master and token contracts
     * @when one peer added to master, 5000 WEI transferred to master,
     * request to withdraw 10000 WEI is sent to master
     * @then call to withdraw fails
     */
    @Test
    fun notEnoughEtherTest() {
        Assertions.assertTimeoutPreemptively(timeoutDuration) {
            cth.sendEthereum(BigInteger.valueOf(5000), master.contractAddress)
            cth.supplyProof()
            Assertions.assertThrows(TransactionException::class.java) {
                cth.withdraw(
                    BigInteger.valueOf(10000),
                    accGreen,
                    etherAddress
                )
            }
        }
    }

    /**
     * @given deployed master and token contracts
     * @when one peer added to master, 5 tokens transferred to master,
     * request to withdraw 10 tokens is sent to master
     * @then call to withdraw fails
     */
    @Test
    fun notEnoughTokensTest() {
        Assertions.assertTimeoutPreemptively(timeoutDuration) {
            cth.transferTokensToMaster(BigInteger.valueOf(5))
            cth.supplyProof()
            Assertions.assertThrows(TransactionException::class.java) {
                cth.withdraw(
                    BigInteger.valueOf(
                        10
                    )
                )
            }
        }
    }

    /**
     * @given deployed master contract
     * @when one peer added to master, 5000 Wei transferred to master,
     * request to withdraw 1000 Wei is sent to master
     * @then call to withdraw succeeded
     */
    @Test
    fun singleCorrectSignatureEtherTestMaster() {
        Assertions.assertTimeoutPreemptively(timeoutDuration) {
            val initialBalance = cth.getETHBalance(accGreen)
            cth.sendEthereum(BigInteger.valueOf(5000), master.contractAddress)
            cth.supplyProof()
            cth.withdraw(
                BigInteger.valueOf(1000),
                accGreen,
                etherAddress
            )
            Assertions.assertEquals(
                BigInteger.valueOf(4000),
                cth.getETHBalance(master.contractAddress)
            )
            Assertions.assertEquals(
                initialBalance + BigInteger.valueOf(1000),
                cth.getETHBalance(accGreen)
            )
        }
    }

    /**
     * @given deployed master contract
     * @when one peer added to master, 5000 Wei transferred to master,
     * request to withdraw 1000 Wei is sent to master but no proof supplied
     * @then call to withdraw fails
     */
    @Test
    fun singleNoProofEtherTestMaster() {
        Assertions.assertTimeoutPreemptively(timeoutDuration) {
            cth.sendEthereum(BigInteger.valueOf(5000), master.contractAddress)
            Assertions.assertThrows(TransactionException::class.java) {
                cth.withdraw(
                    BigInteger.valueOf(1000),
                    accGreen,
                    etherAddress
                )
            }
        }
    }

    /**
     * @given deployed master contract
     * @when one peer added to master, 5000 Wei transferred to master,
     * request to withdraw 10000 Wei is sent to master - withdrawal fails
     * then add 5000 Wei to master (simulate vacuum process) and
     * request to withdraw 10000 Wei is sent to master
     * @then the second call to withdraw succeeded
     */
    @Test
    fun secondWithdrawAfterVacuum() {
        Assertions.assertTimeoutPreemptively(timeoutDuration) {
            val initialBalance = cth.getETHBalance(accGreen)
            cth.sendEthereum(BigInteger.valueOf(5000), master.contractAddress)
            cth.supplyProof()
            Assertions.assertThrows(TransactionException::class.java) {
                cth.withdraw(
                    BigInteger.valueOf(10000),
                    accGreen,
                    etherAddress
                )
            }

            cth.sendEthereum(BigInteger.valueOf(5000), master.contractAddress)
            cth.withdraw(
                BigInteger.valueOf(10000),
                accGreen,
                etherAddress
            )
            Assertions.assertEquals(
                BigInteger.valueOf(0),
                cth.getETHBalance(master.contractAddress)
            )
            Assertions.assertEquals(
                initialBalance + BigInteger.valueOf(10000),
                cth.getETHBalance(accGreen)
            )
        }
    }

    /**
     * @given deployed master and token contracts
     * @when 5 tokens transferred to master,
     * request to withdraw 1 token is sent to master
     * @then withdraw attempt fails
     */
    @Test
    fun noPeersWithdraw() {
        Assertions.assertTimeoutPreemptively(timeoutDuration) {
            cth.transferTokensToMaster(BigInteger.valueOf(5))
            cth.supplyProof()
            Assertions.assertThrows(TransactionException::class.java) {
                cth.withdraw(
                    BigInteger.valueOf(
                        1
                    )
                )
            }
        }
    }

    /**
     * @given deployed master and token contracts
     * @when one peer added to master, 5 tokens transferred to master,
     * request to withdraw 1 token is sent to master with r-array larger than v and s
     * @then withdraw attempt fails
     */
    @Test
    fun differentVRS() {
        Assertions.assertTimeoutPreemptively(timeoutDuration) {
            cth.transferTokensToMaster(BigInteger.valueOf(5))
            cth.supplyProof()
            val amount = BigInteger.valueOf(1)
            val finalHash =
                hashToWithdraw(
                    token.contractAddress,
                    amount.toString(),
                    accGreen,
                    cth.defaultIrohaHash,
                    master.contractAddress
                )

            val sigs = cth.prepareSignatures(1, listOf(keypair), finalHash)
            sigs.rr.add(sigs.rr.first())

            Assertions.assertThrows(TransactionException::class.java) {
                master.withdraw(
                    token.contractAddress,
                    amount,
                    accGreen,
                    cth.defaultByteHash,
                    sigs.vv,
                    sigs.rr,
                    sigs.ss,
                    master.contractAddress
                ).send()
            }
        }
    }

    /**
     * @given deployed master and token contracts
     * @when one peer added to master, 5 tokens transferred to master,
     * request to withdraw 1 token is sent to master with valid signature repeated twice
     * @then call to withdraw fails
     */
    @Test
    fun sameSignatures() {
        Assertions.assertTimeoutPreemptively(timeoutDuration) {
            cth.transferTokensToMaster(BigInteger.valueOf(5))
            cth.supplyProof()
            val amount = BigInteger.valueOf(1)
            val finalHash =
                hashToWithdraw(
                    token.contractAddress,
                    amount.toString(),
                    accGreen,
                    cth.defaultIrohaHash,
                    master.contractAddress
                )
            val sigs = cth.prepareSignatures(2, listOf(keypair, keypair), finalHash)

            Assertions.assertThrows(TransactionException::class.java) {
                master.withdraw(
                    token.contractAddress,
                    amount,
                    accGreen,
                    cth.defaultByteHash,
                    sigs.vv,
                    sigs.rr,
                    sigs.ss,
                    master.contractAddress
                ).send()
            }
        }
    }

    /**
     * @given deployed master contract
     * @when one peer added to master, 5 tokens transferred to master,
     * request to withdraw 1 token is sent to master with two valid signatures:
     * one from added peer and another one from some unknown peer
     * @then call to withdraw fails
     */
    @Test
    fun wrongPeerSignature() {
        Assertions.assertTimeoutPreemptively(timeoutDuration) {
            cth.transferTokensToMaster(BigInteger.valueOf(5))
            cth.supplyProof()
            val amount = BigInteger.valueOf(1)
            val finalHash =
                hashToWithdraw(
                    token.contractAddress,
                    amount.toString(),
                    accGreen,
                    cth.defaultIrohaHash,
                    master.contractAddress
                )
            val sigs = cth.prepareSignatures(2, listOf(keypair, Keys.createEcKeyPair()), finalHash)

            Assertions.assertThrows(TransactionException::class.java) {
                master.withdraw(
                    token.contractAddress,
                    amount,
                    accGreen,
                    cth.defaultByteHash,
                    sigs.vv,
                    sigs.rr,
                    sigs.ss,
                    master.contractAddress
                ).send()
            }
        }
    }

    /**
     * @given deployed master and token contracts
     * @when one peer added to master, 5 tokens transferred to master,
     * request to withdraw 1 token is sent to master with invalid signature
     * @then call to withdraw fails
     */
    @Test
    fun invalidSignature() {
        Assertions.assertTimeoutPreemptively(timeoutDuration) {
            cth.transferTokensToMaster(BigInteger.valueOf(5))
            cth.supplyProof()
            val amount = BigInteger.valueOf(1)
            val finalHash =
                hashToWithdraw(
                    token.contractAddress,
                    amount.toString(),
                    accGreen,
                    cth.defaultIrohaHash,
                    master.contractAddress
                )

            val sigs = cth.prepareSignatures(1, listOf(keypair), finalHash)
            sigs.ss.first()[0] = sigs.ss.first()[0].inc()

            Assertions.assertThrows(TransactionException::class.java) {
                master.withdraw(
                    token.contractAddress,
                    amount,
                    accGreen,
                    cth.defaultByteHash,
                    sigs.vv,
                    sigs.rr,
                    sigs.ss,
                    master.contractAddress
                ).send()
            }
        }
    }

    /**
     * @given deployed master and token contracts
     * @when one peer added to master, 5 tokens transferred to master,
     * request to withdraw 1 token is sent to master twice
     * @then second call to withdraw failed
     */
    @Test
    fun usedHashTest() {
        Assertions.assertTimeoutPreemptively(timeoutDuration) {
            master.addToken(token.contractAddress).send()
            cth.transferTokensToMaster(BigInteger.valueOf(5))
            cth.supplyProof()
            cth.withdraw(BigInteger.valueOf(1))
            Assertions.assertEquals(
                BigInteger.valueOf(4),
                token.balanceOf(master.contractAddress).send()
            )
            Assertions.assertThrows(TransactionException::class.java) {
                cth.withdraw(
                    BigInteger.valueOf(
                        1
                    )
                )
            }
        }
    }

    /**
     * @given deployed master contract
     * @when addToken called twice with different addresses
     * @then both calls succeeded, both tokens are added, there are 3 tokens total (2 added + 1 XOR)
     */
    @Test
    fun addTokenTest() {
        Assertions.assertTimeoutPreemptively(timeoutDuration) {
            val fakeTokenAddress = "0xf230b790e05390fc8295f4d3f60332c93bed42d1"
            master.addToken(token.contractAddress).send()
            master.addToken(fakeTokenAddress).send()
            Assertions.assertTrue(master.isToken(token.contractAddress).send())
            Assertions.assertTrue(master.isToken(fakeTokenAddress).send())
        }
    }

    /**
     * @given deployed master contract
     * @when addToken called twice with same addresses
     * @then second call throws exception
     */
    @Test
    fun addSameTokensTest() {
        Assertions.assertTimeoutPreemptively(timeoutDuration) {
            master.addToken(token.contractAddress).send()
            Assertions.assertThrows(TransactionException::class.java) {
                master.addToken(token.contractAddress).send()
            }
        }
    }

    /**
     * @given deployed master contract
     * @when 4 different peers are added to master, 5000 Wei is transferred to master,
     * request to withdraw 1000 Wei is sent to master with 4 signatures from added peers
     * @then withdraw call succeeded
     */
    @Test
    fun testWithdrawalValidSignatures4of4() {
        Assertions.assertTimeoutPreemptively(timeoutDuration) {
            val sigCount = 4
            val amountToSend = 1000
            val initialBalance = cth.getETHBalance(accGreen)
            val (keyPairs, peers) = cth.getKeyPairsAndPeers(sigCount)

            val master = cth.deployHelper.deployMasterSmartContract(
                peers,
                TOKEN_NAME,
                TOKEN_SYMBOL,
                TOKEN_DECIMALS,
                TOKEN_SUPPLY_BENEFICIARY,
                TOKEN_SUPPLY,
                TOKEN_REWARD
            )

            cth.sendEthereum(BigInteger.valueOf(5000), master.contractAddress)
            cth.supplyProof(sigCount, keyPairs, master)

            val finalHash =
                hashToWithdraw(
                    etherAddress,
                    amountToSend.toString(),
                    accGreen,
                    cth.defaultIrohaHash,
                    master.contractAddress
                )

            val sigs = cth.prepareSignatures(sigCount, keyPairs, finalHash)

            master.withdraw(
                etherAddress,
                BigInteger.valueOf(amountToSend.toLong()),
                accGreen,
                cth.defaultByteHash,
                sigs.vv,
                sigs.rr,
                sigs.ss,
                master.contractAddress
            ).send()

            Assertions.assertEquals(
                BigInteger.valueOf(4000),
                cth.getETHBalance(master.contractAddress)
            )
            Assertions.assertEquals(
                initialBalance + BigInteger.valueOf(1000),
                cth.getETHBalance(accGreen)
            )
        }
    }

    /**
     * @given deployed master contract
     * @when 3 different peers are added to master, 5000 Wei is transferred to master,
     * request to withdraw 1000 Wei is sent to master with 4 signatures from added peers (one more signature)
     * @then withdraw call succeeded
     */
    @Test
    fun testWithdrawValidSignatures4of3() {
        Assertions.assertTimeoutPreemptively(timeoutDuration) {
            val sigCount = 4
            val amountToSend = 1000
            val initialBalance = cth.getETHBalance(accGreen)
            val (keyPairs, peers) = cth.getKeyPairsAndPeers(sigCount)

            val master = cth.deployHelper.deployMasterSmartContract(
                peers.dropLast(1),
                TOKEN_NAME,
                TOKEN_SYMBOL,
                TOKEN_DECIMALS,
                TOKEN_SUPPLY_BENEFICIARY,
                TOKEN_SUPPLY,
                TOKEN_REWARD
            )

            cth.sendEthereum(BigInteger.valueOf(5000), master.contractAddress)

            val finalHash =
                hashToWithdraw(
                    etherAddress,
                    amountToSend.toString(),
                    accGreen,
                    cth.defaultIrohaHash,
                    master.contractAddress
                )

            val sigs = cth.prepareSignatures(sigCount, keyPairs, finalHash)

            cth.supplyProof(sigCount, keyPairs, master)

            master.withdraw(
                etherAddress,
                BigInteger.valueOf(amountToSend.toLong()),
                accGreen,
                cth.defaultByteHash,
                sigs.vv,
                sigs.rr,
                sigs.ss,
                master.contractAddress
            ).send()

            Assertions.assertEquals(
                BigInteger.valueOf(4000),
                cth.getETHBalance(master.contractAddress)
            )
            Assertions.assertEquals(
                initialBalance + BigInteger.valueOf(1000),
                cth.getETHBalance(accGreen)
            )
        }
    }

    /**
     * @given deployed master contract
     * @when 4 different peers are added to master, 5000 Wei is transferred to master,
     * request to withdraw 1000 Wei is sent to master with 3 signatures from added peers
     * @then withdraw call succeeded
     */
    @Test
    fun validSignatures3of4() {
        Assertions.assertTimeoutPreemptively(timeoutDuration) {
            val sigCount = 4
            val realSigCount = 3
            val amountToSend = 1000
            val tokenAddress = etherAddress
            val initialBalance = cth.getETHBalance(accGreen)
            val (keyPairs, peers) = cth.getKeyPairsAndPeers(sigCount)

            val master = cth.deployHelper.deployMasterSmartContract(
                peers,
                TOKEN_NAME,
                TOKEN_SYMBOL,
                TOKEN_DECIMALS,
                TOKEN_SUPPLY_BENEFICIARY,
                TOKEN_SUPPLY,
                TOKEN_REWARD
            )

            cth.sendEthereum(BigInteger.valueOf(5000), master.contractAddress)

            val finalHash =
                hashToWithdraw(
                    tokenAddress,
                    amountToSend.toString(),
                    accGreen,
                    cth.defaultIrohaHash,
                    master.contractAddress
                )

            val sigs =
                cth.prepareSignatures(realSigCount, keyPairs.subList(0, realSigCount), finalHash)

            cth.supplyProof(realSigCount, keyPairs, master)

            master.withdraw(
                tokenAddress,
                BigInteger.valueOf(amountToSend.toLong()),
                accGreen,
                cth.defaultByteHash,
                sigs.vv,
                sigs.rr,
                sigs.ss,
                master.contractAddress
            ).send()

            Assertions.assertEquals(
                BigInteger.valueOf(4000),
                cth.getETHBalance(master.contractAddress)
            )
            Assertions.assertEquals(
                initialBalance + BigInteger.valueOf(1000),
                cth.getETHBalance(accGreen)
            )
        }
    }

    /**
     * @given deployed master contract
     * @when 5 different peers are added to master, 5000 Wei is transferred to master,
     * request to withdraw 1000 Wei is sent to master with 3 signatures from added peers
     * @then withdraw call failed
     */
    @Test
    fun validSignatures3of5() {
        Assertions.assertTimeoutPreemptively(timeoutDuration) {
            val sigCount = 5
            val realSigCount = 3
            val amountToSend = 1000
            val tokenAddress = etherAddress

            val (keyPairs, peers) = cth.getKeyPairsAndPeers(sigCount)

            val master = cth.deployHelper.deployMasterSmartContract(
                peers,
                TOKEN_NAME,
                TOKEN_SYMBOL,
                TOKEN_DECIMALS,
                TOKEN_SUPPLY_BENEFICIARY,
                TOKEN_SUPPLY,
                TOKEN_REWARD
            )

            val finalHash =
                hashToWithdraw(
                    tokenAddress,
                    amountToSend.toString(),
                    accGreen,
                    cth.defaultIrohaHash,
                    master.contractAddress
                )

            val sigs =
                cth.prepareSignatures(realSigCount, keyPairs.subList(0, realSigCount), finalHash)

            cth.supplyProof()

            Assertions.assertThrows(TransactionException::class.java) {
                master.withdraw(
                    tokenAddress,
                    BigInteger.valueOf(amountToSend.toLong()),
                    accGreen,
                    cth.defaultByteHash,
                    sigs.vv,
                    sigs.rr,
                    sigs.ss,
                    master.contractAddress
                ).send()
            }
        }
    }

    /**
     * @given deployed master contract
     * @when 100 different peers are added to master, 5000 Wei is transferred to master,
     * request to withdraw 1000 Wei is sent to master with 50 signatures from added peers
     * @then withdraw call succeeded
     */
    @Test
    fun validSignatures50of50() {
        Assertions.assertTimeoutPreemptively(timeoutDuration) {
            val sigCount = 50
            val amountToSend = 1000
            val tokenAddress = etherAddress
            val initialBalance = cth.getETHBalance(accGreen)
            val (keyPairs, peers) = cth.getKeyPairsAndPeers(sigCount)

            val master = cth.deployHelper.deployMasterSmartContract(
                peers,
                TOKEN_NAME,
                TOKEN_SYMBOL,
                TOKEN_DECIMALS,
                TOKEN_SUPPLY_BENEFICIARY,
                TOKEN_SUPPLY,
                TOKEN_REWARD
            )

            cth.sendEthereum(BigInteger.valueOf(5000), master.contractAddress)

            val finalHash =
                hashToWithdraw(
                    tokenAddress,
                    amountToSend.toString(),
                    accGreen,
                    cth.defaultIrohaHash,
                    master.contractAddress
                )

            val sigs = cth.prepareSignatures(sigCount, keyPairs, finalHash)

            cth.supplyProof(sigCount, keyPairs, master)

            master.withdraw(
                tokenAddress,
                BigInteger.valueOf(amountToSend.toLong()),
                accGreen,
                cth.defaultByteHash,
                sigs.vv,
                sigs.rr,
                sigs.ss,
                master.contractAddress
            ).send()

            Assertions.assertEquals(
                BigInteger.valueOf(4000),
                cth.getETHBalance(master.contractAddress)
            )
            Assertions.assertEquals(
                initialBalance + BigInteger.valueOf(1000),
                cth.getETHBalance(accGreen)
            )
        }
    }

    /**
     * @given deployed master contract
     * @when 50 different peers are added to master, 5000 Wei is transferred to master,
     * request to withdraw 1000 Wei is sent to master with 67 signatures from added peers
     * @then withdraw call succeeded
     */
    @Test
    fun validSignatures34of50() {
        Assertions.assertTimeoutPreemptively(timeoutDuration) {
            val sigCount = 50
            val realSigCount = 34
            val amountToSend = 1000
            val tokenAddress = etherAddress
            val initialBalance = cth.getETHBalance(accGreen)
            val (keyPairs, peers) = cth.getKeyPairsAndPeers(sigCount)

            val master = cth.deployHelper.deployMasterSmartContract(
                peers,
                TOKEN_NAME,
                TOKEN_SYMBOL,
                TOKEN_DECIMALS,
                TOKEN_SUPPLY_BENEFICIARY,
                TOKEN_SUPPLY,
                TOKEN_REWARD
            )
            cth.sendEthereum(BigInteger.valueOf(5000), master.contractAddress)

            val finalHash =
                hashToWithdraw(
                    tokenAddress,
                    amountToSend.toString(),
                    accGreen,
                    cth.defaultIrohaHash,
                    master.contractAddress
                )

            val sigs =
                cth.prepareSignatures(realSigCount, keyPairs.subList(0, realSigCount), finalHash)

            cth.supplyProof(realSigCount, keyPairs, master)

            master.withdraw(
                tokenAddress,
                BigInteger.valueOf(amountToSend.toLong()),
                accGreen,
                cth.defaultByteHash,
                sigs.vv,
                sigs.rr,
                sigs.ss,
                master.contractAddress
            ).send()

            Assertions.assertEquals(
                BigInteger.valueOf(4000),
                cth.getETHBalance(master.contractAddress)
            )
            Assertions.assertEquals(
                initialBalance + BigInteger.valueOf(1000),
                cth.getETHBalance(accGreen)
            )
        }
    }

    /**
     * @given deployed master contract
     * @when 50 different peers are added to master, 5000 Wei is transferred to master,
     * request to withdraw 1000 Wei is sent to master with 66 signatures from added peers
     * @then withdraw call failed
     */
    @Test
    fun validSignatures33of50() {
        Assertions.assertTimeoutPreemptively(timeoutDuration) {
            val sigCount = 50
            val realSigCount = 33
            val amountToSend = 1000
            val tokenAddress = etherAddress

            val (keyPairs, peers) = cth.getKeyPairsAndPeers(sigCount)

            val master = cth.deployHelper.deployMasterSmartContract(
                peers,
                TOKEN_NAME,
                TOKEN_SYMBOL,
                TOKEN_DECIMALS,
                TOKEN_SUPPLY_BENEFICIARY,
                TOKEN_SUPPLY,
                TOKEN_REWARD
            )

            val finalHash =
                hashToWithdraw(
                    tokenAddress,
                    amountToSend.toString(),
                    accGreen,
                    cth.defaultIrohaHash,
                    master.contractAddress
                )

            val sigs =
                cth.prepareSignatures(realSigCount, keyPairs.subList(0, realSigCount), finalHash)

            cth.supplyProof()

            Assertions.assertThrows(TransactionException::class.java) {
                master.withdraw(
                    tokenAddress,
                    BigInteger.valueOf(amountToSend.toLong()),
                    accGreen,
                    cth.defaultByteHash,
                    sigs.vv,
                    sigs.rr,
                    sigs.ss,
                    master.contractAddress
                ).send()
            }
        }
    }

    /**
     * @given master contracts
     * @when try to add one more peer address by all valid peers
     * @then the new peer should be added
     */
    @Test
    fun addNewPeerByPeers4of4() {
        Assertions.assertTimeoutPreemptively(timeoutDuration) {
            val newPeer = "0x006fe444ffbaffb27813265c50a479897b8a2514"
            val sigCount = 4
            val realSigCount = 4

            val finalHash = hashToAddAndRemovePeer(
                newPeer,
                cth.defaultIrohaHash
            )

            val (keyPairs, peers) = cth.getKeyPairsAndPeers(sigCount)

            val master = cth.deployHelper.deployMasterSmartContract(
                peers,
                TOKEN_NAME,
                TOKEN_SYMBOL,
                TOKEN_DECIMALS,
                TOKEN_SUPPLY_BENEFICIARY,
                TOKEN_SUPPLY,
                TOKEN_REWARD
            )

            val sigs =
                cth.prepareSignatures(realSigCount, keyPairs.subList(0, realSigCount), finalHash)

            val result = master.addPeerByPeer(
                newPeer,
                cth.defaultByteHash,
                sigs.vv,
                sigs.rr,
                sigs.ss
            ).send().isStatusOK

            Assertions.assertTrue(result)
            Assertions.assertTrue(master.isPeer(newPeer).send())
            peers.forEach { oldPeer ->
                Assertions.assertTrue(master.isPeer(oldPeer).send())
            }
        }
    }

    /**
     * @given master contracts
     * @when try to add one more peer address by all valid peers and one wrong signature
     * @then the new peer should be added
     */
    @Test
    fun addNewPeerByPeers3of4() {
        Assertions.assertTimeoutPreemptively(timeoutDuration) {
            val newPeer = "0x006fe444ffbaffb27813265c50a479897b8a2514"
            val sigCount = 4
            val realSigCount = 4

            val finalHash = hashToAddAndRemovePeer(
                newPeer,
                cth.defaultIrohaHash
            )

            val (keyPairs, peers) = cth.getKeyPairsAndPeers(sigCount)

            val master = cth.deployHelper.deployMasterSmartContract(
                peers,
                TOKEN_NAME,
                TOKEN_SYMBOL,
                TOKEN_DECIMALS,
                TOKEN_SUPPLY_BENEFICIARY,
                TOKEN_SUPPLY,
                TOKEN_REWARD
            )

            val sigs =
                cth.prepareSignatures(realSigCount, keyPairs.subList(0, realSigCount), finalHash)

            val result = master.addPeerByPeer(
                newPeer,
                cth.defaultByteHash,
                sigs.vv,
                sigs.rr,
                sigs.ss
            ).send().isStatusOK

            Assertions.assertTrue(result)
            Assertions.assertTrue(master.isPeer(newPeer).send())

            peers.forEach { oldPeer ->
                Assertions.assertTrue(master.isPeer(oldPeer).send())
            }
        }
    }

    /**
     * @given master contracts, peer is added with hash
     * @when try to remove peer address by all valid peers and then add peer by used hash
     * @then the peer should not be added again if tx hash is already used
     */
    @Test
    fun addNewPeerByPeersUseSameHash() {
        Assertions.assertTimeoutPreemptively(timeoutDuration) {
            val newPeer = "0x006fe444ffbaffb27813265c50a479897b8a2512"
            val sigCount = 4
            val realSigCount = 4

            val finalHash = hashToAddAndRemovePeer(
                newPeer,
                cth.defaultIrohaHash
            )

            val (keyPairs, peers) = cth.getKeyPairsAndPeers(sigCount)

            val master = cth.deployHelper.deployMasterSmartContract(
                peers,
                TOKEN_NAME,
                TOKEN_SYMBOL,
                TOKEN_DECIMALS,
                TOKEN_SUPPLY_BENEFICIARY,
                TOKEN_SUPPLY,
                TOKEN_REWARD
            )

            val sigs =
                cth.prepareSignatures(realSigCount, keyPairs.subList(0, realSigCount), finalHash)

            // first call
            val resultAdd = master.addPeerByPeer(
                newPeer,
                cth.defaultByteHash,
                sigs.vv,
                sigs.rr,
                sigs.ss
            ).send().isStatusOK

            Assertions.assertTrue(resultAdd)
            Assertions.assertTrue(master.isPeer(newPeer).send())
            peers.forEach { oldPeer ->
                Assertions.assertTrue(master.isPeer(oldPeer).send())
            }

            val removeIrohaHash = Hash.sha3(String.format("%064x", BigInteger.valueOf(54321)))
            val removeHash = hashToAddAndRemovePeer(
                newPeer,
                removeIrohaHash
            )
            val removeByteHash = hexStringToByteArray(removeIrohaHash)
            val removeSigs =
                cth.prepareSignatures(realSigCount, keyPairs.subList(0, realSigCount), removeHash)

            // remove peer
            val resultRemove = master.removePeerByPeer(
                newPeer,
                removeByteHash,
                removeSigs.vv,
                removeSigs.rr,
                removeSigs.ss
            ).send().isStatusOK

            Assertions.assertTrue(resultRemove)
            Assertions.assertFalse(master.isPeer(newPeer).send())
            peers.forEach { oldPeer ->
                Assertions.assertTrue(master.isPeer(oldPeer).send())
            }

            // second call to add peer with the same hash
            Assertions.assertThrows(TransactionException::class.java) {
                master.addPeerByPeer(
                    newPeer,
                    cth.defaultByteHash,
                    sigs.vv,
                    sigs.rr,
                    sigs.ss
                ).send()
            }

            Assertions.assertFalse(master.isPeer(newPeer).send())
        }
    }

    /**
     * @given master contracts
     * @when try to add one more peer address by all valid peers
     * @then the tx should be failed and new peer is not saved
     */
    @Test
    fun addNewPeerByPeers2of4() {
        Assertions.assertTimeoutPreemptively(timeoutDuration) {
            val newPeer = "0x006fe444ffbaffb27813265c50a479897b8a2514"
            val sigCount = 4
            val realSigCount = 2

            val finalHash =
                hashToAddAndRemovePeer(
                    newPeer,
                    cth.defaultIrohaHash
                )

            val (keyPairs, peers) = cth.getKeyPairsAndPeers(sigCount)
            val master = cth.deployHelper.deployMasterSmartContract(
                peers,
                TOKEN_NAME,
                TOKEN_SYMBOL,
                TOKEN_DECIMALS,
                TOKEN_SUPPLY_BENEFICIARY,
                TOKEN_SUPPLY,
                TOKEN_REWARD
            )

            val sigs =
                cth.prepareSignatures(realSigCount, keyPairs.subList(0, realSigCount), finalHash)

            Assertions.assertThrows(TransactionException::class.java) {
                master.addPeerByPeer(
                    newPeer,
                    cth.defaultByteHash,
                    sigs.vv,
                    sigs.rr,
                    sigs.ss
                ).send().isStatusOK
            }

            Assertions.assertFalse(master.isPeer(newPeer).send())
            peers.forEach { oldPeer ->
                Assertions.assertTrue(master.isPeer(oldPeer).send())
            }
        }
    }

    /**
     * @given master contracts
     * @when try to remove one peer address by rest of peers
     * @then the tx should be successful and peer is removed
     */
    @Test
    fun removePeerByPeers() {
        Assertions.assertTimeoutPreemptively(timeoutDuration) {
            val peerToRemove = "0x006fe444ffbaffb27813265c50a479897b8a2514"
            val sigCount = 4
            val realSigCount = 4

            val (keyPairs, peers) = cth.getKeyPairsAndPeers(sigCount)

            // deploy with peer which will be removed
            val withPeerToRemove = peers.toMutableList()
            withPeerToRemove.add(peerToRemove)
            val master = cth.deployHelper.deployMasterSmartContract(
                withPeerToRemove,
                TOKEN_NAME,
                TOKEN_SYMBOL,
                TOKEN_DECIMALS,
                TOKEN_SUPPLY_BENEFICIARY,
                TOKEN_SUPPLY,
                TOKEN_REWARD
            )

            withPeerToRemove.forEach { oldPeer ->
                Assertions.assertTrue(master.isPeer(oldPeer).send())
            }

            val finalHash = hashToAddAndRemovePeer(
                withPeerToRemove.last(),
                cth.defaultIrohaHash
            )

            val sigs = cth.prepareSignatures(realSigCount, keyPairs.subList(0, realSigCount), finalHash)

            Assertions.assertTrue(master.isPeer(peerToRemove).send())
            Assertions.assertTrue(
                master.removePeerByPeer(
                    withPeerToRemove.last(),
                    cth.defaultByteHash,
                    sigs.vv,
                    sigs.rr,
                    sigs.ss
                ).send().isStatusOK
            )

            Assertions.assertFalse(master.isPeer(withPeerToRemove.last()).send())
            peers.forEach { oldPeer ->
                Assertions.assertTrue(master.isPeer(oldPeer).send())
            }
            Assertions.assertFalse(master.isPeer(peerToRemove).send())
        }
    }

    /**
     * @given master and sora token
     * @when try to mint
     * @then should be minted new tokens
     */
    @Test
    fun mintNewTokens4of4peers() {
        Assertions.assertTimeoutPreemptively(timeoutDuration) {
            val sigCount = 4
            val realSigCount = 4

            val beneficiary = "0xbcBCeb4D66065B7b34d1B90f4fa572829F2c6D5c"
            val amountToSend = 1000

            val (keyPairs, peers) = cth.getKeyPairsAndPeers(sigCount)

            val master = cth.deployHelper.deployUpgradableMasterSmartContract(
                peers,
                TOKEN_NAME,
                TOKEN_SYMBOL,
                TOKEN_DECIMALS,
                TOKEN_SUPPLY_BENEFICIARY,
                TOKEN_SUPPLY,
                TOKEN_REWARD
            )
            val xorAddress = master.tokenInstance().send()

            val finalHash = hashToMint(
                xorAddress,
                amountToSend.toString(),
                beneficiary,
                cth.defaultIrohaHash,
                cth.master.contractAddress
            )
            val sigs = cth.prepareSignatures(realSigCount, keyPairs.subList(0, realSigCount), finalHash)

            cth.supplyProof(realSigCount, keyPairs, master)

            Assertions.assertTrue(
                master.mintTokensByPeers(
                    xorAddress,
                    BigInteger.valueOf(amountToSend.toLong()),
                    beneficiary,
                    cth.defaultByteHash,
                    sigs.vv,
                    sigs.rr,
                    sigs.ss,
                    cth.master.contractAddress
                ).send().isStatusOK
            )

            Assertions.assertEquals(
                amountToSend,
                cth.getTokenContract(master.tokenInstance().send()).balanceOf(beneficiary).send().toInt()
            )
        }
    }

    /**
     * @given master and sora token
     * @when try to mint if one extra wrong signature provided
     * @then should be minted new tokens
     */
    @Test
    fun mintNewTokens4of3peers() {
        Assertions.assertTimeoutPreemptively(timeoutDuration) {
            val sigCount = 4
            val realSigCount = 4

            val beneficiary = "0xbcBCeb4D66065B7b34d1B90f4fa572829F2c6D5c"
            val amountToSend = 1000

            val (keyPairs, peers) = cth.getKeyPairsAndPeers(sigCount)

            val master = cth.deployHelper.deployUpgradableMasterSmartContract(
                peers.dropLast(1),
                TOKEN_NAME,
                TOKEN_SYMBOL,
                TOKEN_DECIMALS,
                TOKEN_SUPPLY_BENEFICIARY,
                TOKEN_SUPPLY,
                TOKEN_REWARD
            )
            val xorToken = master.tokenInstance().send()

            val finalHash = hashToMint(
                xorToken,
                amountToSend.toString(),
                beneficiary,
                cth.defaultIrohaHash,
                cth.master.contractAddress
            )
            val sigs =
                cth.prepareSignatures(realSigCount, keyPairs.subList(0, realSigCount), finalHash)

            cth.supplyProof(realSigCount - 1, keyPairs, master)

            Assertions.assertTrue(
                master.mintTokensByPeers(
                    xorToken,
                    BigInteger.valueOf(amountToSend.toLong()),
                    beneficiary,
                    cth.defaultByteHash,
                    sigs.vv,
                    sigs.rr,
                    sigs.ss,
                    cth.master.contractAddress
                ).send().isStatusOK
            )

            Assertions.assertEquals(
                amountToSend,
                cth.getTokenContract(master.tokenInstance().send()).balanceOf(beneficiary).send().toInt()
            )
        }
    }

    /**
     * @given master and sora token
     * @when try to mint without proof supplied
     * @then minting fails
     */
    @Test
    fun mintNewTokens4of4peersNoProof() {
        Assertions.assertTimeoutPreemptively(timeoutDuration) {
            val sigCount = 4
            val realSigCount = 4

            val beneficiary = "0xbcBCeb4D66065B7b34d1B90f4fa572829F2c6D5c"
            val amountToSend = 1000

            val (keyPairs, peers) = cth.getKeyPairsAndPeers(sigCount)

            val master = cth.deployHelper.deployUpgradableMasterSmartContract(
                peers,
                TOKEN_NAME,
                TOKEN_SYMBOL,
                TOKEN_DECIMALS,
                TOKEN_SUPPLY_BENEFICIARY,
                TOKEN_SUPPLY,
                TOKEN_REWARD
            )
            val xorAddress = master.tokenInstance().send()

            val finalHash = hashToMint(
                xorAddress,
                amountToSend.toString(),
                beneficiary,
                cth.defaultIrohaHash,
                cth.master.contractAddress
            )
            val sigs = cth.prepareSignatures(realSigCount, keyPairs.subList(0, realSigCount), finalHash)

            Assertions.assertThrows(TransactionException::class.java) {
                master.mintTokensByPeers(
                    xorAddress,
                    BigInteger.valueOf(amountToSend.toLong()),
                    beneficiary,
                    cth.defaultByteHash,
                    sigs.vv,
                    sigs.rr,
                    sigs.ss,
                    cth.master.contractAddress
                ).send().isStatusOK
            }
        }
    }

    /**
     * @given master contract and sora token and mint is called with Iroha hash
     * @when mint is called with the same hash
     * @then mint call fails
     */
    @Test
    fun mintWithSameHash() {
        Assertions.assertTimeoutPreemptively(timeoutDuration) {
            val beneficiary = "0xbcBCeb4D66065B7b34d1B90f4fa572829F2c6D5c"
            val amountToSend: Long = 1000

            cth.supplyProof()

            val result = cth.mintByPeer(beneficiary, amountToSend).isStatusOK

            Assertions.assertTrue(result)
            Assertions.assertEquals(
                amountToSend,
                cth.getTokenContract(master.tokenInstance().send()).balanceOf(beneficiary).send().toLong()
            )

            Assertions.assertThrows(TransactionException::class.java) {
                cth.mintByPeer(beneficiary, amountToSend)
            }
            Assertions.assertEquals(
                amountToSend,
                cth.getTokenContract(master.tokenInstance().send()).balanceOf(beneficiary).send().toLong()
            )
        }
    }

    /**
     * @given master contract with token
     * @when check token balance
     * @then master contract balance should be equals to totalSupply which is 0
     */
    @Test
    fun checkMasterBalance() {
        Assertions.assertTimeoutPreemptively(timeoutDuration) {
            assertEquals(
                BigInteger("0"),
                cth.getTokenContract(master.tokenInstance().send()).balanceOf(master.contractAddress).send()
            )
        }
    }
}

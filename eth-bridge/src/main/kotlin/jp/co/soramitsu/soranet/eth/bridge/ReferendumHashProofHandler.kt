/*
 * Copyright Soramitsu Co., Ltd. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package jp.co.soramitsu.soranet.eth.bridge

import com.d3.commons.sidechain.iroha.consumer.IrohaConsumer
import com.d3.commons.sidechain.iroha.util.ModelUtil
import com.d3.commons.util.GsonInstance
import com.d3.commons.util.irohaEscape
import iroha.protocol.BlockOuterClass
import iroha.protocol.Commands
import jp.co.soramitsu.soranet.eth.sidechain.util.*
import mu.KLogging
import org.web3j.crypto.Credentials

const val SORA_PROOF_DOMAIN = "@soraProof"

/**
 * Class responsible for referendum merkle root signing
 */
class ReferendumHashProofHandler(
    private val soraProofSetterAccountId: String,
    private val deployHelper: DeployHelper,
    private val irohaConsumer: IrohaConsumer,
    private val credentials: Credentials
) {
    private val gson = GsonInstance.get()

    init {
        logger.info { "Referendum: Initialization of ReferendumHashProofHandler soraProofAccountId=$soraProofSetterAccountId" }
    }

    fun proceedBlock(block: BlockOuterClass.Block) {
        block.blockV1.payload.transactionsList
            .filter { tx ->
                tx.payload.reducedPayload.creatorAccountId == soraProofSetterAccountId
            }
            .forEach { tx ->
                processEligibleCommands(tx.payload.reducedPayload.commandsList)
                    .forEach { proofContext ->
                        val accountId = proofContext.accountId
                        val hash = proofContext.hash
                        logger.info { "Referendum: Referendum proof event account=$accountId, hash=$hash" }
                        // write proof
                        val key = credentials.address
                        val proof = createProof(hash)
                        ModelUtil.setAccountDetail(irohaConsumer, accountId, key, proof).get()
                    }
            }
    }

    private fun processEligibleCommands(commands: List<Commands.Command>): List<ReferendumProofContext> {
        return commands
            .map { command ->
                when {
                    command.hasCompareAndSetAccountDetail() -> processCompareAndSetAccountDetailCommand(
                        command.compareAndSetAccountDetail
                    )
                    command.hasSetAccountDetail() -> processSetAccountDetailCommand(
                        command.setAccountDetail
                    )
                    else -> null
                }

            }
            .mapNotNull {
                it
            }
    }

    private fun processCompareAndSetAccountDetailCommand(command: Commands.CompareAndSetAccountDetail): ReferendumProofContext? {
        return if (command.key == PROOF_MERKLE_ROOT_KEY && command.accountId.endsWith(SORA_PROOF_DOMAIN))
            ReferendumProofContext(
                command.accountId,
                command.value
            ) else
            null
    }

    private fun processSetAccountDetailCommand(command: Commands.SetAccountDetail): ReferendumProofContext? {
        return if (command.key == PROOF_MERKLE_ROOT_KEY && command.accountId.endsWith(SORA_PROOF_DOMAIN))
            ReferendumProofContext(
                command.accountId,
                command.value
            ) else
            null
    }

    private fun createProof(proof: String): String {
        val signatureString = deployHelper.signUserData(hashToProve(proof))
        val signature = extractVRSSignature(signatureString)

        val referendumHashProof = ReferendumHashProof(
            proof,
            signature
        )
        return gson.toJson(referendumHashProof).irohaEscape()
    }

    /**
     * Logger
     */
    companion object : KLogging() {
        const val PROOF_MERKLE_ROOT_KEY = "root"
    }
}

data class ReferendumProofContext(val accountId: String, val hash: String)

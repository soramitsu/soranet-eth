/*
 * Copyright Soramitsu Co., Ltd. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package jp.co.soramitsu.soranet.eth.bridge

import com.d3.commons.expansion.ServiceExpansion
import iroha.protocol.BlockOuterClass
import jp.co.soramitsu.soranet.eth.config.EthereumConfig
import jp.co.soramitsu.soranet.eth.config.EthereumPasswords
import jp.co.soramitsu.soranet.eth.sidechain.util.DeployHelper
import org.web3j.utils.Numeric

/**
 * Withdrawal service expansion strategy
 */
class EthereumBridgeEthExpansionStrategy(
    private val ethereumConfig: EthereumConfig,
    private val ethereumPasswords: EthereumPasswords,
    private val ethMasterAddress: String,
    private val expansionService: ServiceExpansion,
    private val proofCollector: ProofCollector
) {
    /**
     * Filter block for expansion trigger event and perform expansion logic:
     * - query proofs for expansion from all notaries
     * - send expansion transaction to Ethereum
     * @param block - iroha block
     */
    fun filterAndExpand(block: BlockOuterClass.Block) {
        expansionService.expand(block) { expansionDetails, triggerTxHash, _ ->
            val ethereumPeerAddress = expansionDetails.additionalData["eth_address"]!!
            val addPeerProof = proofCollector.collectProofForAddPeer(
                ethereumPeerAddress,
                triggerTxHash
            ).get()

            val masterContract = DeployHelper(
                ethereumConfig,
                ethereumPasswords
            ).loadMasterContract(ethMasterAddress)

            masterContract.addPeerByPeer(
                ethereumPeerAddress,
                Numeric.hexStringToByteArray(triggerTxHash),
                addPeerProof.v,
                addPeerProof.r,
                addPeerProof.s
            ).send()
        }
    }
}

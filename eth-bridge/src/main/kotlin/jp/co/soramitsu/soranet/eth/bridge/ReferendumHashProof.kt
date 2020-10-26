/*
 * Copyright Soramitsu Co., Ltd. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package jp.co.soramitsu.soranet.eth.bridge

import jp.co.soramitsu.soranet.eth.sidechain.util.VRSSignature

/**
 * Represents Ethereum hash proof from one node
 */
data class ReferendumHashProof(
    // Hash or merkle root of the data to prove
    val hash: String,

    // ethereum notary signature
    val signature: VRSSignature
)

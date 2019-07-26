/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.eth.registration.relay

import com.d3.commons.config.EthereumConfig
import com.d3.commons.config.IrohaConfig
import com.d3.commons.config.IrohaCredentialRawConfig

/**
 * Interface represents configs for relay registration service for cfg4k
 */
interface RelayRegistrationConfig {

    /** Number of accounts to deploy */
    val number: Int

    /** How often run registration of new relays in seconds */
    val replenishmentPeriod: Long

    /** Address of iroha account that stores address of master smart contract in Ethereum */
    val ethMasterAddressStorageAccountId: String

    /** Address of iroha account that sets address of master smart contract in Ethereum */
    val ethMasterAddressWriterAccountId: String

    /** Address of iroha account that stores address of implementation of Relay contract in Ethereum */
    val ethRelayImplementationAddressStorageAccountId: String

    /** Address of iroha account that sets address of implementation of Relay contract in Ethereum */
    val ethRelayImplementationAddressWriterAccountId: String

    /** Notary Iroha account that stores relay register */
    val notaryIrohaAccount: String

    val relayRegistrationCredential: IrohaCredentialRawConfig

    /** Iroha configurations */
    val iroha: IrohaConfig

    /** Ethereum configurations */
    val ethereum: EthereumConfig
}

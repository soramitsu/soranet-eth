/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.eth.abi

import com.google.gson.*
import java.lang.reflect.Type
import javax.xml.bind.DatatypeConverter

internal object AbiGsonHelper {
    val customGson: Gson by lazy {
        GsonBuilder().registerTypeHierarchyAdapter(
            ByteArray::class.java,
            ByteArrayToEthHexTypeAdapter()
        ).create()
    }

    private class ByteArrayToEthHexTypeAdapter : JsonSerializer<ByteArray>, JsonDeserializer<ByteArray> {
        @Throws(JsonParseException::class)
        override fun deserialize(
            json: JsonElement,
            typeOfT: Type,
            context: JsonDeserializationContext
        ): ByteArray {
            return DatatypeConverter.parseHexBinary(removePrefixIfNeeded(json.asString))
        }

        override fun serialize(
            src: ByteArray,
            typeOfSrc: Type,
            context: JsonSerializationContext
        ): JsonElement {
            return JsonPrimitive(addPrefixIfNeeded(DatatypeConverter.printHexBinary(src)))
        }

        private fun addPrefixIfNeeded(address: String): String {
            if (address.length == ETH_TRIMMED_ADDRESS_LENGTH) {
                return "$ETH_PREFIX$address"
            }
            return address
        }

        private fun removePrefixIfNeeded(address: String): String {
            if (address.length == ETH_ADDRESS_LENGTH) {
                return address.removePrefix(ETH_PREFIX)
            }
            return address
        }
    }

    private const val ETH_TRIMMED_ADDRESS_LENGTH = 40
    private const val ETH_ADDRESS_LENGTH = ETH_TRIMMED_ADDRESS_LENGTH + 2
    const val ETH_PREFIX = "0x"
}

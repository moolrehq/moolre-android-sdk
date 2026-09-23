package com.moolre.sdk.model

import java.util.UUID

/**
 * Generates collision-resistant external references for payment attempts.
 */
object MoolreReferenceGenerator {
    @JvmStatic
    fun generate(): String = "moolre-${UUID.randomUUID()}"
}

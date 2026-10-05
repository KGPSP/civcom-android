/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2024, 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.tests.konsist

import com.google.common.truth.Truth.assertThat
import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.verify.assertTrue
import org.junit.Test

class KonsistLicenseTest {
    private val civcomOwnedFiles = setOf(
        "features/location/impl/src/main/kotlin/io/element/android/features/location/impl/DisabledLocationNode.kt",
        "libraries/push/impl/src/test/kotlin/io/element/android/libraries/push/impl/notifications/CivcomNotificationDisplayerTest.kt",
    )
    private val civcomLicense = """
        Copyright (c) 2026 KG PSP.
         *
         * SPDX-License-Identifier: AGPL-3.0-only.
         * Please see LICENSE files in the repository root for full details.
        """.trimIndent()

    private val publicLicense = """
        /\*
        (?:.*\n)* \* Copyright \(c\) 20\d\d((, |-)20\d\d)? Element Creations Ltd\.
        (?:.*\n)* \*
         \* SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial\.
         \* Please see LICENSE files in the repository root for full details\.
         \*/
        """.trimIndent().toRegex()

    @Test
    fun `assert that FOSS files have the correct license header`() {
        Konsist
            .scopeFromProject()
            .files
            .filter {
                it.moduleName.startsWith("enterprise").not() &&
                    it.moduleName != "libraries/rustls-tls" &&
                    it.nameWithExtension != "locales.kt" &&
                    it.name.startsWith("Template ").not()
            }
            .also {
                assertThat(it).isNotEmpty()
            }
            .assertTrue {
                if (civcomOwnedFiles.any { path -> it.path.endsWith(path) }) {
                    it.text.contains(civcomLicense)
                } else {
                    publicLicense.containsMatchIn(it.text)
                }
            }
    }

    @Test
    fun `assert that files do not have double license header`() {
        Konsist
            .scopeFromProject()
            .files
            .filter {
                it.moduleName.endsWith("rustls-tls").not() &&
                it.nameWithExtension != "locales.kt" &&
                it.nameWithExtension != "KonsistLicenseTest.kt" &&
                    it.name.startsWith("Template ").not()
            }
            .assertTrue {
                if (civcomOwnedFiles.any { path -> it.path.endsWith(path) }) {
                    it.text.count("KG PSP.") == 1 && it.text.count("Element Creations Ltd.") == 0
                } else {
                    it.text.count("Element Creations Ltd.") == 1
                }
            }
    }
}

private fun String.count(subString: String): Int {
    var count = 0
    var index = 0
    while (true) {
        index = indexOf(subString, index)
        if (index == -1) return count
        count++
        index += subString.length
    }
}

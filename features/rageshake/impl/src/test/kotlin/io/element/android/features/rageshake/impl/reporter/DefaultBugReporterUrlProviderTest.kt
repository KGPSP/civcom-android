/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.rageshake.impl.reporter

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DefaultBugReporterUrlProviderTest {
    @Test
    fun `provide completes with a disabled URL`() = runTest {
        DefaultBugReporterUrlProvider().provide().test {
            assertThat(awaitItem()).isNull()
            awaitComplete()
        }
    }

    @Test
    fun `real feature availability hides report controls`() = runTest {
        io.element.android.features.rageshake.impl.DefaultRageshakeFeatureAvailability(DefaultBugReporterUrlProvider()).isAvailable().test {
            assertThat(awaitItem()).isFalse()
            awaitComplete()
        }
    }
}

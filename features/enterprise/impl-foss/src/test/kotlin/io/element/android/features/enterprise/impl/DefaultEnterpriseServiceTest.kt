/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2024, 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.enterprise.impl

import androidx.compose.ui.graphics.Color
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.element.android.features.enterprise.api.BugReportUrl
import io.element.android.libraries.core.extensions.runCatchingExceptions
import io.element.android.libraries.matrix.test.A_HOMESERVER_URL
import io.element.android.libraries.matrix.test.A_SESSION_ID
import io.element.android.libraries.matrix.test.FakeTemporaryMatrixClient
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DefaultEnterpriseServiceTest {
    @Test
    fun `only official server and API accepted without credential or path tricks`() = runTest {
        val service = createDefaultEnterpriseService()
        listOf("soia.info", "https://soia.info", "https://soia.info/", "https://matrix.soia.info").forEach {
            assertThat(service.isAllowedToConnectToHomeserver(it)).isTrue()
        }
        listOf(
            "http://soia.info",
            "https://soia.info.evil",
            "https://soia.info@evil",
            "https://matrix.org",
            "https://soia.info/login",
            "https://soia.info:444",
            "https://auth.soia.info"
        ).forEach {
            assertThat(service.isAllowedToConnectToHomeserver(it)).isFalse()
        }
    }

    @Test
    fun `foreign OIDC authorization endpoints are rejected before user credentials`() = runTest {
        val service = createDefaultEnterpriseService()
        val client = FakeTemporaryMatrixClient()
        assertThat(service.tweakMasUrl("https://auth.soia.info/", client)).isEqualTo("https://auth.soia.info/")
        assertThat(runCatchingExceptions { service.tweakMasUrl("https://foreign.invalid/", client) }.isFailure).isTrue()
    }

    @Test
    fun homeserverWhitelist() {
        val defaultEnterpriseService = createDefaultEnterpriseService()
        assertThat(defaultEnterpriseService.homeserverAllowList()).containsExactly("soia.info")
    }

    @Test
    fun `foreign account providers are rejected`() = runTest {
        val defaultEnterpriseService = createDefaultEnterpriseService()
        assertThat(defaultEnterpriseService.isAllowedToConnectToHomeserver(A_HOMESERVER_URL)).isFalse()
    }

    @Test
    fun `isEnterpriseUser always return false`() = runTest {
        val defaultEnterpriseService = createDefaultEnterpriseService()
        assertThat(defaultEnterpriseService.isEnterpriseUser(A_SESSION_ID)).isFalse()
    }

    @Test
    fun `semanticColorsFlow always emits the same value`() = runTest {
        val defaultEnterpriseService = createDefaultEnterpriseService()
        defaultEnterpriseService.semanticColorsFlow(null).test {
            val initialState = awaitItem()
            assertThat(initialState.light.bgActionPrimaryRest).isEqualTo(Color(0xFFC01F2A))
            assertThat(initialState.dark.bgActionPrimaryRest).isEqualTo(Color(0xFFD22730))
            awaitComplete()
        }
    }

    @Test
    fun `brandColorsFlow always emits null`() = runTest {
        val defaultEnterpriseService = createDefaultEnterpriseService()
        defaultEnterpriseService.brandColorsFlow(null).test {
            val initialState = awaitItem()
            assertThat(initialState).isNull()
            awaitComplete()
        }
    }

    @Test
    fun `semanticColorsFlow always emits the same value for a session`() = runTest {
        val defaultEnterpriseService = createDefaultEnterpriseService()
        defaultEnterpriseService.semanticColorsFlow(A_SESSION_ID).test {
            val initialState = awaitItem()
            assertThat(initialState.light.bgActionPrimaryRest).isEqualTo(Color(0xFFC01F2A))
            assertThat(initialState.dark.bgActionPrimaryRest).isEqualTo(Color(0xFFD22730))
            awaitComplete()
        }
    }

    @Test
    fun `overrideBrandColor has no effect`() = runTest {
        val defaultEnterpriseService = createDefaultEnterpriseService()
        defaultEnterpriseService.overrideBrandColor(A_SESSION_ID, "aColor")
    }

    @Test
    fun `firebasePushGateway returns null`() = runTest {
        val defaultEnterpriseService = createDefaultEnterpriseService()
        assertThat(defaultEnterpriseService.firebasePushGateway()).isNull()
    }

    @Test
    fun `unifiedPushDefaultPushGateway returns null`() = runTest {
        val defaultEnterpriseService = createDefaultEnterpriseService()
        assertThat(defaultEnterpriseService.unifiedPushDefaultPushGateway()).isNull()
    }

    @Test
    fun `bugReportUrlFlow only emits Disabled`() = runTest {
        val defaultEnterpriseService = createDefaultEnterpriseService()
        defaultEnterpriseService.bugReportUrlFlow(A_SESSION_ID).test {
            assertThat(awaitItem()).isEqualTo(BugReportUrl.Disabled)
            awaitComplete()
        }
    }

    @Test
    fun `getNoisyNotificationChannelId returns null`() = runTest {
        val defaultEnterpriseService = createDefaultEnterpriseService()
        assertThat(defaultEnterpriseService.getNoisyNotificationChannelId(A_SESSION_ID)).isNull()
    }

    @Test
    fun `CIVCOM has no Element Pro requirement`() = runTest {
        assertThat(createDefaultEnterpriseService().isElementProEnforced("soia.info")).isFalse()
    }

    private fun createDefaultEnterpriseService() = DefaultEnterpriseService()
}

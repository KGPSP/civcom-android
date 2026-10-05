/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2024, 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.matrix.impl

import com.google.common.truth.Truth.assertThat
import io.element.android.libraries.featureflag.test.FakeFeatureFlagService
import io.element.android.libraries.matrix.api.core.SessionId
import io.element.android.libraries.matrix.impl.auth.FakeProxyProvider
import io.element.android.libraries.matrix.impl.fixtures.fakes.FakeFfiClient
import io.element.android.libraries.matrix.impl.fixtures.fakes.FakeFfiClientBuilder
import io.element.android.libraries.matrix.impl.room.FakeTimelineEventFilterFactory
import io.element.android.libraries.matrix.impl.storage.FakeSqliteStoreBuilderProvider
import io.element.android.libraries.network.useragent.SimpleUserAgentProvider
import io.element.android.libraries.sessionstorage.api.SessionStore
import io.element.android.libraries.sessionstorage.test.InMemorySessionStore
import io.element.android.libraries.sessionstorage.test.aSessionData
import io.element.android.libraries.workmanager.api.WorkManagerRequestBuilder
import io.element.android.libraries.workmanager.test.FakeWorkManagerScheduler
import io.element.android.services.analytics.test.FakeAnalyticsService
import io.element.android.services.toolbox.test.systemclock.FakeSystemClock
import io.element.android.tests.testutils.lambda.lambdaRecorder
import io.element.android.tests.testutils.testCoroutineDispatchers
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.File

class RustMatrixClientFactoryTest {
    @Test
    fun `official restored aliases reach builder and SDK session as fixed API`() = runTest {
        listOf("soia.info", "https://soia.info/", "https://matrix.soia.info").forEach { alias ->
            val client = FakeFfiClient(withUtdHook = {})
            val builder = FakeFfiClientBuilder(buildResult = { client })
            val sut = createRustMatrixClientFactory(
                clientBuilderProvider = FakeClientBuilderProvider { builder },
                workManagerScheduler = FakeWorkManagerScheduler(submitLambda = {}),
            )
            val data = aSessionData(sessionId = "@alice:soia.info").copy(homeserverUrl = alias)
            val restored = sut.create(data)
            assertThat(builder.lastHomeserverUrl).isEqualTo("https://matrix.soia.info")
            assertThat(client.restoredSession?.homeserverUrl).isEqualTo("https://matrix.soia.info")
            assertThat(client.restoredSession?.accessToken).isEqualTo(data.accessToken)
            restored.destroy()
        }
    }

    @Test
    fun `foreign restored URLs and Matrix identities never obtain an SDK builder`() = runTest {
        val official = aSessionData(sessionId = "@alice:soia.info").copy(homeserverUrl = "https://matrix.soia.info")
        listOf(
            official.copy(homeserverUrl = "https://matrix.org"),
            official.copy(homeserverUrl = "https://matrix.soia.info/foreign"),
            official.copy(userId = "@alice:matrix.org"),
            official.copy(userId = "@alice:soia.info.attacker.invalid"),
            official.copy(userId = "alice:soia.info"),
        ).forEach { data ->
            var builderCalls = 0
            val sut = createRustMatrixClientFactory(
                clientBuilderProvider = FakeClientBuilderProvider {
                    builderCalls++
                    FakeFfiClientBuilder()
                },
                workManagerScheduler = FakeWorkManagerScheduler(submitLambda = {}),
            )
            var rejected = false
            try {
                sut.create(data)
            } catch (_: IllegalArgumentException) {
                rejected = true
            }
            assertThat(rejected).isTrue()
            assertThat(builderCalls).isEqualTo(0)
        }
    }

    @Test
    fun test() = runTest {
        val scheduleVacuumLambda = lambdaRecorder<WorkManagerRequestBuilder, Unit> {}
        val workManagerScheduler = FakeWorkManagerScheduler(submitLambda = scheduleVacuumLambda)
        val sut = createRustMatrixClientFactory(workManagerScheduler = workManagerScheduler)

        val result = sut.create(aSessionData(sessionId = "@alice:soia.info").copy(homeserverUrl = "https://matrix.soia.info"))

        assertThat(result.sessionId).isEqualTo(SessionId("@alice:soia.info"))
        scheduleVacuumLambda.assertions().isCalledOnce()
        result.destroy()
    }
}

fun TestScope.createRustMatrixClientFactory(
    cacheDirectory: File = File("/cache"),
    sessionStore: SessionStore = InMemorySessionStore(
        updateUserProfileResult = { _, _, _ -> },
    ),
    clientBuilderProvider: ClientBuilderProvider = FakeClientBuilderProvider(),
    workManagerScheduler: FakeWorkManagerScheduler = FakeWorkManagerScheduler(),
) = RustMatrixClientFactory(
    cacheDirectory = cacheDirectory,
    appCoroutineScope = backgroundScope,
    coroutineDispatchers = testCoroutineDispatchers(),
    sessionStore = sessionStore,
    userAgentProvider = SimpleUserAgentProvider(),
    proxyProvider = FakeProxyProvider(),
    clock = FakeSystemClock(),
    analyticsService = FakeAnalyticsService(),
    featureFlagService = FakeFeatureFlagService(),
    timelineEventFilterFactory = FakeTimelineEventFilterFactory(),
    clientBuilderProvider = clientBuilderProvider,
    sqliteStoreBuilderProvider = FakeSqliteStoreBuilderProvider(),
    workManagerScheduler = workManagerScheduler,
    clientBuilderEnterpriseHook = { builder, _ -> builder },
)

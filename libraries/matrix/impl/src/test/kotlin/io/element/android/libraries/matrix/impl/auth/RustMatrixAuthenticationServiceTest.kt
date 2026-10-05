/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2024, 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.matrix.impl.auth

import com.google.common.truth.Truth.assertThat
import io.element.android.features.enterprise.api.EnterpriseService
import io.element.android.features.enterprise.test.FakeEnterpriseService
import io.element.android.libraries.featureflag.test.FakeFeatureFlagService
import io.element.android.libraries.matrix.api.auth.OAuthPrompt
import io.element.android.libraries.matrix.api.core.SessionId
import io.element.android.libraries.matrix.impl.ClientBuilderProvider
import io.element.android.libraries.matrix.impl.FakeClientBuilderProvider
import io.element.android.libraries.matrix.impl.auth.qrlogin.SdkQrCodeLoginData
import io.element.android.libraries.matrix.impl.createRustMatrixClientFactory
import io.element.android.libraries.matrix.impl.fixtures.fakes.FakeFfiClient
import io.element.android.libraries.matrix.impl.fixtures.fakes.FakeFfiClientBuilder
import io.element.android.libraries.matrix.impl.fixtures.fakes.FakeFfiHomeserverLoginDetails
import io.element.android.libraries.matrix.impl.fixtures.fakes.FakeFfiQrCodeData
import io.element.android.libraries.matrix.impl.paths.SessionPathsFactory
import io.element.android.libraries.matrix.test.auth.FakeOAuthRedirectUrlProvider
import io.element.android.libraries.matrix.test.core.aBuildMeta
import io.element.android.libraries.sessionstorage.api.SessionStore
import io.element.android.libraries.sessionstorage.test.InMemorySessionStore
import io.element.android.libraries.sessionstorage.test.aSessionData
import io.element.android.libraries.workmanager.test.FakeWorkManagerScheduler
import io.element.android.tests.testutils.lambda.lambdaRecorder
import io.element.android.tests.testutils.testCoroutineDispatchers
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Test
import uniffi.matrix_sdk.NoHandle
import uniffi.matrix_sdk.OAuthAuthorizationData
import java.io.File

class RustMatrixAuthenticationServiceTest {
    @Test
    fun `SDK authorization URL is admitted only for the official auth host before enterprise hook`() = runTest {
        listOf(
            "https://auth.soia.info/oauth2/auth?state=fixture" to "https://auth.soia.info/oauth2/auth?state=fixture",
            "https://foreign.invalid/oauth2/auth" to "https://foreign.invalid/oauth2/auth",
            "https://auth.soia.info/oauth2/auth?state=fixture" to "https://foreign.invalid/oauth2/auth",
        ).forEach { (url, hookUrl) ->
            val authorization = object : OAuthAuthorizationData(NoHandle) {
                override fun loginUrl(): String = url
            }
            val client = FakeFfiClient(
                homeserverLoginDetailsResult = { FakeFfiHomeserverLoginDetails() },
                urlForOauthResult = { authorization },
            )
            var hookCalls = 0
            val service = createRustMatrixAuthenticationService(
                clientBuilderProvider = FakeClientBuilderProvider { FakeFfiClientBuilder { client } },
                enterpriseService = FakeEnterpriseService(tweakMasUrlResult = { _, _ ->
                    hookCalls++
                    hookUrl
                }),
            )
            assertThat(service.setHomeserver("soia.info").isSuccess).isTrue()
            val official = url.startsWith("https://auth.soia.info/")
            assertThat(service.getOAuthUrl(OAuthPrompt.Login, null).isSuccess).isEqualTo(official && hookUrl.startsWith("https://auth.soia.info/"))
            assertThat(hookCalls).isEqualTo(if (official) 1 else 0)
        }
    }

    @Test
    fun `rejected restored sessions retain original stored data and never obtain a builder`() = runTest {
        val data = aSessionData(sessionId = "@alice:soia.info", isTokenValid = true).copy(homeserverUrl = "https://matrix.org")
        val store = InMemorySessionStore(initialList = listOf(data))
        var builderCalls = 0
        val service = createRustMatrixAuthenticationService(
            sessionStore = store,
            clientBuilderProvider = FakeClientBuilderProvider {
                builderCalls++
                FakeFfiClientBuilder()
            },
        )
        assertThat(service.restoreSession(SessionId(data.userId)).isFailure).isTrue()
        assertThat(builderCalls).isEqualTo(0)
        assertThat(store.getSession(data.userId)).isEqualTo(data)
    }

    @Test
    fun `disabled QR service rejects before reading metadata progress or building`() = runTest {
        var metadataCalls = 0
        var builderCalls = 0
        var progressCalls = 0
        val qrData = SdkQrCodeLoginData(FakeFfiQrCodeData(baseUrlResult = {
            metadataCalls++
            "https://matrix.org"
        }))
        val service = createRustMatrixAuthenticationService(clientBuilderProvider = FakeClientBuilderProvider {
            builderCalls++
            FakeFfiClientBuilder()
        })
        assertThat(service.loginWithQrCode(qrData) { progressCalls++ }.isFailure).isTrue()
        assertThat(metadataCalls).isEqualTo(0)
        assertThat(builderCalls).isEqualTo(0)
        assertThat(progressCalls).isEqualTo(0)
    }

    @Test
    fun `official logical name is remapped to the fixed API at the actual SDK builder boundary`() = runTest {
        val builder = FakeFfiClientBuilder(buildResult = {
            FakeFfiClient(homeserverLoginDetailsResult = { FakeFfiHomeserverLoginDetails() })
        })
        val service = createRustMatrixAuthenticationService(
            clientBuilderProvider = FakeClientBuilderProvider(provideResult = { builder }),
        )
        assertThat(service.setHomeserver("soia.info").isSuccess).isTrue()
        assertThat(builder.lastServerNameOrUrl).isEqualTo("https://matrix.soia.info")
    }

    @Test
    fun `foreign discovery never reaches the SDK endpoint setter`() = runTest {
        val builder = FakeFfiClientBuilder()
        val service = createRustMatrixAuthenticationService(
            clientBuilderProvider = FakeClientBuilderProvider(provideResult = { builder }),
        )
        assertThat(service.setHomeserver("matrix.org").isFailure).isTrue()
        assertThat(builder.lastServerNameOrUrl).isNull()
    }

    @Test
    fun `setHomeserver is successful`() = runTest {
        val sut = createRustMatrixAuthenticationService(
            clientBuilderProvider = FakeClientBuilderProvider(
                provideResult = {
                    FakeFfiClientBuilder(
                        buildResult = {
                            FakeFfiClient(
                                homeserverLoginDetailsResult = {
                                    FakeFfiHomeserverLoginDetails()
                                }
                            )
                        }
                    )
                }
            ),
        )
        assertThat(sut.setHomeserver("soia.info").isSuccess).isTrue()
    }

    @Test
    fun `setHomeserver can fail gracefully and clean up the temporary client`() = runTest {
        val closeResult = lambdaRecorder<Unit> {}
        val sut = createRustMatrixAuthenticationService(
            clientBuilderProvider = FakeClientBuilderProvider(
                provideResult = {
                    FakeFfiClientBuilder(
                        buildResult = {
                            FakeFfiClient(
                                homeserverLoginDetailsResult = {
                                    throw IllegalStateException("Failed to get homeserver login details")
                                },
                                closeResult = closeResult,
                            )
                        },
                    )
                },
            ),
        )
        assertThat(sut.setHomeserver("soia.info").isFailure).isTrue()
        closeResult.assertions().isCalledOnce()
    }

    private fun TestScope.createRustMatrixAuthenticationService(
        sessionStore: SessionStore = InMemorySessionStore(),
        clientBuilderProvider: ClientBuilderProvider = FakeClientBuilderProvider(),
        enterpriseService: EnterpriseService = FakeEnterpriseService(),
    ): RustMatrixAuthenticationService {
        val baseDirectory = File("/base")
        val cacheDirectory = File("/cache")
        val rustMatrixClientFactory = createRustMatrixClientFactory(
            cacheDirectory = cacheDirectory,
            sessionStore = sessionStore,
            clientBuilderProvider = clientBuilderProvider,
            workManagerScheduler = FakeWorkManagerScheduler(submitLambda = {}),
        )
        return RustMatrixAuthenticationService(
            sessionPathsFactory = SessionPathsFactory(baseDirectory, cacheDirectory),
            coroutineDispatchers = testCoroutineDispatchers(),
            sessionStore = sessionStore,
            rustMatrixClientFactory = rustMatrixClientFactory,
            secretGenerator = FakeSecretGenerator(),
            oAuthConfigurationProvider = OAuthConfigurationProvider(
                buildMeta = aBuildMeta(),
                oAuthRedirectUrlProvider = FakeOAuthRedirectUrlProvider(),
            ),
            enterpriseService = enterpriseService,
            featureFlagService = FakeFeatureFlagService(),
            clientEnterpriseHook = {},
        )
    }
}

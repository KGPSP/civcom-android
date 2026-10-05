/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2024, 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.call

import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import io.element.android.features.call.api.CallData
import io.element.android.features.call.impl.DefaultElementCallEntryPoint
import io.element.android.features.call.impl.notifications.CallNotificationData
import io.element.android.features.call.utils.FakeActiveCallManager
import io.element.android.libraries.matrix.test.AN_EVENT_ID
import io.element.android.libraries.matrix.test.A_ROOM_ID
import io.element.android.libraries.matrix.test.A_SESSION_ID
import io.element.android.libraries.matrix.test.A_USER_ID_2
import io.element.android.tests.testutils.lambda.lambdaRecorder
import io.element.android.tests.testutils.robolectric.RobolectricTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import kotlin.time.Duration.Companion.seconds

class DefaultElementCallEntryPointTest : RobolectricTest() {
    @Test
    fun `startCall - never starts an excluded call activity`() = runTest {
        val entryPoint = createEntryPoint()
        entryPoint.startCall(CallData(A_SESSION_ID, A_ROOM_ID, isAudioCall = false))

        assertThat(shadowOf(RuntimeEnvironment.getApplication()).nextStartedActivity).isNull()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `handleIncomingCall - never registers or initializes an excluded call`() = runTest {
        val registerIncomingCallLambda = lambdaRecorder<CallNotificationData, Unit> {}
        val activeCallManager = FakeActiveCallManager(registerIncomingCallResult = registerIncomingCallLambda)
        val entryPoint = createEntryPoint(activeCallManager = activeCallManager)

        entryPoint.handleIncomingCall(
            callData = CallData(A_SESSION_ID, A_ROOM_ID, isAudioCall = false),
            eventId = AN_EVENT_ID,
            senderId = A_USER_ID_2,
            roomName = "roomName",
            senderName = "senderName",
            avatarUrl = "avatarUrl",
            timestamp = 0,
            expirationTimestamp = 0,
            notificationChannelId = "notificationChannelId",
            textContent = "textContent",
        )

        advanceTimeBy(1.seconds)

        registerIncomingCallLambda.assertions().isNeverCalled()
    }

    private fun TestScope.createEntryPoint(
        activeCallManager: FakeActiveCallManager = FakeActiveCallManager(),
    ) = DefaultElementCallEntryPoint(
        context = InstrumentationRegistry.getInstrumentation().targetContext,
        activeCallManager = lazy { activeCallManager },
    )
}

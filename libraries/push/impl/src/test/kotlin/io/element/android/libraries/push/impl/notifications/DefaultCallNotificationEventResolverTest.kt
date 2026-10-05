/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.push.impl.notifications

import com.google.common.truth.Truth.assertThat
import io.element.android.libraries.matrix.api.notification.CallIntent
import io.element.android.libraries.matrix.api.notification.NotificationContent
import io.element.android.libraries.matrix.api.notification.RtcNotificationType
import io.element.android.libraries.matrix.test.A_SESSION_ID
import io.element.android.libraries.matrix.test.A_USER_ID_2
import io.element.android.libraries.matrix.test.FakeMatrixClient
import io.element.android.libraries.matrix.test.FakeMatrixClientProvider
import io.element.android.libraries.matrix.test.notification.aNotificationData
import io.element.android.services.appnavstate.test.FakeAppForegroundStateService
import io.element.android.services.toolbox.test.strings.FakeStringProvider
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DefaultCallNotificationEventResolverTest {
    @Test
    fun `CIVCOM rejects ring and notify events before restoring a client or activating sync`() = runTest {
        var restoredClient = false
        val foregroundState = FakeAppForegroundStateService()
        val resolver = DefaultCallNotificationEventResolver(
            stringProvider = FakeStringProvider(),
            appForegroundStateService = foregroundState,
            clientProvider = FakeMatrixClientProvider(getClient = {
                restoredClient = true
                Result.success(FakeMatrixClient())
            }),
        )
        listOf(RtcNotificationType.RING, RtcNotificationType.NOTIFY).forEach { type ->
            val data = aNotificationData(
                content = NotificationContent.MessageLike.RtcNotification(A_USER_ID_2, type, CallIntent.VIDEO, 1567)
            )
            val result = resolver.resolveEvent(A_SESSION_ID, data)
            assertThat(result.isFailure).isTrue()
        }
        assertThat(restoredClient).isFalse()
        assertThat(foregroundState.hasRingingCall.value).isFalse()
    }
}

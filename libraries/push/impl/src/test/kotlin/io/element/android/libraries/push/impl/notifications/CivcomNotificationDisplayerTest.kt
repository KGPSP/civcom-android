/*
 * Copyright (c) 2026 KG PSP.
 *
 * SPDX-License-Identifier: AGPL-3.0-only.
 * Please see LICENSE files in the repository root for full details.
 */
package io.element.android.libraries.push.impl.notifications

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.common.truth.Truth.assertThat
import io.element.android.tests.testutils.robolectric.RobolectricTest
import org.junit.Test
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

class CivcomNotificationDisplayerTest : RobolectricTest() {
    @Test
    fun `generic redaction preserves new-event and update alert decisions and group behavior`() {
        val context = RuntimeEnvironment.getApplication()
        shadowOf(context).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(NotificationChannel("messages", "messages", NotificationManager.IMPORTANCE_DEFAULT))
        val displayer = DefaultNotificationDisplayer(context, NotificationManagerCompat.from(context))
        listOf(false, true, false).forEach { updateOnly ->
            val source = NotificationCompat.Builder(context, "messages")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("private room")
                .setContentText("private message")
                .setOnlyAlertOnce(updateOnly)
                .setGroup("account")
                .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_CHILDREN)
                .build()
            assertThat(displayer.showNotification("same room", 42, source)).isTrue()
            val displayed = shadowOf(manager).allNotifications.single()
            assertThat(displayed.flags and Notification.FLAG_ONLY_ALERT_ONCE != 0).isEqualTo(updateOnly)
            assertThat(NotificationCompat.getGroupAlertBehavior(displayed)).isEqualTo(NotificationCompat.GROUP_ALERT_CHILDREN)
            assertThat(displayed.extras.getCharSequence(Notification.EXTRA_TITLE).toString()).isEqualTo("CIVCOM")
            assertThat(displayed.extras.getCharSequence(Notification.EXTRA_TEXT).toString()).isEqualTo("Nowa wiadomość")
        }
    }

    @Test
    fun `messages invitations and summaries never expose content at OS boundary`() {
        val context = RuntimeEnvironment.getApplication()
        shadowOf(context).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(NotificationChannel("messages", "messages", NotificationManager.IMPORTANCE_DEFAULT))
        val displayer = DefaultNotificationDisplayer(context, NotificationManagerCompat.from(context))
        val privateNotification = NotificationCompat.Builder(context, "messages")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("private room")
            .setContentText("private sender and message")
            .setTicker("private ticker")
            .setStyle(NotificationCompat.BigTextStyle().bigText("private body"))
            .addAction(android.R.drawable.ic_dialog_info, "private sender", null)
            .build()
        assertThat(displayer.showNotification("room", 1, privateNotification)).isTrue()
        val displayed = shadowOf(manager).allNotifications.single()
        assertThat(displayed.extras.getCharSequence(Notification.EXTRA_TITLE).toString()).isEqualTo("CIVCOM")
        assertThat(displayed.extras.getCharSequence(Notification.EXTRA_TEXT).toString()).isEqualTo("Nowa wiadomość")
        assertThat(displayed.extras.getCharSequence(Notification.EXTRA_BIG_TEXT)).isNull()
        assertThat(displayed.extras.getParcelableArray(Notification.EXTRA_MESSAGES)).isNull()
        assertThat(displayed.actions).isNull()
        assertThat(displayed.tickerText.toString()).doesNotContain("private")
    }
}

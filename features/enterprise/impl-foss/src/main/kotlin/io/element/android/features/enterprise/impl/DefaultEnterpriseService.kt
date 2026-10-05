/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2024, 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.enterprise.impl

import androidx.compose.ui.graphics.Color
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import io.element.android.compound.colors.SemanticColorsLightDark
import io.element.android.features.enterprise.api.BugReportUrl
import io.element.android.features.enterprise.api.EnterpriseService
import io.element.android.libraries.matrix.api.CivcomPolicy
import io.element.android.libraries.matrix.api.ClientUrlContentFetcher
import io.element.android.libraries.matrix.api.core.SessionId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

@ContributesBinding(AppScope::class)
class DefaultEnterpriseService : EnterpriseService {
    override suspend fun isEnterpriseUser(sessionId: SessionId) = false
    override suspend fun tweakMasUrl(url: String, urlContentFetcher: ClientUrlContentFetcher): String {
        require(CivcomPolicy.isTrustedOAuthEndpoint(url)) { "Unsupported CIVCOM authentication endpoint" }
        return url
    }
    override fun homeserverAllowList(): List<String> = listOf(CivcomPolicy.SERVER_NAME)
    override suspend fun isAllowedToConnectToHomeserver(homeserverUrl: String) = CivcomPolicy.isAllowedHomeserver(homeserverUrl)
    override suspend fun isElementProEnforced(serverName: String): Boolean = false

    override suspend fun overrideBrandColor(sessionId: SessionId?, brandColor: String?) = Unit

    override fun brandColorsFlow(sessionId: SessionId?): Flow<Color?> {
        return flowOf(null)
    }

    override fun semanticColorsFlow(sessionId: SessionId?): Flow<SemanticColorsLightDark> {
        return flowOf(
            SemanticColorsLightDark(
                light = SemanticColorsLightDark.default.light.copy(
                    bgAccentRest = Color(0xFF1D5FAE),
                    bgAccentHovered = Color(0xFF1D5FAE),
                    bgAccentPressed = Color(0xFF1D5FAE),
                    bgAccentSelected = Color(0xFF1D5FAE),
                    bgActionPrimaryHovered = Color(0xFFC01F2A),
                    bgActionPrimaryPressed = Color(0xFFC01F2A),
                    borderAccentPrimary = Color(0xFF1D5FAE),
                    bgActionPrimaryRest = Color(0xFFC01F2A),
                    bgCanvasDefault = Color(0xFFF7F9FB),
                    textPrimary = Color(0xFF14212F),
                    textSecondary = Color(0xFF5C6E82),
                    iconPrimary = Color(0xFF14212F),
                    iconSecondary = Color(0xFF5C6E82),
                    bgCanvasDefaultLevel1 = Color(0xFFEDF1F6),
                    bgSubtlePrimary = Color(0xFFEDF1F6),
                    bgSubtleSecondary = Color(0xFFEDF1F6),
                    borderInteractivePrimary = Color(0xFFC9D4E0),
                    separatorPrimary = Color(0xFFC9D4E0),
                    separatorSecondary = Color(0xFFC9D4E0),
                    borderFocused = Color(0xFF1D5FAE),
                    textLinkExternal = Color(0xFF1D5FAE),
                    textOnSolidPrimary = Color(0xFFFFFFFF),
                    iconOnSolidPrimary = Color(0xFFFFFFFF),
                    textSuccessPrimary = Color(0xFF22794B),
                    iconSuccessPrimary = Color(0xFF22794B),
                    textWarningPrimary = Color(0xFFD97A00),
                    iconWarningPrimary = Color(0xFFD97A00),
                    textActionAccent = Color(0xFF1D5FAE),
                    iconAccentPrimary = Color(0xFF1D5FAE),
                ),
                dark = SemanticColorsLightDark.default.dark.copy(
                    bgAccentRest = Color(0xFF4C8FD6),
                    bgAccentHovered = Color(0xFF4C8FD6),
                    bgAccentPressed = Color(0xFF4C8FD6),
                    bgAccentSelected = Color(0xFF4C8FD6),
                    bgActionPrimaryHovered = Color(0xFFD22730),
                    bgActionPrimaryPressed = Color(0xFFD22730),
                    borderAccentPrimary = Color(0xFF4C8FD6),
                    bgActionPrimaryRest = Color(0xFFD22730),
                    bgCanvasDefault = Color(0xFF0B1523),
                    textPrimary = Color(0xFFE8EEF5),
                    textSecondary = Color(0xFF8CA0B8),
                    iconPrimary = Color(0xFFE8EEF5),
                    iconSecondary = Color(0xFF8CA0B8),
                    bgCanvasDefaultLevel1 = Color(0xFF101D30),
                    bgSubtlePrimary = Color(0xFF101D30),
                    bgSubtleSecondary = Color(0xFF101D30),
                    borderInteractivePrimary = Color(0xFF223650),
                    separatorPrimary = Color(0xFF223650),
                    separatorSecondary = Color(0xFF223650),
                    borderFocused = Color(0xFF4C8FD6),
                    textLinkExternal = Color(0xFF4C8FD6),
                    textOnSolidPrimary = Color(0xFFFFFFFF),
                    iconOnSolidPrimary = Color(0xFFFFFFFF),
                    textSuccessPrimary = Color(0xFF2F9E63),
                    iconSuccessPrimary = Color(0xFF2F9E63),
                    textWarningPrimary = Color(0xFFF08A00),
                    iconWarningPrimary = Color(0xFFF08A00),
                    textActionAccent = Color(0xFF4C8FD6),
                    iconAccentPrimary = Color(0xFF4C8FD6),
                ),
            )
        )
    }

    override fun firebasePushGateway(): String? = null
    override fun unifiedPushDefaultPushGateway(): String? = null

    override fun bugReportUrlFlow(sessionId: SessionId?): Flow<BugReportUrl> {
        return flowOf(BugReportUrl.Disabled)
    }

    override fun getNoisyNotificationChannelId(sessionId: SessionId): String? = null
}

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
                    bgAccentRest = Color(0xFFC01F2A),
                    bgAccentHovered = Color(0xFFC01F2A),
                    bgAccentPressed = Color(0xFFC01F2A),
                    bgAccentSelected = Color(0xFFC01F2A),
                    bgActionPrimaryHovered = Color(0xFFC01F2A),
                    bgActionPrimaryPressed = Color(0xFFC01F2A),
                    borderAccentPrimary = Color(0xFFC01F2A),
                    bgActionPrimaryRest = Color(0xFFC01F2A),
                    bgCanvasDefault = Color(0xFFF7F9FB),
                    textPrimary = Color(0xFF14212F),
                    textActionAccent = Color(0xFFC01F2A),
                    iconAccentPrimary = Color(0xFF1D5FAE),
                ),
                dark = SemanticColorsLightDark.default.dark.copy(
                    bgAccentRest = Color(0xFFD22730),
                    bgAccentHovered = Color(0xFFD22730),
                    bgAccentPressed = Color(0xFFD22730),
                    bgAccentSelected = Color(0xFFD22730),
                    bgActionPrimaryHovered = Color(0xFFD22730),
                    bgActionPrimaryPressed = Color(0xFFD22730),
                    borderAccentPrimary = Color(0xFFD22730),
                    bgActionPrimaryRest = Color(0xFFD22730),
                    bgCanvasDefault = Color(0xFF121F33),
                    textPrimary = Color(0xFFE8EEF5),
                    textActionAccent = Color(0xFFD22730),
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

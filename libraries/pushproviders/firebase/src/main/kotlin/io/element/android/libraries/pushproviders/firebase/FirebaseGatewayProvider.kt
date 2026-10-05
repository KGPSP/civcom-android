/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.pushproviders.firebase

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding

interface FirebaseGatewayProvider {
    fun getFirebaseGateway(): String
}

@ContributesBinding(AppScope::class)
class DefaultFirebaseGatewayProvider : FirebaseGatewayProvider {
    override fun getFirebaseGateway(): String {
        return FirebaseConfig.PUSHER_HTTP_URL
    }
}

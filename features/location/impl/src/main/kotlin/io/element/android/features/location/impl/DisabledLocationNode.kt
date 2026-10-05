/*
 * Copyright (c) 2026 KG PSP.
 *
 * SPDX-License-Identifier: AGPL-3.0-only.
 * Please see LICENSE files in the repository root for full details.
 */
package io.element.android.features.location.impl

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.bumble.appyx.core.modality.BuildContext
import com.bumble.appyx.core.node.Node
import io.element.android.libraries.designsystem.components.dialogs.ErrorDialog

internal class DisabledLocationNode(buildContext: BuildContext) : Node(buildContext) {
    @Composable
    override fun View(modifier: Modifier) {
        ErrorDialog(
            title = stringResource(R.string.screen_location_civcom_disabled_title),
            content = stringResource(R.string.screen_location_civcom_disabled_message),
            onSubmit = ::navigateUp,
        )
    }
}

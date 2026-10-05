/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.login.impl.screens.onboarding

import android.graphics.Typeface
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.element.android.compound.theme.ElementTheme
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.features.login.impl.R
import io.element.android.features.login.impl.login.LoginModeEvent
import io.element.android.features.login.impl.login.LoginModeView
import io.element.android.libraries.architecture.AsyncData
import io.element.android.libraries.designsystem.atomic.atoms.ElementLogoAtom
import io.element.android.libraries.designsystem.atomic.atoms.ElementLogoAtomSize
import io.element.android.libraries.designsystem.atomic.molecules.ButtonColumnMolecule
import io.element.android.libraries.designsystem.atomic.pages.FlowStepPage
import io.element.android.libraries.designsystem.components.BigIcon
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.components.Button
import io.element.android.libraries.designsystem.theme.components.Icon
import io.element.android.libraries.designsystem.theme.components.IconButton
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.designsystem.theme.components.TextButton
import io.element.android.libraries.matrix.api.auth.OAuthDetails
import io.element.android.libraries.permissions.api.localnetwork.LocalNetworkPermissionDialogView
import io.element.android.libraries.testtags.TestTags
import io.element.android.libraries.testtags.testTag
import io.element.android.libraries.ui.strings.CommonStrings
import androidx.compose.material3.Button as NativeButton

/**
 * Ref: https://www.figma.com/design/pDlJZGBsri47FNTXMnEdXB/Compound-Android-Templates?node-id=41-6503
 */
@Composable
fun OnBoardingView(
    state: OnBoardingState,
    onBackClick: () -> Unit,
    onDeveloperSettingsClick: () -> Unit,
    onSignInWithQrCode: () -> Unit,
    onSignIn: (mustChooseAccountProvider: Boolean) -> Unit,
    onCreateAccount: () -> Unit,
    onOAuthDetails: (OAuthDetails) -> Unit,
    onNeedLoginPassword: () -> Unit,
    onLearnMoreClick: () -> Unit,
    onReportProblem: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val loginView = @Composable {
        LoginModeView(
            loginMode = state.loginModeState.loginMode,
            onClearError = {
                state.eventSink(OnBoardingEvent.ClearError)
            },
            onLearnMoreClick = onLearnMoreClick,
            onOAuthDetails = onOAuthDetails,
            onNeedLoginPassword = onNeedLoginPassword,
        )
        LocalNetworkPermissionDialogView(
            dialog = state.loginModeState.localNetworkPermissionDialog,
            onSubmit = {
                state.loginModeState.eventSink(LoginModeEvent.RequestLocalNetworkPermission)
            },
            onDismiss = {
                state.loginModeState.eventSink(LoginModeEvent.DismissLocalNetworkPermission)
            }
        )
    }
    val buttons = @Composable {
        OnBoardingButtons(
            state = state,
            onSignInWithQrCode = onSignInWithQrCode,
            onSignIn = onSignIn,
            onCreateAccount = onCreateAccount,
            onReportProblem = onReportProblem,
        )
    }

    if (state.isAddingAccount) {
        AddOtherAccountScaffold(
            modifier = modifier,
            loginView = loginView,
            buttons = buttons,
            onBackClick = onBackClick,
        )
    } else {
        AddFirstAccountScaffold(
            modifier = modifier,
            state = state,
            loginView = loginView,
            buttons = buttons,
            onBackClick = onBackClick,
            onDeveloperSettingsClick = onDeveloperSettingsClick,
        )
    }
}

@Composable
private fun AddFirstAccountScaffold(
    state: OnBoardingState,
    loginView: @Composable () -> Unit,
    buttons: @Composable () -> Unit,
    onBackClick: () -> Unit,
    onDeveloperSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().background(ElementTheme.colors.bgCanvasDefault)
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = CenterHorizontally,
    ) {
        if (state.showBackButton) {
            IconButton(onClick = onBackClick, modifier = Modifier.align(Alignment.End)) {
                Icon(imageVector = CompoundIcons.Close(), contentDescription = stringResource(CommonStrings.action_cancel))
            }
        }
        if (state.showDeveloperSettings) {
            IconButton(onClick = onDeveloperSettingsClick) {
                Icon(imageVector = CompoundIcons.SettingsSolid(), contentDescription = stringResource(CommonStrings.common_developer_options))
            }
        }
        OnBoardingContent(state.onBoardingLogoResId)
        Spacer(Modifier.height(24.dp))
        loginView()
        buttons()
    }
}

@Composable
private fun AddOtherAccountScaffold(
    loginView: @Composable () -> Unit,
    buttons: @Composable () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowStepPage(
        modifier = modifier,
        title = stringResource(CommonStrings.common_add_account),
        iconStyle = BigIcon.Style.Default(CompoundIcons.HomeSolid()),
        buttons = { buttons() },
        content = loginView,
        onBackClick = onBackClick,
    )
}

@Composable
private fun OnBoardingContent(logoResId: Int?) {
    val heading = ElementTheme.typography.fontHeadingLgBold.copy(
        fontFamily = FontFamily(Typeface.create("sans-serif-condensed", Typeface.BOLD)),
        fontWeight = FontWeight.Bold,
        letterSpacing = 2.sp,
    )
    Column(horizontalAlignment = CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        if (logoResId != null) {
            Image(painterResource(logoResId), contentDescription = stringResource(R.string.screen_onboarding_civcom_title), modifier = Modifier.size(120.dp))
        } else {
            ElementLogoAtom(size = ElementLogoAtomSize.Large)
        }
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.screen_onboarding_civcom_title), style = heading, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(R.string.screen_onboarding_civcom_signature),
            style = ElementTheme.typography.fontBodySmRegular.copy(fontFamily = heading.fontFamily, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp),
            color = ElementTheme.colors.textSecondary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.screen_onboarding_civcom_message), style = ElementTheme.typography.fontBodyLgRegular, textAlign = TextAlign.Center)
    }
}

@Composable
private fun CivcomButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    showProgress: Boolean = false,
    outlined: Boolean = false,
    qrIcon: Boolean = false,
) {
    NativeButton(
        onClick = onClick,
        enabled = enabled && !showProgress,
        modifier = modifier.heightIn(min = 48.dp),
        shape = RoundedCornerShape(4.dp),
        border = if (outlined) BorderStroke(1.dp, ElementTheme.colors.borderInteractivePrimary) else null,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (outlined) Color.Transparent else ElementTheme.colors.bgActionPrimaryRest,
            contentColor = if (outlined) ElementTheme.colors.textPrimary else Color.White,
        ),
    ) {
        if (showProgress) {
            io.element.android.libraries.designsystem.theme.components.CircularProgressIndicator(Modifier.size(20.dp), color = Color.White)
            Spacer(Modifier.size(8.dp))
        }
        if (qrIcon && !showProgress) {
            Icon(imageVector = CompoundIcons.QrCode(), contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.size(8.dp))
        }
        Text(text, style = ElementTheme.typography.fontBodyMdRegular.copy(fontWeight = FontWeight.SemiBold))
    }
}

@Composable
private fun OnBoardingButtons(
    state: OnBoardingState,
    onSignInWithQrCode: () -> Unit,
    onSignIn: (mustChooseAccountProvider: Boolean) -> Unit,
    onCreateAccount: () -> Unit,
    onReportProblem: () -> Unit,
) {
    val isLoading by remember(state.loginModeState.loginMode) {
        derivedStateOf {
            state.loginModeState.loginMode is AsyncData.Loading
        }
    }

    ButtonColumnMolecule {
        val signInButtonStringRes = if (state.canLoginWithQrCode || state.canCreateAccount) {
            R.string.screen_onboarding_sign_in_manually
        } else {
            CommonStrings.action_continue
        }
        if (state.canLoginWithQrCode) {
            CivcomButton(
                text = stringResource(id = R.string.screen_onboarding_sign_in_with_qr_code),
                outlined = true,
                qrIcon = true,
                onClick = onSignInWithQrCode,
                modifier = Modifier.fillMaxWidth()
            )
        }
        val defaultAccountProvider = state.defaultAccountProvider
        if (defaultAccountProvider == null) {
            CivcomButton(
                text = stringResource(id = signInButtonStringRes),
                onClick = {
                    onSignIn(state.mustChooseAccountProvider)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(TestTags.onBoardingSignIn)
            )
        } else {
            CivcomButton(
                text = stringResource(id = R.string.screen_onboarding_civcom_sign_in),
                showProgress = isLoading,
                onClick = {
                    state.eventSink(OnBoardingEvent.OnSignIn(defaultAccountProvider))
                },
                enabled = state.submitEnabled || isLoading,
                modifier = Modifier
                    .fillMaxWidth()
            )
        }
        if (state.canCreateAccount) {
            TextButton(
                text = stringResource(id = R.string.screen_onboarding_sign_up),
                onClick = onCreateAccount,
                modifier = Modifier
                    .fillMaxWidth()
            )
        }
        if (state.isAddingAccount.not()) {
            if (state.canReportBug) {
                // Add a report problem text button. Use a Text since we need a special theme here.
                Text(
                    modifier = Modifier
                        .clickable(onClick = onReportProblem)
                        .padding(16.dp),
                    text = stringResource(id = CommonStrings.common_report_a_problem),
                    style = ElementTheme.typography.fontBodySmRegular.copy(fontFamily = FontFamily.Monospace, fontFeatureSettings = "tnum"),
                    color = ElementTheme.colors.textSecondary,
                )
                Text(
                    stringResource(R.string.screen_onboarding_civcom_footer),
                    style = ElementTheme.typography.fontBodySmRegular.copy(fontFamily = FontFamily.Monospace),
                    color = ElementTheme.colors.textSecondary,
                    textAlign = TextAlign.Center,
                )
            } else {
                Text(
                    modifier = Modifier
                        .clickable(role = Role.Button) {
                            state.eventSink(OnBoardingEvent.OnVersionClick)
                        }
                        .padding(16.dp),
                    text = stringResource(id = R.string.screen_onboarding_app_version, state.version),
                    style = ElementTheme.typography.fontBodySmRegular.copy(fontFamily = FontFamily.Monospace, fontFeatureSettings = "tnum"),
                    color = ElementTheme.colors.textSecondary,
                )
                Text(
                    stringResource(R.string.screen_onboarding_civcom_footer),
                    style = ElementTheme.typography.fontBodySmRegular.copy(fontFamily = FontFamily.Monospace),
                    color = ElementTheme.colors.textSecondary,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@PreviewsDayNight
@Composable
internal fun OnBoardingViewPreview(
    @PreviewParameter(OnBoardingStatePreviewParam::class) state: OnBoardingState
) = ElementPreview {
    OnBoardingView(
        state = state,
        onBackClick = {},
        onDeveloperSettingsClick = {},
        onSignInWithQrCode = {},
        onSignIn = {},
        onCreateAccount = {},
        onReportProblem = {},
        onOAuthDetails = {},
        onNeedLoginPassword = {},
        onLearnMoreClick = {},
    )
}

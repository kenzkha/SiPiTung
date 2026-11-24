package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.auth.signOutFromGoogle
import com.example.data.ChildProfile
import com.example.ui.theme.PintarTheme
import com.example.viewmodel.MainTab
import com.example.viewmodel.PintarHitungViewModel
import com.example.viewmodel.UiState

private data class NavDestination(
    val tab: MainTab,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val AppDestinations = listOf(
    NavDestination(
        MainTab.BERANDA,
        R.string.nav_home,
        Icons.Rounded.Home,
        Icons.Outlined.Home,
        "nav_tab_beranda"
    ),
    NavDestination(
        MainTab.BELAJAR,
        R.string.nav_learn,
        Icons.Rounded.Calculate,
        Icons.Outlined.Calculate,
        "nav_tab_belajar"
    ),
    NavDestination(
        MainTab.MAIN,
        R.string.nav_play,
        Icons.Rounded.SportsEsports,
        Icons.Outlined.SportsEsports,
        "nav_tab_main"
    ),
    NavDestination(
        MainTab.ORANG_TUA,
        R.string.nav_parent,
        Icons.Rounded.Insights,
        Icons.Outlined.Insights,
        "nav_tab_orang_tua"
    )
)

@Composable
fun MainAppScaffold(
    viewModel: PintarHitungViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val selectedTopic by viewModel.selectedTopic.collectAsStateWithLifecycle()
    val profileState by viewModel.profileState.collectAsStateWithLifecycle()
    val sessionsState by viewModel.sessionsState.collectAsStateWithLifecycle()
    val rewardsState by viewModel.rewardsState.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val extended = PintarTheme.extended

    LaunchedEffect(snackbarMessage) {
        val msg = snackbarMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    BackHandler(enabled = selectedTab != MainTab.BERANDA) {
        viewModel.selectTab(MainTab.BERANDA)
    }

    val profile: ChildProfile? = (profileState as? UiState.Success)?.data
    val sessions = (sessionsState as? UiState.Success)?.data ?: emptyList()
    val rewards = (rewardsState as? UiState.Success)?.data ?: emptyList()

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (!useNavigationRail) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 4.dp
                    ) {
                        AppDestinations.forEach { dest ->
                            val selected = selectedTab == dest.tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { viewModel.selectTab(dest.tab) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = stringResource(dest.labelRes)
                                    )
                                },
                                label = {
                                    Text(
                                        text = stringResource(dest.labelRes),
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = extended.sunnyPillContent,
                                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                    indicatorColor = extended.sunnyPill,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavigationRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        AppDestinations.forEach { dest ->
                            val selected = selectedTab == dest.tab
                            NavigationRailItem(
                                selected = selected,
                                onClick = { viewModel.selectTab(dest.tab) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = stringResource(dest.labelRes)
                                    )
                                },
                                label = { Text(stringResource(dest.labelRes)) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = extended.sunnyPillContent,
                                    indicatorColor = extended.sunnyPill
                                ),
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    when {
                        profileState is UiState.Error -> {
                            val err = (profileState as UiState.Error).message
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "Gagal memuat data belajar",
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Text(
                                    text = err,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        profile == null -> {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(42.dp),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Menyiapkan ruang belajar…",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 12.dp)
                                )
                            }
                        }

                        else -> {
                            when (selectedTab) {
                                MainTab.BERANDA -> HomeScreen(
                                    profile = profile,
                                    rewards = rewards,
                                    onSelectGrade = viewModel::updateGrade,
                                    onStartWorksheet = viewModel::openWorksheetWithTopic,
                                    onOpenGames = { viewModel.selectTab(MainTab.MAIN) },
                                    onPurchaseOrEquipAvatar = viewModel::purchaseOrEquipAvatar,
                                    onRedeemParentReward = viewModel::redeemParentReward
                                )

                                MainTab.BELAJAR -> WorksheetScreen(
                                    profile = profile,
                                    selectedTopic = selectedTopic,
                                    onSelectTopic = viewModel::selectTopic,
                                    onSelectGrade = viewModel::updateGrade,
                                    onCompleteWorksheet = viewModel::recordSession
                                )

                                MainTab.MAIN -> GamesScreen(
                                    profile = profile,
                                    onSelectGrade = viewModel::updateGrade,
                                    onCompleteGame = viewModel::recordSession
                                )

                                MainTab.ORANG_TUA -> ParentReportScreen(
                                    profile = profile,
                                    sessions = sessions,
                                    rewards = rewards,
                                    onUpdateProfile = viewModel::updateProfileSettings,
                                    onCreateReward = viewModel::createParentReward,
                                    onFulfillReward = viewModel::fulfillParentReward,
                                    onDeleteReward = viewModel::deleteParentReward,
                                    onSignOut = {
                                        signOutFromGoogle(
                                            credentialManager = credentialManager,
                                            scope = scope
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

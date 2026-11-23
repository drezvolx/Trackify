package com.blankpages.trackify
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.blankpages.trackify.navigation.TrackifyTab
import com.blankpages.trackify.ui.TrackifyUiState
import com.blankpages.trackify.ui.TrackifyViewModel
import com.blankpages.trackify.ui.home.HomeScreen
import com.blankpages.trackify.ui.locations.LocationsScreen
import com.blankpages.trackify.ui.notes.NotesScreen
import com.blankpages.trackify.ui.settings.SettingsScreen
import com.blankpages.trackify.ui.spreadsheet.SpreadsheetScreen
import com.blankpages.trackify.ui.theme.TrackifyTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlin.math.roundToInt

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val vm: TrackifyViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val state by vm.uiState.collectAsState()

            LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
                vm.syncCurrentDate()
            }

            // Theme & System bar > Follow the device via SystemBarStyle.auto.
            TrackifyTheme {
                val stateHolder = rememberSaveableStateHolder()
                val scheme = MaterialTheme.colorScheme

                var tabOrdinal by rememberSaveable { mutableIntStateOf(0) }
                var inSettings by rememberSaveable { mutableStateOf(false) }
                val currentTab = TrackifyTab.of(tabOrdinal)

                BackHandler(inSettings) { inSettings = false }
                BackHandler(!inSettings && currentTab != TrackifyTab.HOME) { tabOrdinal = 0 }

                Scaffold(
                    containerColor = scheme.background,
                    // Insets > Honor status bars everywhere; skip bottom inset behind the bar.
                    contentWindowInsets = WindowInsets.statusBars,
                    bottomBar = {
                        if (!inSettings) {
                            ReferenceNotchedBottomBar(
                                currentTab = currentTab,
                                onSelectTab = { tabOrdinal = it.ordinal },
                            )
                        }
                    },
                ) { padding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(
                                start = padding.calculateStartPadding(LocalLayoutDirection.current),
                                top = padding.calculateTopPadding(),
                                end = padding.calculateEndPadding(LocalLayoutDirection.current),
                            ),
                    ) {
                        if (inSettings) {
                            SettingsScreen(
                                defaultRate = state.defaultRate,
                                defaultBonus = state.settings.defaultBonus,
                                targetAmount = state.settings.targetAmount,
                                dailyReminderEnabled = state.settings.dailyReminderEnabled,
                                onBack = { inSettings = false },
                                onUpdateRate = vm::updateDefaultRate,
                                onUpdateBonus = vm::updateDefaultBonus,
                                onUpdateTarget = vm::updateTargetAmount,
                                onToggleReminder = vm::setDailyReminder,
                                onExportCsv = vm::exportCsvForMonth,
                                onBackupData = vm::exportBackup,
                                onRestoreData = vm::importBackup,
                                onResetData = {
                                    vm.resetAllData { msg ->
                                        Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
                                    }
                                },
                            )
                        } else {
                            AnimatedContent(
                                targetState = currentTab,
                                transitionSpec = {
                                    EnterTransition.None togetherWith ExitTransition.None
                                },
                                label = "fast_tab_switch",
                            ) { tab ->
                                stateHolder.SaveableStateProvider(tab.name) {
                                    TabContent(
                                        tab = tab,
                                        state = state,
                                        onOpenSettings = { inSettings = true },
                                        onNavigateTab = { tabOrdinal = it.ordinal },
                                        vm = vm,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TabContent(
    tab: TrackifyTab,
    state: TrackifyUiState,
    onOpenSettings: () -> Unit,
    onNavigateTab: (TrackifyTab) -> Unit,
    vm: TrackifyViewModel,
) {
    when (tab) {
        TrackifyTab.HOME -> HomeScreen(
            todayLog = state.todayLog,
            monthlyLogs = state.currentMonthLogs,
            summary = state.currentMonthSummary,
            savedLocations = state.locations,
            targetAmount = state.settings.targetAmount,
            onOpenSettings = onOpenSettings,
            onOpenRekap = { onNavigateTab(TrackifyTab.REKAP) },
            onOpenLocations = { onNavigateTab(TrackifyTab.LOKASI) },
            onOpenNotes = { onNavigateTab(TrackifyTab.CATATAN) },
            onUpdateTodayLog = vm::upsertLog,
        )

        TrackifyTab.REKAP -> SpreadsheetScreen(
            todayLog = state.todayLog,
            yesterdayRemaining = state.yesterdayRemaining,
            defaultRate = state.defaultRate,
            selectedMonth = state.selectedMonth,
            monthlyLogs = state.monthlyLogs,
            summary = state.summary,
            targetAmount = state.settings.targetAmount,
            onUpdateTarget = vm::updateTargetAmount,
            onShiftMonth = vm::shiftMonth,
            onCarryoverYesterday = vm::carryoverYesterdayRemaining,
            onExportCsv = vm::exportMonthlyCsv,
            onUpdateLog = vm::upsertLog,
            onDeleteLog = vm::deleteLog,
        )

        TrackifyTab.LOKASI -> LocationsScreen(
            locations = state.locations,
            onSaveLocation = vm::saveLocation,
            onDeleteLocation = vm::deleteLocation,
            onQuickDelivered = vm::incrementTodayDelivered,
            onCreateLinkedNote = vm::upsertNote,
        )

        TrackifyTab.CATATAN -> NotesScreen(
            notes = state.notes,
            locations = state.locations,
            onSaveNote = { note, isNew -> vm.upsertNote(if (isNew) note.copy(id = 0) else note) },
            onDeleteNote = vm::deleteNote,
        )
    }
}

@Composable
private fun ReferenceNotchedBottomBar(currentTab: TrackifyTab, onSelectTab: (TrackifyTab) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val density = LocalDensity.current

    val animatedIndex by animateFloatAsState(
        targetValue = currentTab.ordinal.toFloat(),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "notched_tab_index",
    )

    val barHeight: Dp = 64.dp
    val topLift: Dp = 18.dp
    val orbSize: Dp = 46.dp
    val notchRadius: Dp = 30.dp

    val topLiftPx = remember(density, topLift) { with(density) { topLift.toPx() } }
    val totalHeightPx = remember(density, barHeight, topLift) { with(density) { (barHeight + topLift).toPx() } }
    val notchRadiusPx = remember(density, notchRadius) { with(density) { notchRadius.toPx() } }
    val cornerRadiusPx = remember(density) { with(density) { 18.dp.toPx() } }
    val strokeWidthPx = remember(density) { with(density) { 1.dp.toPx() } }
    val orbSizePx = remember(density, orbSize) { with(density) { orbSize.toPx() } }

    val baseRectPath = remember { Path() }
    val scoopPath = remember { Path() }
    val notchedBarPath = remember { Path() }
    val strokeSpec = remember(strokeWidthPx) { Stroke(width = strokeWidthPx) }

    var barSize by remember { mutableStateOf(IntSize.Zero) }

    // Bar fill > Solid enough to separate icons from content, not fully black.
    val barColor = scheme.background.copy(alpha = 0.72f)
    val borderColor = scheme.outlineVariant
    val primaryColor = scheme.primary
    val onPrimaryColor = scheme.onPrimary
    val outlineColor = scheme.outline
    val onSurfaceVariantColor = scheme.onSurfaceVariant

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .height(barHeight + topLift)
            .onSizeChanged { barSize = it }
            .drawBehind {
                val widthPx = size.width
                if (widthPx <= 0f) return@drawBehind
                val slotPx = widthPx / TrackifyTab.entries.size
                val centerXPx = (slotPx * animatedIndex) + (slotPx * 0.5f)

                baseRectPath.reset()
                baseRectPath.addRoundRect(
                    RoundRect(
                        rect = Rect(0f, topLiftPx, widthPx, totalHeightPx),
                        cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
                    ),
                )

                scoopPath.reset()
                scoopPath.addOval(
                    Rect(
                        left = centerXPx - notchRadiusPx,
                        top = topLiftPx - notchRadiusPx * 0.72f,
                        right = centerXPx + notchRadiusPx,
                        bottom = topLiftPx + notchRadiusPx * 1.28f,
                    ),
                )

                notchedBarPath.op(baseRectPath, scoopPath, PathOperation.Difference)
                drawPath(path = notchedBarPath, color = barColor)
                drawPath(path = notchedBarPath, color = borderColor, style = strokeSpec)
            },
    ) {
        Surface(
            modifier = Modifier
                .offset {
                    val widthPx = barSize.width.toFloat()
                    val slotPx = if (widthPx > 0f) widthPx / TrackifyTab.entries.size else 0f
                    val leftPx = (slotPx * animatedIndex) + (slotPx * 0.5f) - (orbSizePx * 0.5f)
                    IntOffset(x = leftPx.roundToInt(), y = 0)
                }
                .size(orbSize),
            shape = CircleShape,
            color = primaryColor,
            border = BorderStroke(2.dp, outlineColor),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = currentTab.icon,
                    contentDescription = currentTab.label,
                    tint = onPrimaryColor,
                    modifier = Modifier.size(21.dp),
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight)
                .align(Alignment.BottomCenter),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TrackifyTab.entries.forEach { tab ->
                val isSelected = tab == currentTab
                val interactionSource = remember { MutableInteractionSource() }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = { onSelectTab(tab) },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (!isSelected) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.label,
                            tint = onSurfaceVariantColor,
                            modifier = Modifier.size(21.dp),
                        )
                    }
                }
            }
        }
    }
}

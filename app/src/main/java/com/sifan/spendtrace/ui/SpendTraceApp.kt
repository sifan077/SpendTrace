package com.sifan.spendtrace.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sifan.spendtrace.R
import com.sifan.spendtrace.data.backup.BackupService
import com.sifan.spendtrace.ui.components.ExpenseEditorSheet
import com.sifan.spendtrace.ui.home.EditorEvent
import com.sifan.spendtrace.ui.home.HomeScreen
import com.sifan.spendtrace.ui.home.SpendTraceViewModel
import com.sifan.spendtrace.ui.settings.SettingsScreen
import com.sifan.spendtrace.ui.stats.StatsScreen
import kotlinx.coroutines.launch

private const val EDITOR_CLOSED = -1L
private const val EDITOR_NEW = 0L

private const val TAB_HOME = 0
private const val TAB_STATS = 1
private const val TAB_SETTINGS = 2

/** 应用主界面：账单 / 统计 / 设置三个页签 + 记账弹窗。 */
@Composable
fun SpendTraceApp(
    viewModel: SpendTraceViewModel,
    backupService: BackupService,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val lastCategoryId by viewModel.lastCategoryId.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val totalCount by viewModel.totalCount.collectAsStateWithLifecycle()

    val resources = LocalResources.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var selectedTab by rememberSaveable { mutableIntStateOf(TAB_HOME) }
    // 记录当前正在编辑的记录 id：-1 表示关闭，0 表示新增，>0 表示编辑某条记录
    var editorKey by rememberSaveable { mutableStateOf(EDITOR_CLOSED) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                EditorEvent.Saved, EditorEvent.Deleted -> editorKey = EDITOR_CLOSED
                is EditorEvent.SaveFailed -> snackbarHostState.showSnackbar(
                    resources.getString(R.string.save_failed, event.detail)
                )

                is EditorEvent.DeleteFailed -> snackbarHostState.showSnackbar(
                    resources.getString(R.string.delete_failed, event.detail)
                )
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Column {
                HorizontalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 0.dp,
                ) {
                    BottomTab(
                        selected = selectedTab == TAB_HOME,
                        iconRes = R.drawable.ic_tab_list,
                        label = stringResource(R.string.tab_home),
                        onClick = { selectedTab = TAB_HOME },
                    )
                    BottomTab(
                        selected = selectedTab == TAB_STATS,
                        iconRes = R.drawable.ic_tab_stats,
                        label = stringResource(R.string.tab_stats),
                        onClick = { selectedTab = TAB_STATS },
                    )
                    BottomTab(
                        selected = selectedTab == TAB_SETTINGS,
                        iconRes = R.drawable.ic_tab_settings,
                        label = stringResource(R.string.tab_settings),
                        onClick = { selectedTab = TAB_SETTINGS },
                    )
                }
            }
        },
        floatingActionButton = {
            if (selectedTab == TAB_HOME) {
                ExtendedFloatingActionButton(
                    onClick = { editorKey = EDITOR_NEW },
                    shape = RoundedCornerShape(18.dp),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = null,
                        )
                    },
                    text = {
                        Text(
                            text = stringResource(R.string.add_expense),
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                        )
                    },
                )
            }
        },
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedTab) {
                TAB_HOME -> HomeScreen(
                    uiState = uiState,
                    onSettingsClick = { selectedTab = TAB_SETTINGS },
                    onPreviousMonth = viewModel::selectPreviousMonth,
                    onNextMonth = viewModel::selectNextMonth,
                    onBackToCurrentMonth = viewModel::goToCurrentMonth,
                    onExpenseClick = { expense -> editorKey = expense.id },
                    onDeleteExpense = viewModel::deleteExpense,
                    onRetry = viewModel::retry,
                )

                TAB_STATS -> StatsScreen(
                    uiState = uiState,
                    onPreviousMonth = viewModel::selectPreviousMonth,
                    onNextMonth = viewModel::selectNextMonth,
                    onBackToCurrentMonth = viewModel::goToCurrentMonth,
                    onRetry = viewModel::retry,
                )

                else -> SettingsScreen(
                    themeMode = themeMode,
                    onThemeModeChange = viewModel::setThemeMode,
                    categories = categories,
                    totalCount = totalCount,
                    backupService = backupService,
                    onShowMessage = { message ->
                        snackbarHostState.currentSnackbarData?.dismiss()
                        scope.launch { snackbarHostState.showSnackbar(message) }
                    },
                )
            }
        }
    }

    val editingExpense = remember(uiState.groups, editorKey) {
        if (editorKey > 0L) {
            uiState.groups.asSequence()
                .flatMap { it.expenses.asSequence() }
                .firstOrNull { it.id == editorKey }
        } else {
            null
        }
    }

    if (editorKey == EDITOR_NEW || editingExpense != null) {
        key(editorKey) {
            ExpenseEditorSheet(
                expense = editingExpense,
                categories = categories,
                defaultCategoryId = lastCategoryId,
                isSaving = isSaving,
                onDismiss = { editorKey = EDITOR_CLOSED },
                onSave = viewModel::saveExpense,
                onDelete = viewModel::deleteExpense,
            )
        }
    }
}

@Composable
private fun RowScope.BottomTab(
    selected: Boolean,
    iconRes: Int,
    label: String,
    onClick: () -> Unit,
) {
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.padding(bottom = 1.dp),
            )
        },
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            )
        },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = MaterialTheme.colorScheme.primary,
            selectedTextColor = MaterialTheme.colorScheme.primary,
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
            indicatorColor = Color.Transparent,
        ),
    )
}

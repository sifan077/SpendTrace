package com.sifan.spendtrace.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sifan.spendtrace.AppInfo
import com.sifan.spendtrace.R
import com.sifan.spendtrace.core.DateFormats
import com.sifan.spendtrace.core.Money
import com.sifan.spendtrace.data.ThemeMode
import com.sifan.spendtrace.data.backup.BackupContents
import com.sifan.spendtrace.data.backup.BackupException
import com.sifan.spendtrace.data.backup.BackupFiles
import com.sifan.spendtrace.data.backup.BackupPreview
import com.sifan.spendtrace.data.backup.BackupService
import com.sifan.spendtrace.data.model.Category
import com.sifan.spendtrace.ui.components.CategoryIcon
import com.sifan.spendtrace.ui.components.SectionTitle
import java.time.Instant
import kotlinx.coroutines.launch

private const val BACKUP_FILE_PREFIX = "spendtrace-backup-"

/** 校验通过、等待用户确认的导入内容。 */
private data class PendingImport(
    val fileName: String,
    val contents: BackupContents,
)

/** 一次性错误弹窗。 */
private data class ErrorInfo(
    val title: String,
    val message: String,
)

/**
 * 设置页。
 *
 * 只放真实可用的能力：主题模式、分类一览、本地备份（导出 / 导入）与关于信息。
 */
@Composable
fun SettingsScreen(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    categories: List<Category>,
    totalCount: Int,
    backupService: BackupService,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val version = stringResource(R.string.version_name)
    val context = LocalContext.current
    // 用 LocalResources 取运行时文案，语言或字号变化后仍能拿到最新资源
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()

    var workingLabel by remember { mutableStateOf<String?>(null) }
    var pendingExport by remember { mutableStateOf<String?>(null) }
    var pendingImport by remember { mutableStateOf<PendingImport?>(null) }
    var errorInfo by remember { mutableStateOf<ErrorInfo?>(null) }

    val isWorking = workingLabel != null

    // 导出：先选保存位置，拿到 Uri 后再写入内容
    val createDocumentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(BackupService.MIME_TYPE)
    ) { uri ->
        val text = pendingExport
        pendingExport = null
        if (uri == null || text == null) return@rememberLauncherForActivityResult
        scope.launch {
            workingLabel = resources.getString(R.string.backup_exporting)
            try {
                BackupFiles.write(context, uri, text)
                onShowMessage(resources.getString(R.string.backup_export_done))
            } catch (error: BackupException) {
                errorInfo = ErrorInfo(
                    title = resources.getString(R.string.backup_export_failed_title),
                    message = error.message ?: resources.getString(R.string.backup_export_failed_message),
                )
            } catch (error: Exception) {
                errorInfo = ErrorInfo(
                    title = resources.getString(R.string.backup_export_failed_title),
                    message = resources.getString(R.string.backup_export_failed_message),
                )
            } finally {
                workingLabel = null
            }
        }
    }

    // 导入：先选文件并完整校验，确认后才写入数据库
    val openDocumentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            workingLabel = resources.getString(R.string.backup_validating)
            try {
                val bytes = BackupFiles.read(context, uri)
                val contents = backupService.inspect(bytes)
                pendingImport = PendingImport(
                    fileName = BackupFiles.displayName(context, uri) ?: uri.lastPathSegment.orEmpty(),
                    contents = contents,
                )
            } catch (error: BackupException) {
                errorInfo = ErrorInfo(
                    title = resources.getString(R.string.backup_import_failed_title),
                    message = error.message ?: resources.getString(R.string.backup_import_failed_message),
                )
            } catch (error: Exception) {
                errorInfo = ErrorInfo(
                    title = resources.getString(R.string.backup_import_failed_title),
                    message = resources.getString(R.string.backup_import_failed_message),
                )
            } finally {
                workingLabel = null
            }
        }
    }

    val startExport: () -> Unit = {
        if (!isWorking) {
            scope.launch {
                workingLabel = resources.getString(R.string.backup_exporting)
                val text = try {
                    backupService.export()
                } catch (error: Exception) {
                    errorInfo = ErrorInfo(
                        title = resources.getString(R.string.backup_export_failed_title),
                        message = resources.getString(R.string.backup_export_failed_message),
                    )
                    null
                }
                workingLabel = null
                if (text != null) {
                    pendingExport = text
                    try {
                        createDocumentLauncher.launch(
                            "$BACKUP_FILE_PREFIX${DateFormats.fileTimestamp(Instant.now())}" +
                                ".${BackupService.FILE_EXTENSION}"
                        )
                    } catch (error: Exception) {
                        pendingExport = null
                        errorInfo = ErrorInfo(
                            title = resources.getString(R.string.backup_pick_failed_title),
                            message = resources.getString(R.string.backup_pick_failed_message),
                        )
                    }
                }
            }
        }
    }

    val startImport: () -> Unit = {
        if (!isWorking) {
            try {
                // 备份文件在不同文件管理器里的 MIME 类型并不统一，这里不做类型过滤
                openDocumentLauncher.launch(arrayOf("*/*"))
            } catch (error: Exception) {
                errorInfo = ErrorInfo(
                    title = resources.getString(R.string.backup_pick_failed_title),
                    message = resources.getString(R.string.backup_pick_failed_message),
                )
            }
        }
    }

    val confirmImport: (PendingImport) -> Unit = { pending ->
        pendingImport = null
        scope.launch {
            workingLabel = resources.getString(R.string.backup_restoring)
            try {
                backupService.restore(pending.contents)
                onShowMessage(
                    resources.getString(
                        R.string.backup_restored,
                        pending.contents.preview.expenseCount,
                        pending.contents.preview.categoryCount,
                    )
                )
            } catch (error: BackupException) {
                errorInfo = ErrorInfo(
                    title = resources.getString(R.string.backup_import_failed_title),
                    message = error.message ?: resources.getString(R.string.backup_import_failed_message),
                )
            } catch (error: Exception) {
                errorInfo = ErrorInfo(
                    title = resources.getString(R.string.backup_import_failed_title),
                    message = resources.getString(R.string.backup_import_failed_message),
                )
            } finally {
                workingLabel = null
            }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        if (isWorking) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 21.dp, end = 21.dp, top = 20.dp, bottom = 40.dp),
        ) {
            item(key = "header") {
                Text(
                    text = stringResource(R.string.settings_section),
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontSize = 31.sp,
                        letterSpacing = (-0.8).sp,
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    text = stringResource(R.string.settings_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (workingLabel != null) {
                    Spacer(Modifier.height(14.dp))
                    WorkingBanner(label = workingLabel.orEmpty())
                }
                Spacer(Modifier.height(26.dp))
            }

            item(key = "appearance") {
                SettingsSection(
                    title = stringResource(R.string.settings_group_appearance),
                    subtitle = stringResource(R.string.settings_group_appearance_desc),
                ) {
                    ThemeSelector(selected = themeMode, onSelect = onThemeModeChange)
                }
                Spacer(Modifier.height(26.dp))
            }

            item(key = "categories") {
                SettingsSection(
                    title = stringResource(R.string.settings_group_category),
                    subtitle = stringResource(R.string.settings_group_category_desc),
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        categories.chunked(4).forEach { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                row.forEach { category ->
                                    CategoryCell(category = category, modifier = Modifier.weight(1f))
                                }
                                repeat(4 - row.size) {
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                }
                Spacer(Modifier.height(26.dp))
            }

            item(key = "backup") {
                SettingsSection(
                    title = stringResource(R.string.settings_group_backup),
                    subtitle = stringResource(R.string.settings_group_backup_desc),
                ) {
                    OfflineNote()
                    TileDivider(indent = 14.dp)
                    SettingsTile(
                        iconRes = R.drawable.ic_settings_export,
                        title = stringResource(R.string.settings_export),
                        subtitle = stringResource(R.string.settings_export_desc, totalCount),
                        enabled = !isWorking,
                        onClick = startExport,
                    )
                    TileDivider()
                    SettingsTile(
                        iconRes = R.drawable.ic_settings_import,
                        title = stringResource(R.string.settings_import),
                        subtitle = stringResource(R.string.settings_import_desc),
                        enabled = !isWorking,
                        onClick = startImport,
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.settings_backup_hint),
                    style = MaterialTheme.typography.labelSmall.copy(lineHeight = 17.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(26.dp))
            }

            item(key = "about") {
                SettingsSection(
                    title = stringResource(R.string.settings_group_about),
                    subtitle = stringResource(R.string.settings_group_about_desc),
                ) {
                    BrandHeaderRow()
                    TileDivider()
                    SettingsTile(
                        iconRes = R.drawable.ic_settings_info,
                        title = stringResource(R.string.settings_version),
                        subtitle = version,
                        enabled = false,
                    )
                    TileDivider()
                    SettingsTile(
                        iconRes = R.drawable.ic_settings_database,
                        title = stringResource(R.string.settings_storage),
                        subtitle = stringResource(R.string.settings_storage_value),
                        enabled = false,
                    )
                    TileDivider()
                    SettingsTile(
                        iconRes = R.drawable.ic_settings_person,
                        title = stringResource(R.string.settings_author),
                        subtitle = AppInfo.AUTHOR,
                        onClick = { openUrl(context, AppInfo.AUTHOR_URL) },
                    )
                    TileDivider()
                    SettingsTile(
                        iconRes = R.drawable.ic_settings_repository,
                        title = stringResource(R.string.settings_repository),
                        subtitle = AppInfo.REPOSITORY_DISPLAY,
                        onClick = { openUrl(context, AppInfo.REPOSITORY_URL) },
                    )
                }
                Spacer(Modifier.height(18.dp))
                Text(
                    text = stringResource(
                        R.string.settings_copyright,
                        AppInfo.COPYRIGHT_YEAR,
                        AppInfo.AUTHOR,
                    ),
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.4.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    pendingImport?.let { pending ->
        ImportConfirmDialog(
            fileName = pending.fileName,
            preview = pending.contents.preview,
            onConfirm = { confirmImport(pending) },
            onDismiss = { pendingImport = null },
        )
    }

    errorInfo?.let { info ->
        ErrorDialog(
            title = info.title,
            message = info.message,
            onDismiss = { errorInfo = null },
        )
    }
}

/** 分组标题 + 副标题 + 卡片，与设置页其余分组保持一致。 */
@Composable
private fun SettingsSection(
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    SectionTitle(text = title)
    Spacer(Modifier.height(3.dp))
    Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(12.dp))
    SettingsCard(content = content)
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(22.dp),
            ),
        content = content,
    )
}

/** 卡片内的分隔线；默认缩进到图标右侧，与文案左边缘对齐。 */
@Composable
private fun TileDivider(indent: Dp = 68.dp) {
    HorizontalDivider(
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.outlineVariant,
        modifier = Modifier.padding(start = indent),
    )
}

@Composable
private fun SettingsTile(
    @DrawableRes iconRes: Int,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
) {
    val scheme = MaterialTheme.colorScheme
    val clickable = if (enabled) onClick else null
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (clickable != null) Modifier.clickable(onClick = clickable) else Modifier)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(scheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = if (enabled) {
                    scheme.onSurfaceVariant
                } else {
                    scheme.onSurfaceVariant.copy(alpha = 0.55f)
                },
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = if (enabled) {
                    scheme.onSurface
                } else {
                    scheme.onSurface.copy(alpha = 0.72f)
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (clickable != null) {
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = scheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/** 主题选择：三选一的分段控件。 */
@Composable
private fun ThemeSelector(selected: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(scheme.surfaceContainerHigh)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        ThemeMode.entries.forEach { mode ->
            val isSelected = mode == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(11.dp))
                    .background(if (isSelected) scheme.primary else Color.Transparent)
                    .clickable { onSelect(mode) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(mode.labelRes()),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    ),
                    color = if (isSelected) scheme.onPrimary else scheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun CategoryCell(category: Category, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CategoryIcon(category = category, size = 36.dp)
        Spacer(Modifier.height(6.dp))
        Text(
            text = category.name,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

/** 「数据只在本机」的安全说明，放在备份卡片顶部。 */
@Composable
private fun OfflineNote() {
    Row(
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_settings_info),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(9.dp))
        Text(
            text = stringResource(R.string.settings_offline_note),
            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** 关于卡片顶部的品牌信息。 */
@Composable
private fun BrandHeaderRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.secondary,
                        )
                    )
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_sparkle),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(24.dp),
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 17.sp,
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.ExtraBold,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = stringResource(R.string.settings_brand_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun WorkingBanner(label: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(15.dp),
            strokeWidth = 2.dp,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.width(9.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

/** 导入前的确认弹窗：展示备份内容并强调覆盖风险。 */
@Composable
private fun ImportConfirmDialog(
    fileName: String,
    preview: BackupPreview,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.backup_confirm_title)) },
        text = {
            Column {
                Text(
                    text = fileName,
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(14.dp))
                PreviewRow(
                    label = stringResource(R.string.backup_confirm_time),
                    value = DateFormats.dateTimeLabel(preview.exportedAt),
                )
                PreviewRow(
                    label = stringResource(R.string.backup_confirm_contents),
                    value = stringResource(
                        R.string.backup_confirm_contents_value,
                        preview.expenseCount,
                        preview.categoryCount,
                    ),
                )
                PreviewRow(
                    label = stringResource(R.string.backup_confirm_total),
                    value = Money.formatWithSymbol(preview.totalAmountMinor),
                )
                if (!preview.appVersion.isNullOrBlank()) {
                    PreviewRow(
                        label = stringResource(R.string.backup_confirm_version),
                        value = preview.appVersion,
                    )
                }
                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(scheme.errorContainer.copy(alpha = 0.45f))
                        .padding(12.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = null,
                        tint = scheme.error,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.backup_confirm_warning),
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onErrorContainer,
                    )
                }
            }
        },
        confirmButton = {
            FilledTonalButton(onClick = onConfirm) {
                Text(text = stringResource(R.string.backup_confirm_action))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.cancel))
            }
        },
    )
}

@Composable
private fun PreviewRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(72.dp),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun ErrorDialog(title: String, message: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = { Text(text = message) },
        confirmButton = {
            FilledTonalButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.dialog_ok))
            }
        },
    )
}

/** 打开外部链接；设备没有浏览器时静默忽略，不影响使用。 */
private fun openUrl(context: Context, url: String) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }
}

private fun ThemeMode.labelRes(): Int = when (this) {
    ThemeMode.SYSTEM -> R.string.settings_theme_system
    ThemeMode.LIGHT -> R.string.settings_theme_light
    ThemeMode.DARK -> R.string.settings_theme_dark
}

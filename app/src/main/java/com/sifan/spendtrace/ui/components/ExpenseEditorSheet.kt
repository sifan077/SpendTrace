package com.sifan.spendtrace.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sifan.spendtrace.R
import com.sifan.spendtrace.core.DateFormats
import com.sifan.spendtrace.core.Money
import com.sifan.spendtrace.data.model.Category
import com.sifan.spendtrace.data.model.Expense
import com.sifan.spendtrace.data.model.ExpenseDraft
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneOffset

private const val MAX_NOTE_LENGTH = 60

/**
 * 添加 / 编辑支出的底部弹窗。
 *
 * [expense] 为 null 时表示新增，否则为编辑；表单状态在弹窗内部维护，
 * 保存与删除交给上层 ViewModel 处理。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseEditorSheet(
    expense: Expense?,
    categories: List<Category>,
    defaultCategoryId: Long?,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (ExpenseDraft) -> Unit,
    onDelete: (Long) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scrollState = rememberScrollState()
    val editorKey = expense?.id ?: 0L

    var amountText by rememberSaveable(editorKey) {
        mutableStateOf(expense?.let { Money.formatPlain(it.amountMinor) } ?: "")
    }
    var noteText by rememberSaveable(editorKey) { mutableStateOf(expense?.note.orEmpty()) }
    var selectedCategoryId by rememberSaveable(editorKey) {
        mutableLongStateOf(expense?.category?.id ?: defaultCategoryId ?: categories.firstOrNull()?.id ?: 1L)
    }
    var spentAtMillis by rememberSaveable(editorKey) {
        mutableLongStateOf((expense?.spentAt ?: Instant.now()).toEpochMilli())
    }
    var showAmountError by rememberSaveable(editorKey) { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // 出现校验错误时内容会变高，滚动到底部保证「保存」按钮不被键盘遮挡
    LaunchedEffect(showAmountError) {
        if (showAmountError) {
            withFrameNanos { }
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    // 分类列表异步加载完成后再兜底选中
    LaunchedEffect(categories, editorKey) {
        if (expense == null && categories.isNotEmpty() && categories.none { it.id == selectedCategoryId }) {
            val fallback = defaultCategoryId?.takeIf { id -> categories.any { it.id == id } }
                ?: categories.first().id
            selectedCategoryId = fallback
        }
    }

    // 校验并保存；表单「保存」与内置键盘的保存键共用此逻辑
    fun submitDraft() {
        val amountMinor = Money.parseToMinor(amountText)
        if (amountMinor == null) {
            showAmountError = true
            return
        }
        onSave(
            ExpenseDraft(
                id = expense?.id,
                amountMinor = amountMinor,
                categoryId = selectedCategoryId,
                note = noteText.trim().takeIf { it.isNotEmpty() },
                spentAt = Instant.ofEpochMilli(spentAtMillis),
            )
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.outline) },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(scrollState)
                .padding(horizontal = 23.dp)
                .padding(bottom = 24.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(
                        if (expense == null) R.string.add_expense else R.string.edit_expense
                    ),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.cancel),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            AmountInput(
                value = amountText,
                isError = showAmountError,
            )

            if (showAmountError) {
                Text(
                    text = stringResource(R.string.amount_error),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                )
            }

            Spacer(Modifier.height(18.dp))

            Text(
                text = stringResource(R.string.category_label),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(10.dp))
            CategoryGrid(
                categories = categories,
                selectedCategoryId = selectedCategoryId,
                onSelect = { selectedCategoryId = it },
            )

            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FieldButton(
                    label = stringResource(R.string.date_label),
                    value = DateFormats.fullDateLabel(
                        DateFormats.toLocalDate(Instant.ofEpochMilli(spentAtMillis))
                    ),
                    onClick = { showDatePicker = true },
                    modifier = Modifier.weight(1.2f),
                )
                FieldButton(
                    label = stringResource(R.string.time_label),
                    value = DateFormats.timeLabel(Instant.ofEpochMilli(spentAtMillis)),
                    onClick = { showTimePicker = true },
                    modifier = Modifier.weight(0.8f),
                )
            }

            Spacer(Modifier.height(10.dp))

            TextField(
                value = noteText,
                onValueChange = { noteText = it.take(MAX_NOTE_LENGTH) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = stringResource(R.string.note_placeholder),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                ),
            )

            Spacer(Modifier.height(14.dp))

            AmountKeypad(
                isSaving = isSaving,
                onDigit = { digit ->
                    val next = if (digit == "." && amountText.isEmpty()) "0." else amountText + digit
                    amountText = Money.sanitizeInput(next)
                    if (showAmountError) showAmountError = false
                },
                onBackspace = {
                    amountText = amountText.dropLast(1)
                    if (showAmountError) showAmountError = false
                },
                onClear = {
                    amountText = ""
                    if (showAmountError) showAmountError = false
                },
                onSave = { submitDraft() },
            )

            if (expense != null) {
                TextButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(R.string.delete_expense),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }

    if (showDatePicker) {
        val currentDate = DateFormats.toLocalDate(Instant.ofEpochMilli(spentAtMillis))
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = currentDate
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val picked = datePickerState.selectedDateMillis
                        if (picked != null) {
                            val pickedDate = Instant.ofEpochMilli(picked)
                                .atZone(ZoneOffset.UTC)
                                .toLocalDate()
                            val time = DateFormats.toLocalTime(Instant.ofEpochMilli(spentAtMillis))
                            spentAtMillis = DateFormats.toInstant(pickedDate, time).toEpochMilli()
                        }
                        showDatePicker = false
                    }
                ) {
                    Text(text = stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(text = stringResource(R.string.cancel))
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val currentTime = DateFormats.toLocalTime(Instant.ofEpochMilli(spentAtMillis))
        val timePickerState = rememberTimePickerState(
            initialHour = currentTime.hour,
            initialMinute = currentTime.minute,
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text(text = stringResource(R.string.time_label)) },
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val date = DateFormats.toLocalDate(Instant.ofEpochMilli(spentAtMillis))
                        val time = LocalTime.of(timePickerState.hour, timePickerState.minute)
                        spentAtMillis = DateFormats.toInstant(date, time).toEpochMilli()
                        showTimePicker = false
                    }
                ) {
                    Text(text = stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text(text = stringResource(R.string.cancel))
                }
            },
        )
    }

    if (showDeleteConfirm && expense != null) {
        DeleteExpenseDialog(
            expense = expense,
            onConfirm = {
                showDeleteConfirm = false
                onDelete(expense.id)
            },
            onDismiss = { showDeleteConfirm = false },
        )
    }
}

/**
 * 居中的大号金额展示，金额由弹窗底部的内置数字键盘输入，
 * 不再聚焦 TextField，也就不会唤起系统输入法。
 */
@Composable
private fun AmountInput(
    value: String,
    isError: Boolean,
) {
    val color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    // 左对齐：容器宽度本就贴合文字，居中会让空态占位符显得偏移
    val amountStyle = TextStyle(
        fontWeight = FontWeight.ExtraBold,
        fontSize = 39.sp,
        color = color,
        textAlign = TextAlign.Start,
        letterSpacing = (-1).sp,
    )
    val placeholder = stringResource(R.string.amount_placeholder)
    val placeholderStyle = TextStyle(
        fontWeight = FontWeight.ExtraBold,
        fontSize = 39.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
    )
    // 按实际文字宽度决定展示区宽度，保证「¥」与数字始终紧贴并整体居中。
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val fieldWidth = remember(value, amountStyle, placeholderStyle, placeholder) {
        val style = if (value.isEmpty()) placeholderStyle else amountStyle
        val display = value.ifEmpty { placeholder }
        val measured = measurer.measure(
            text = AnnotatedString(display),
            style = style,
            maxLines = 1,
        )
        with(density) { measured.size.width.toDp() }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "¥",
            style = TextStyle(
                fontWeight = FontWeight.Bold,
                fontSize = 26.sp,
                color = color,
            ),
        )
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier.width(fieldWidth),
            contentAlignment = Alignment.CenterStart,
        ) {
            if (value.isEmpty()) {
                Text(text = placeholder, style = placeholderStyle)
            } else {
                Text(text = value, style = amountStyle)
            }
        }
    }
}

/**
 * 内置数字键盘：免去唤起系统输入法，直接完成金额输入与保存。
 *
 * 布局为 4 行 4 列，最后一行的「保存」占据半行宽度，与表单保存逻辑一致。
 */
@Composable
private fun AmountKeypad(
    isSaving: Boolean,
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    onSave: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KeypadKey("7", onClick = { onDigit("7") }, modifier = Modifier.weight(1f))
            KeypadKey("8", onClick = { onDigit("8") }, modifier = Modifier.weight(1f))
            KeypadKey("9", onClick = { onDigit("9") }, modifier = Modifier.weight(1f))
            KeypadKey(
                "⌫",
                onClick = onBackspace,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KeypadKey("4", onClick = { onDigit("4") }, modifier = Modifier.weight(1f))
            KeypadKey("5", onClick = { onDigit("5") }, modifier = Modifier.weight(1f))
            KeypadKey("6", onClick = { onDigit("6") }, modifier = Modifier.weight(1f))
            KeypadKey(
                "C",
                onClick = onClear,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KeypadKey("1", onClick = { onDigit("1") }, modifier = Modifier.weight(1f))
            KeypadKey("2", onClick = { onDigit("2") }, modifier = Modifier.weight(1f))
            KeypadKey("3", onClick = { onDigit("3") }, modifier = Modifier.weight(1f))
            KeypadKey(".", onClick = { onDigit(".") }, modifier = Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KeypadKey("0", onClick = { onDigit("0") }, modifier = Modifier.weight(2f))
            KeypadKey(
                label = stringResource(R.string.save_expense),
                onClick = onSave,
                enabled = !isSaving,
                container = MaterialTheme.colorScheme.primary,
                tint = MaterialTheme.colorScheme.onPrimary,
                labelStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.weight(2f),
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
        }
    }
}

/** 数字键盘的单个按键。 */
@Composable
private fun KeypadKey(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    container: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    labelStyle: TextStyle = MaterialTheme.typography.titleLarge.copy(
        fontWeight = FontWeight.SemiBold,
    ),
    content: @Composable () -> Unit = {},
) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = modifier
            .height(52.dp)
            .clip(shape)
            .background(container)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = label, style = labelStyle, color = tint, maxLines = 1)
        content()
    }
}

/** 4 列分类选择格。 */
@Composable
private fun CategoryGrid(
    categories: List<Category>,
    selectedCategoryId: Long,
    onSelect: (Long) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        categories.chunked(4).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { category ->
                    CategoryTile(
                        category = category,
                        selected = category.id == selectedCategoryId,
                        onClick = { onSelect(category.id) },
                        modifier = Modifier.weight(1f),
                    )
                }
                repeat(4 - row.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun CategoryTile(
    category: Category,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(
                if (selected) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                }
            )
            .then(
                if (selected) {
                    Modifier.border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), shape)
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painter = painterResource(categoryIconRes(category.icon)),
            contentDescription = null,
            tint = Color(category.color),
            modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = category.name,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) {
                MaterialTheme.colorScheme.onSecondaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun FieldButton(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

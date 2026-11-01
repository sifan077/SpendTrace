package com.sifan.spendtrace.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sifan.spendtrace.R
import com.sifan.spendtrace.core.DateFormats
import com.sifan.spendtrace.core.Money
import com.sifan.spendtrace.data.model.Expense
import com.sifan.spendtrace.domain.ExpenseDayGroup
import com.sifan.spendtrace.ui.components.BrandHeader
import com.sifan.spendtrace.ui.components.CategoryIcon
import com.sifan.spendtrace.ui.components.DeleteExpenseDialog
import com.sifan.spendtrace.ui.components.EmptyState
import com.sifan.spendtrace.ui.components.ErrorBlock
import com.sifan.spendtrace.ui.components.LoadingBlock
import com.sifan.spendtrace.ui.components.MonthPill
import com.sifan.spendtrace.ui.components.MonthSummaryCard
import com.sifan.spendtrace.ui.components.SectionTitle
import java.time.LocalDate

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onSettingsClick: () -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onBackToCurrentMonth: () -> Unit,
    onExpenseClick: (Expense) -> Unit,
    onDeleteExpense: (Long) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val today = remember { LocalDate.now() }
    var pendingDelete by remember { mutableStateOf<Expense?>(null) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 21.dp, end = 21.dp, top = 20.dp, bottom = 112.dp),
    ) {
        item(key = "brand") {
            BrandHeader(onSettingsClick = onSettingsClick)
            Spacer(Modifier.height(24.dp))
        }

        item(key = "summary") {
            MonthSummaryCard(
                caption = if (uiState.isCurrentMonth) {
                    stringResource(R.string.month_total_caption)
                } else {
                    stringResource(R.string.month_total_label, DateFormats.monthLabel(uiState.month))
                },
                totalMinor = uiState.monthTotalMinor,
                isCurrentMonth = uiState.isCurrentMonth,
                todayTotalMinor = uiState.todayTotalMinor,
                dailyAverageMinor = uiState.dailyAverageMinor,
                expenseCount = uiState.expenseCount,
            )
            Spacer(Modifier.height(26.dp))
        }

        item(key = "section") {
            SectionTitle(
                text = stringResource(R.string.section_records),
                trailing = {
                    MonthPill(
                        month = uiState.month,
                        isCurrentMonth = uiState.isCurrentMonth,
                        onPreviousMonth = onPreviousMonth,
                        onNextMonth = onNextMonth,
                        onBackToCurrentMonth = onBackToCurrentMonth,
                    )
                },
            )
            Spacer(Modifier.height(2.dp))
        }

        when {
            uiState.loading -> item(key = "loading") { LoadingBlock() }

            uiState.error != null -> item(key = "error") {
                ErrorBlock(message = uiState.error, onRetry = onRetry)
            }

            uiState.isEmpty -> item(key = "empty") {
                EmptyState(
                    title = stringResource(
                        if (uiState.isCurrentMonth) R.string.empty_title else R.string.empty_title_past_month
                    ),
                    description = stringResource(
                        if (uiState.isCurrentMonth) R.string.empty_desc else R.string.empty_desc_past_month
                    ),
                )
            }

            else -> uiState.groups.forEach { group ->
                item(key = "day-${group.date}") {
                    DayHeader(group = group, today = today)
                }
                items(items = group.expenses, key = { expense -> "expense-${expense.id}" }) { expense ->
                    ExpenseEntry(
                        expense = expense,
                        onClick = { onExpenseClick(expense) },
                        onLongClick = { pendingDelete = expense },
                    )
                }
            }
        }
    }

    pendingDelete?.let { expense ->
        DeleteExpenseDialog(
            expense = expense,
            onConfirm = {
                onDeleteExpense(expense.id)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null },
        )
    }
}

@Composable
private fun DayHeader(group: ExpenseDayGroup, today: LocalDate) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 22.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = DateFormats.dayLabel(group.date, today),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = Money.formatWithSymbol(group.totalMinor),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ExpenseEntry(
    expense: Expense,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                .padding(vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CategoryIcon(category = expense.category, size = 44.dp)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = expense.category.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = expense.note?.takeIf { it.isNotBlank() }
                        ?: DateFormats.timeLabel(expense.spentAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(4.dp))
            Text(
                text = "−${Money.formatWithSymbol(expense.amountMinor)}",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
        }
        HorizontalDivider(
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant,
        )
    }
}

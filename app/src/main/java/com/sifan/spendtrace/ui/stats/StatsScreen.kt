package com.sifan.spendtrace.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sifan.spendtrace.R
import com.sifan.spendtrace.core.DateFormats
import com.sifan.spendtrace.core.Money
import com.sifan.spendtrace.domain.CategoryTotal
import com.sifan.spendtrace.ui.components.CategoryIcon
import com.sifan.spendtrace.ui.components.EmptyState
import com.sifan.spendtrace.ui.components.ErrorBlock
import com.sifan.spendtrace.ui.components.LoadingBlock
import com.sifan.spendtrace.ui.components.MonthPill
import com.sifan.spendtrace.ui.components.SectionTitle
import com.sifan.spendtrace.ui.home.HomeUiState
import java.util.Locale

@Composable
fun StatsScreen(
    uiState: HomeUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onBackToCurrentMonth: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 21.dp, end = 21.dp, top = 20.dp, bottom = 32.dp),
    ) {
        item(key = "header") {
            SectionTitle(
                text = stringResource(R.string.section_stats),
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
            Spacer(Modifier.height(18.dp))
        }

        item(key = "total") {
            StatsTotalCard(state = uiState)
            Spacer(Modifier.height(24.dp))
        }

        when {
            uiState.loading -> item(key = "loading") { LoadingBlock() }

            uiState.error != null -> item(key = "error") {
                ErrorBlock(message = uiState.error, onRetry = onRetry)
            }

            uiState.categoryTotals.isEmpty() -> item(key = "empty") {
                EmptyState(
                    title = stringResource(R.string.empty_stats_title),
                    description = stringResource(R.string.empty_stats_desc),
                )
            }

            else -> {
                item(key = "category-title") {
                    SectionTitle(text = stringResource(R.string.stats_section_title))
                    Spacer(Modifier.height(6.dp))
                }
                items(items = uiState.categoryTotals, key = { it.category.id }) { total ->
                    CategoryShareRow(total = total)
                }
            }
        }
    }
}

@Composable
private fun StatsTotalCard(state: HomeUiState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(21.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(20.dp),
    ) {
        Text(
            text = if (state.isCurrentMonth) {
                stringResource(R.string.stats_total_label)
            } else {
                stringResource(R.string.stats_total_label_past, DateFormats.monthLabel(state.month))
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.62f),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = Money.formatWithSymbol(state.monthTotalMinor),
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = stringResource(
                R.string.stats_count_and_average,
                state.expenseCount,
                Money.formatWithSymbol(state.dailyAverageMinor),
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.62f),
        )
    }
}

@Composable
private fun CategoryShareRow(total: CategoryTotal) {
    val categoryColor = Color(total.category.color)
    Column(modifier = Modifier.padding(vertical = 10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CategoryIcon(category = total.category, size = 28.dp)
            Text(
                text = total.category.name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = formatShare(total.share),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = Money.formatWithSymbol(total.amountMinor),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Spacer(Modifier.height(9.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(9.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        ) {
            val fraction = total.share.coerceIn(0f, 1f)
            if (fraction > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(20.dp))
                        .background(categoryColor),
                )
            }
        }
    }
}

private fun formatShare(share: Float): String =
    String.format(Locale.CHINA, "%.1f%%", share.coerceIn(0f, 1f) * 100f)

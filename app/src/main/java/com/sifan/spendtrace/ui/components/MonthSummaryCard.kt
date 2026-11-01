package com.sifan.spendtrace.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sifan.spendtrace.R
import com.sifan.spendtrace.core.Money

/**
 * 首页顶部汇总卡片：本月总支出 + 今日支出 / 日均支出。
 *
 * 右上角的六角星是纯装饰，参考原型的 `✳` 水印。
 */
@Composable
fun MonthSummaryCard(
    caption: String,
    totalMinor: Long,
    isCurrentMonth: Boolean,
    todayTotalMinor: Long,
    dailyAverageMinor: Long,
    expenseCount: Int,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.primaryContainer),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_sparkle),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.08f),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 30.dp, y = (-34).dp)
                .size(140.dp),
        )

        Column(modifier = Modifier.padding(horizontal = 23.dp, vertical = 22.dp)) {
            Text(
                text = caption,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.62f),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = Money.formatWithSymbol(totalMinor),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(44.dp)) {
                SummaryStat(
                    label = if (isCurrentMonth) {
                        stringResource(R.string.today_total_label)
                    } else {
                        stringResource(R.string.expense_count_label)
                    },
                    value = if (isCurrentMonth) {
                        Money.formatWithSymbol(todayTotalMinor)
                    } else {
                        stringResource(R.string.expense_count_value, expenseCount)
                    },
                )
                SummaryStat(
                    label = stringResource(R.string.daily_average_label),
                    value = Money.formatWithSymbol(dailyAverageMinor),
                )
            }
        }
    }
}

@Composable
private fun SummaryStat(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.62f),
        )
        Spacer(Modifier.height(5.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

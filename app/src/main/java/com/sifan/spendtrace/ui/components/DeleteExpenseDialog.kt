package com.sifan.spendtrace.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.sifan.spendtrace.R
import com.sifan.spendtrace.core.DateFormats
import com.sifan.spendtrace.core.Money
import com.sifan.spendtrace.data.model.Expense

/** 删除支出的二次确认弹窗。 */
@Composable
fun DeleteExpenseDialog(
    expense: Expense,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.delete_title)) },
        text = {
            Text(
                text = "${expense.category.name} ${Money.formatWithSymbol(expense.amountMinor)}" +
                    "（${DateFormats.shortDateLabel(DateFormats.toLocalDate(expense.spentAt))}）\n" +
                    stringResource(R.string.delete_message),
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = stringResource(R.string.delete),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.cancel))
            }
        },
    )
}

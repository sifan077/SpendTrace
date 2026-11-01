package com.sifan.spendtrace.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sifan.spendtrace.R
import com.sifan.spendtrace.data.local.DefaultCategories
import com.sifan.spendtrace.data.model.Category

/** 分类图标标识 -> 项目内置矢量图。 */
@DrawableRes
fun categoryIconRes(icon: String): Int = when (icon) {
    DefaultCategories.ICON_FOOD -> R.drawable.ic_category_food
    DefaultCategories.ICON_TRANSPORT -> R.drawable.ic_category_transport
    DefaultCategories.ICON_SHOPPING -> R.drawable.ic_category_shopping
    DefaultCategories.ICON_DAILY -> R.drawable.ic_category_daily
    DefaultCategories.ICON_ENTERTAINMENT -> R.drawable.ic_category_entertainment
    DefaultCategories.ICON_MEDICAL -> R.drawable.ic_category_medical
    else -> R.drawable.ic_category_other
}

/** 圆角方形底 + 分类图标，颜色取自分类自身色值。 */
@Composable
fun CategoryIcon(
    category: Category,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
) {
    val color = Color(category.color)
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.34f))
            .background(color.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(categoryIconRes(category.icon)),
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(size * 0.5f),
        )
    }
}

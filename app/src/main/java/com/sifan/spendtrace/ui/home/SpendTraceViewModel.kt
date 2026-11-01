package com.sifan.spendtrace.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.sifan.spendtrace.AppContainer
import com.sifan.spendtrace.core.DateFormats
import com.sifan.spendtrace.data.AppPreferences
import com.sifan.spendtrace.data.ThemeMode
import com.sifan.spendtrace.data.model.Category
import com.sifan.spendtrace.data.model.ExpenseDraft
import com.sifan.spendtrace.data.repository.CategoryRepository
import com.sifan.spendtrace.data.repository.ExpenseRepository
import com.sifan.spendtrace.domain.CategoryTotal
import com.sifan.spendtrace.domain.ExpenseDayGroup
import com.sifan.spendtrace.domain.ExpenseStats
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 首页与统计页共用的页面状态。 */
data class HomeUiState(
    val month: YearMonth,
    val isCurrentMonth: Boolean = true,
    val loading: Boolean = true,
    val monthTotalMinor: Long = 0L,
    val todayTotalMinor: Long = 0L,
    val dailyAverageMinor: Long = 0L,
    val expenseCount: Int = 0,
    val groups: List<ExpenseDayGroup> = emptyList(),
    val categoryTotals: List<CategoryTotal> = emptyList(),
    val error: String? = null,
) {
    val isEmpty: Boolean
        get() = !loading && error == null && groups.isEmpty()
}

/** 弹窗内保存 / 删除的一次性结果。 */
sealed interface EditorEvent {
    data object Saved : EditorEvent
    data object Deleted : EditorEvent
    data class SaveFailed(val detail: String) : EditorEvent
    data class DeleteFailed(val detail: String) : EditorEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
class SpendTraceViewModel(
    private val expenseRepository: ExpenseRepository,
    categoryRepository: CategoryRepository,
    private val preferences: AppPreferences,
    private val zoneId: ZoneId = ZoneId.systemDefault(),
    private val clock: Clock = Clock.systemDefaultZone(),
) : ViewModel() {

    private val today: LocalDate
        get() = LocalDate.now(clock.withZone(zoneId))

    private val selectedMonth = MutableStateFlow(YearMonth.now(clock.withZone(zoneId)))
    private val retryTick = MutableStateFlow(0)
    private val _isSaving = MutableStateFlow(false)
    private val _lastCategoryId = MutableStateFlow(preferences.lastCategoryId())
    private val _themeMode = MutableStateFlow(preferences.themeMode())
    private val _events = MutableSharedFlow<EditorEvent>(extraBufferCapacity = 4)

    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()
    val lastCategoryId: StateFlow<Long?> = _lastCategoryId.asStateFlow()
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    /** 全部记录数（跨月份），用于设置页的数据概览。 */
    val totalCount: StateFlow<Int> = expenseRepository.observeTotalCount()
        .catch { emit(0) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
    val events: SharedFlow<EditorEvent> = _events.asSharedFlow()

    val categories: StateFlow<List<Category>> = categoryRepository.observeActive()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val monthExpenses = combine(selectedMonth, retryTick) { month, _ -> month }
        .flatMapLatest { month ->
            expenseRepository.observeMonth(month, zoneId)
                .map { expenses -> Result.success(expenses) }
                .catch { throwable -> emit(Result.failure(throwable)) }
        }

    val uiState: StateFlow<HomeUiState> =
        combine(selectedMonth, monthExpenses) { month, result ->
            val currentDay = today
            val isCurrentMonth = month == YearMonth.from(currentDay)
            val expenses = result.getOrNull()
            if (expenses == null) {
                HomeUiState(
                    month = month,
                    isCurrentMonth = isCurrentMonth,
                    loading = false,
                    error = result.exceptionOrNull()?.message ?: "数据库读取失败",
                )
            } else {
                val total = ExpenseStats.total(expenses)
                HomeUiState(
                    month = month,
                    isCurrentMonth = isCurrentMonth,
                    loading = false,
                    monthTotalMinor = total,
                    todayTotalMinor = if (isCurrentMonth) {
                        ExpenseStats.todayTotal(expenses, currentDay, zoneId)
                    } else {
                        0L
                    },
                    dailyAverageMinor = ExpenseStats.dailyAverage(total, month, currentDay),
                    expenseCount = expenses.size,
                    groups = ExpenseStats.groupByDay(expenses, zoneId),
                    categoryTotals = ExpenseStats.categoryTotals(expenses),
                )
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState(month = selectedMonth.value),
        )

    fun selectPreviousMonth() {
        selectedMonth.update { it.minusMonths(1) }
    }

    fun selectNextMonth() {
        val currentMonth = YearMonth.now(clock.withZone(zoneId))
        selectedMonth.update { month -> if (month < currentMonth) month.plusMonths(1) else month }
    }

    fun goToCurrentMonth() {
        selectedMonth.value = YearMonth.now(clock.withZone(zoneId))
    }

    fun retry() {
        retryTick.update { it + 1 }
    }

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        preferences.setThemeMode(mode)
    }

    fun saveExpense(draft: ExpenseDraft) {
        if (_isSaving.value) return
        viewModelScope.launch {
            _isSaving.value = true
            try {
                expenseRepository.save(draft)
                preferences.setLastCategoryId(draft.categoryId)
                _lastCategoryId.value = draft.categoryId
                // 记录可能落在其他月份，切换过去让用户立刻看到结果
                selectedMonth.value = YearMonth.from(DateFormats.toLocalDate(draft.spentAt, zoneId))
                _events.emit(EditorEvent.Saved)
            } catch (throwable: Exception) {
                _events.emit(EditorEvent.SaveFailed(throwable.message ?: "未知错误"))
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun deleteExpense(expenseId: Long) {
        if (_isSaving.value) return
        viewModelScope.launch {
            _isSaving.value = true
            try {
                expenseRepository.delete(expenseId)
                _events.emit(EditorEvent.Deleted)
            } catch (throwable: Exception) {
                _events.emit(EditorEvent.DeleteFailed(throwable.message ?: "未知错误"))
            } finally {
                _isSaving.value = false
            }
        }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                SpendTraceViewModel(
                    expenseRepository = container.expenseRepository,
                    categoryRepository = container.categoryRepository,
                    preferences = container.preferences,
                )
            }
        }
    }
}

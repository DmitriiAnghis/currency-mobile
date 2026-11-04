package md.anghis.currency.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight

private val TABS = listOf("⇄" to "Конвертер", "7" to "Неделя", "30" to "Месяц")

/** Всё приложение в одном окне: тёмная шапка, вкладка и нижняя панель вкладок. Без всплывающих окон. */
@Composable
fun CurrencyApp(
    converter: ConverterState,
    week: HistoryState,
    month: HistoryState,
    converterActions: ConverterActions,
    historyActions: HistoryActions,
    initialTab: Int = 0,
) {
    var tab by rememberSaveable { mutableIntStateOf(initialTab) }
    Scaffold(
        containerColor = Palette.bg,
        topBar = { AppHeader("Курс BNM · ${TABS[tab].second}", converter.headerStatus) },
        bottomBar = {
            NavigationBar(containerColor = Palette.card) {
                TABS.forEachIndexed { i, (icon, label) ->
                    NavigationBarItem(
                        selected = tab == i,
                        onClick = { tab = i },
                        icon = { Text(icon, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
                        label = { Text(label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Palette.accent,
                            selectedTextColor = Palette.accent,
                            indicatorColor = Palette.badgeBg,
                            unselectedIconColor = Palette.muted,
                            unselectedTextColor = Palette.muted,
                        ),
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when (tab) {
                0 -> ConverterScreen(converter, converterActions)
                1 -> ComparisonScreen(week, converter.currencies, historyActions)
                else -> ComparisonScreen(month, converter.currencies, historyActions)
            }
        }
    }
}

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import md.anghis.currency.core.ConversionLog
import md.anghis.currency.core.RatesTable
import md.anghis.currency.data.shareCsv
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

const val TAB_CONVERTER = 0
const val TAB_RATES = 1
const val TAB_WEEK = 2
const val TAB_MONTH = 3
const val TAB_JOURNAL = 4

private val TABS = listOf("⇄" to "Конвертер", "≡" to "Курсы", "7" to "Неделя", "30" to "Месяц", "☰" to "Журнал")
private val FILE_STAMP = DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")

/** Всё приложение в одном окне: тёмная шапка, вкладка и нижняя панель вкладок. Без всплывающих окон. */
@Composable
fun CurrencyApp(
    converter: ConverterState,
    week: HistoryState,
    month: HistoryState,
    journal: JournalState,
    converterActions: ConverterActions,
    historyActions: HistoryActions,
    journalActions: JournalActions,
    initialTab: Int = TAB_CONVERTER,
    initialRatesQuery: String = "",
) {
    var tab by rememberSaveable { mutableIntStateOf(initialTab) }
    val context = LocalContext.current
    Scaffold(
        containerColor = Palette.bg,
        topBar = { AppHeader("Курс BNM · ${TABS[tab].second}", converter.headerStatus) },
        bottomBar = {
            NavigationBar(containerColor = Palette.card) {
                TABS.forEachIndexed { i, (icon, label) ->
                    val shown = if (i == TAB_JOURNAL && journal.entries.isNotEmpty()) "$label ${journal.entries.size}" else label
                    NavigationBarItem(
                        selected = tab == i,
                        onClick = { tab = i },
                        icon = { Text(icon, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
                        label = { Text(shown, maxLines = 1, overflow = TextOverflow.Clip) },
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
                TAB_CONVERTER -> ConverterScreen(converter, converterActions)
                TAB_RATES -> RatesScreen(
                    converter, converterActions,
                    onPick = { code -> journalActions.useCurrency(code); tab = TAB_CONVERTER },
                    onShareCsv = {
                        converter.rates?.let { rates ->
                            shareCsv(context, "bnm_rates_${rates.date}.csv", RatesTable.toCsv(rates))
                        }
                    },
                    initialQuery = initialRatesQuery,
                )
                TAB_WEEK -> ComparisonScreen(week, converter.currencies, historyActions)
                TAB_MONTH -> ComparisonScreen(month, converter.currencies, historyActions)
                else -> JournalScreen(
                    journal, journalActions,
                    onRepeat = { id -> journalActions.repeat(id); tab = TAB_CONVERTER },
                    onShareCsv = {
                        shareCsv(context, "journal_${LocalDateTime.now().format(FILE_STAMP)}.csv",
                            ConversionLog.toCsv(journal.shown))
                    },
                )
            }
        }
    }
}

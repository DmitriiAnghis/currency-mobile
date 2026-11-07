package md.anghis.currency.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import md.anghis.currency.core.AmountResult
import md.anghis.currency.core.AmountValidator
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.math.BigDecimal

/**
 * Рисует экраны приложения в JVM (Robolectric) и сохраняет PNG в docs/screenshots/app.
 * Эти картинки вставлены в README, поэтому они всегда совпадают с текущим кодом.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w400dp-h860dp-xhdpi")
class ScreenshotTest {
    private companion object {
        /** Экраны сравнения длиннее: высота побольше, чтобы влезла вся статистика. */
        const val TALL = "w400dp-h1180dp-xhdpi"
    }

    @get:Rule
    val compose = createComposeRule()

    private val ready = ConverterState(loading = false, rates = DemoData.rates)

    private fun shot(
        name: String,
        converter: ConverterState = ready,
        week: HistoryState = DemoData.history(7),
        month: HistoryState = DemoData.history(30),
        journal: JournalState = DemoData.journal(),
        tab: Int = TAB_CONVERTER,
        ratesQuery: String = "",
    ) {
        compose.setContent {
            AppTheme {
                CurrencyApp(converter, week, month, journal, NoActions, NoActions, NoActions,
                    initialTab = tab, initialRatesQuery = ratesQuery)
            }
        }
        java.io.File("../docs/screenshots/app").mkdirs()
        compose.onRoot().captureRoboImage("../docs/screenshots/app/$name.png")
    }

    @Test
    fun converterResult() = shot(
        "01_converter",
        ready.copy(result = formatResult(BigDecimal(100), "EUR", "MDL", DemoData.rates)),
    )

    @Test
    fun crossRate() = shot(
        "02_cross_usd_eur",
        ready.copy(amount = "250", from = "USD", to = "EUR",
            result = formatResult(BigDecimal(250), "USD", "EUR", DemoData.rates)),
    )

    @Test
    fun invalidInput() = shot(
        "03_invalid",
        ready.copy(amount = "12abc", inputError = (AmountValidator.validate("12abc") as AmountResult.Invalid).message),
    )

    @Test
    fun offlineCache() = shot(
        "04_offline_cache",
        ready.copy(fromCache = true,
            notice = "Нет подключения к источнику. Показан сохранённый курс от 04.11.2026",
            result = formatResult(BigDecimal(100), "EUR", "MDL", DemoData.rates)),
    )

    @Test
    fun noData() = shot(
        "05_no_data",
        ConverterState(loading = false, error = "Нет подключения к источнику, сохранённых курсов нет"),
    )

    @Test
    @Config(qualifiers = TALL)
    fun week() = shot("06_week", tab = TAB_WEEK)

    @Test
    @Config(qualifiers = TALL)
    fun month() = shot("07_month", tab = TAB_MONTH)

    @Test
    @Config(qualifiers = TALL)
    fun weekOffline() = shot(
        "08_week_offline",
        week = DemoData.history(7, notice = "Нет сети: показаны сохранённые данные, последние от 04.11.2026"),
        tab = TAB_WEEK,
    )

    @Test
    @Config(qualifiers = TALL)
    fun rates() = shot("09_rates", tab = TAB_RATES)

    @Test
    fun ratesSearch() = shot("10_rates_search", tab = TAB_RATES, ratesQuery = "leu")

    @Test
    @Config(qualifiers = TALL)
    fun journal() = shot("11_journal", tab = TAB_JOURNAL)

    @Test
    fun journalSearch() = shot("12_journal_search", journal = DemoData.journal().copy(query = "eur"), tab = TAB_JOURNAL)

    @Test
    fun journalDeleted() {
        val j = DemoData.journal()
        val removed = j.entries[1]
        shot("13_journal_deleted",
            journal = j.copy(entries = j.entries - removed, message = "Удалено: ${removed.text}", undo = removed),
            tab = TAB_JOURNAL)
    }

    @Test
    fun journalClearConfirm() = shot("14_journal_clear", journal = DemoData.journal().copy(confirmClear = true),
        tab = TAB_JOURNAL)

    @Test
    fun journalEmpty() = shot("15_journal_empty", journal = JournalState(), tab = TAB_JOURNAL)
}

package md.anghis.currency.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import md.anghis.currency.core.RatesTable
import md.anghis.currency.core.fmt

/**
 * Вкладка «Курсы»: все официальные курсы BNM на дату, поиск по коду и названию,
 * нажатие на строку открывает конвертер с этой валютой, CSV можно отправить через «Поделиться».
 */
@Composable
fun RatesScreen(
    state: ConverterState,
    actions: ConverterActions,
    onPick: (String) -> Unit,
    onShareCsv: () -> Unit,
    initialQuery: String = "",
) {
    var query by rememberSaveable { mutableStateOf(initialQuery) }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        state.error?.let {
            Banner(it, BannerKind.ERROR, actionLabel = "Повторить", onAction = actions::refreshRates)
        }
        state.notice?.let { Banner(it, BannerKind.WARN) }

        val rates = state.rates
        SectionCard(title = "Официальные курсы") {
            Text(state.sourceLine ?: if (state.loading) "Загрузка курса…" else "Курс не загружен",
                color = Palette.muted, style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Поиск: код или название") },
                singleLine = true,
                colors = fieldColors(),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth(),
            )
            FilledTonalButton(onClick = onShareCsv, enabled = rates != null, shape = RoundedCornerShape(10.dp)) {
                Text("Поделиться CSV", fontWeight = FontWeight.SemiBold)
            }
        }

        if (rates != null) {
            val rows = RatesTable.rows(rates, query)
            SectionCard {
                Row(Modifier.fillMaxWidth()) {
                    Text("Валюта", color = Palette.muted, style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.weight(1f))
                    Text("MDL за 1 ед.", color = Palette.muted, style = MaterialTheme.typography.labelMedium)
                }
                if (rows.isEmpty()) {
                    Text("Ничего не найдено", color = Palette.muted)
                }
                rows.forEachIndexed { i, rate ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                            .background(if (i % 2 == 1) Palette.bg else Palette.card)
                            .clickable { onPick(rate.code) }
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(rate.code, fontWeight = FontWeight.Bold, color = Palette.accent,
                            modifier = Modifier.width(52.dp))
                        Column(Modifier.weight(1f)) {
                            Text(rate.name, color = Palette.text, style = MaterialTheme.typography.bodyMedium)
                            if (rate.nominal > 1) {
                                Text("${rate.nominal} ${rate.code} = ${rate.value.plain()} MDL", color = Palette.muted,
                                    style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        Text(rate.perUnit.short(), fontWeight = FontWeight.SemiBold, color = Palette.text,
                            textAlign = TextAlign.End)
                    }
                }
                Text("Нажмите на валюту, чтобы открыть её в конвертере. Курс на ${rates.date.fmt()}.",
                    color = Palette.muted, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

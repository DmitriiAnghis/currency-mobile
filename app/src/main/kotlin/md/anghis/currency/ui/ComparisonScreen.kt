package md.anghis.currency.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import md.anghis.currency.core.RatePoint
import md.anghis.currency.core.Trend
import md.anghis.currency.core.fmt

private val RISE = Color(0xFF1B873F)
private val FALL = Color(0xFFC62828)

@Composable
fun ComparisonScreen(state: HistoryState, currencies: List<String>, vm: AppViewModel) {
    LaunchedEffect(state.days) { vm.loadHistory(state.days) }
    val title = if (state.days == 7) "Сравнение за неделю" else "Сравнение за месяц"
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall)
        Text("Курс за последние ${state.days} дней", style = MaterialTheme.typography.bodySmall)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CurrencyPicker("Валюта", state.from, currencies.filter { it != state.to },
                { vm.onHistoryPair(state.days, from = it) }, Modifier.weight(1f))
            CurrencyPicker("В", state.to, currencies.filter { it != state.from },
                { vm.onHistoryPair(state.days, to = it) }, Modifier.weight(1f))
        }

        if (state.loading) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CircularProgressIndicator()
                Text("Загрузка истории курса…")
            }
        }
        state.notice?.let { NoticeCard(it) }
        state.error?.let { NoticeCard(it, isError = true) }

        if (state.points.size >= 2) {
            RateChart(state.points, Modifier.fillMaxWidth().height(220.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(state.points.first().date.fmt(), style = MaterialTheme.typography.bodySmall)
                Text(state.points.last().date.fmt(), style = MaterialTheme.typography.bodySmall)
            }
        }

        state.stats?.let { s ->
            val unit = "${state.to} за 1 ${state.from}"
            StatRow("Минимум", "${s.min.value.short()} ($unit), ${s.min.date.fmt()}")
            StatRow("Максимум", "${s.max.value.short()} ($unit), ${s.max.date.fmt()}")
            StatRow("Среднее", s.average.short())
            val sign = if (s.absoluteChange.signum() > 0) "+" else ""
            StatRow("Изменение", "$sign${s.absoluteChange.short()} ($sign${s.percentChange.toPlainString()} %)")
            val (arrow, color) = when (s.trend) {
                Trend.RISE -> "▲" to RISE
                Trend.FALL -> "▼" to FALL
                Trend.FLAT -> "■" to MaterialTheme.colorScheme.onSurface
            }
            Text("$arrow Тенденция: ${s.trend.label}", color = color,
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
        TextButton(onClick = { vm.loadHistory(state.days, force = true) }, enabled = !state.loading) {
            Text("Обновить")
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
    }
}

/** Линейный график курса: ось X — даты по порядку, ось Y — от минимума до максимума. */
@Composable
fun RateChart(points: List<RatePoint>, modifier: Modifier = Modifier) {
    val line = MaterialTheme.colorScheme.primary
    val grid = MaterialTheme.colorScheme.outlineVariant
    val values = points.map { it.value.toFloat() }
    val min = values.min()
    val max = values.max()
    val span = (max - min).takeIf { it > 0f } ?: 1f
    Canvas(modifier.padding(vertical = 8.dp)) {
        val stepX = size.width / (values.size - 1)
        fun y(v: Float) = size.height - (v - min) / span * size.height
        for (i in 0..4) {
            val gy = size.height * i / 4
            drawLine(grid, Offset(0f, gy), Offset(size.width, gy), strokeWidth = 1f)
        }
        val path = Path()
        values.forEachIndexed { i, v ->
            val p = Offset(i * stepX, y(v))
            if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
        }
        drawPath(path, line, style = Stroke(width = 5f))
        values.forEachIndexed { i, v -> drawCircle(line, radius = 7f, center = Offset(i * stepX, y(v))) }
    }
}

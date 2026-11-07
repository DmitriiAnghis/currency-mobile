package md.anghis.currency.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import md.anghis.currency.core.RatePoint
import md.anghis.currency.core.Trend
import md.anghis.currency.core.fmt

@Composable
fun ComparisonScreen(state: HistoryState, currencies: List<String>, actions: HistoryActions) {
    LaunchedEffect(state.days) { actions.loadHistory(state.days) }
    val title = if (state.days == 7) "Сравнение за неделю" else "Сравнение за месяц"
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        state.error?.let {
            Banner(it, BannerKind.ERROR, actionLabel = "Повторить",
                onAction = { actions.loadHistory(state.days, force = true) })
        }
        state.notice?.let { Banner(it, BannerKind.WARN) }

        SectionCard(title = title) {
            Text("Курс за последние ${state.days} дней", color = Palette.muted,
                style = MaterialTheme.typography.bodySmall)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CurrencyPicker("Валюта", state.from, currencies.filter { it != state.to },
                    { actions.onHistoryPair(state.days, from = it) }, Modifier.weight(1f))
                CurrencyPicker("В", state.to, currencies.filter { it != state.from },
                    { actions.onHistoryPair(state.days, to = it) }, Modifier.weight(1f))
            }
        }

        if (state.loading) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = Palette.accent)
                Text("Загрузка истории курса…", color = Palette.muted)
            }
        }

        if (state.points.size >= 2) {
            SectionCard(title = "${state.from} → ${state.to}") {
                RateChart(state.points, Modifier.fillMaxWidth().height(200.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(state.points.first().date.fmt(), color = Palette.muted, style = MaterialTheme.typography.bodySmall)
                    Text(state.points.last().date.fmt(), color = Palette.muted, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        state.stats?.let { s ->
            SectionCard(title = "Статистика") {
                Text("${state.to} за 1 ${state.from}", color = Palette.muted, style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatTile("Минимум", s.min.value.short(), s.min.date.fmt(), Modifier.weight(1f))
                    StatTile("Максимум", s.max.value.short(), s.max.date.fmt(), Modifier.weight(1f))
                }
                val sign = if (s.absoluteChange.signum() > 0) "+" else ""
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatTile("Среднее", s.average.short(), "за ${state.points.size} дн.", Modifier.weight(1f))
                    StatTile("Изменение", "$sign${s.absoluteChange.short()}",
                        "$sign${s.percentChange.toPlainString()} %", Modifier.weight(1f))
                }
                val (arrow, bg, fg) = when (s.trend) {
                    Trend.RISE -> Triple("▲", Palette.okBg, Palette.okText)
                    Trend.FALL -> Triple("▼", Palette.errorBg, Palette.error)
                    Trend.FLAT -> Triple("■", Palette.badgeBg, Palette.accent)
                }
                Badge("$arrow Тенденция: ${s.trend.label}", bg, fg)
            }
        }
        TextButton(onClick = { actions.loadHistory(state.days, force = true) }, enabled = !state.loading) {
            Text("Обновить", color = Palette.accent)
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, hint: String, modifier: Modifier = Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(10.dp)).background(Palette.bg).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(label, color = Palette.muted, style = MaterialTheme.typography.labelMedium)
        Text(value, color = Palette.text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(hint, color = Palette.muted, style = MaterialTheme.typography.bodySmall)
    }
}

/** Линейный график курса: ось X — даты по порядку, ось Y — от минимума до максимума. */
@Composable
fun RateChart(points: List<RatePoint>, modifier: Modifier = Modifier) {
    val line = Palette.accent
    val grid = Palette.border
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
        val fill = Path().apply {
            addPath(path)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(fill, Brush.verticalGradient(listOf(line.copy(alpha = 0.18f), Color.Transparent)))
        drawPath(path, line, style = Stroke(width = 5f))
        values.forEachIndexed { i, v ->
            val c = Offset(i * stepX, y(v))
            drawCircle(Color.White, radius = 8f, center = c)
            drawCircle(line, radius = 5f, center = c)
        }
    }
}

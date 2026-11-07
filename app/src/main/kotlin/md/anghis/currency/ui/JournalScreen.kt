package md.anghis.currency.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import md.anghis.currency.core.LogEntry
import md.anghis.currency.core.fmt

/**
 * Вкладка «Журнал»: все конвертации, поиск, повтор, удаление с возвратом,
 * очистка с подтверждением в полосе (не во всплывающем окне) и CSV через «Поделиться».
 */
@Composable
fun JournalScreen(
    state: JournalState,
    actions: JournalActions,
    onRepeat: (Long) -> Unit,
    onShareCsv: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (state.confirmClear) {
            Banner("Удалить все ${state.entries.size} ${recordWord(state.entries.size)} из журнала?",
                BannerKind.WARN, actionLabel = "Очистить", onAction = actions::confirmClear,
                secondLabel = "Отмена", onSecond = actions::cancelClear)
        }
        state.message?.let { msg ->
            if (state.undo != null) {
                Banner(msg, BannerKind.OK, actionLabel = "Вернуть", onAction = actions::undoDelete,
                    secondLabel = "✕", onSecond = actions::dismissMessage)
            } else {
                Banner(msg, BannerKind.OK, actionLabel = "✕", onAction = actions::dismissMessage)
            }
        }

        SectionCard(title = "Журнал конвертаций") {
            Text(state.caption, color = Palette.muted, style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(
                value = state.query,
                onValueChange = actions::onJournalQuery,
                label = { Text("Поиск: валюта, сумма, дата") },
                singleLine = true,
                colors = fieldColors(),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = onShareCsv, enabled = state.shown.isNotEmpty(),
                    shape = RoundedCornerShape(10.dp), modifier = Modifier.weight(1f)) {
                    Text("Поделиться CSV", fontWeight = FontWeight.SemiBold)
                }
                OutlinedButton(onClick = actions::askClear, enabled = state.entries.isNotEmpty(),
                    shape = RoundedCornerShape(10.dp), modifier = Modifier.weight(1f)) {
                    Text("Очистить всё", color = if (state.entries.isNotEmpty()) Palette.error else Palette.muted)
                }
            }
        }

        if (state.entries.isEmpty()) {
            SectionCard {
                Text("Конвертаций пока нет", fontWeight = FontWeight.SemiBold, color = Palette.text)
                Text("Посчитайте сумму на вкладке «Конвертер» — она появится здесь и сохранится после перезапуска.",
                    color = Palette.muted, style = MaterialTheme.typography.bodySmall)
            }
        } else if (state.shown.isEmpty()) {
            Text("Ничего не найдено", color = Palette.muted)
        }
        state.shown.forEach { entry -> JournalCard(entry, onRepeat, actions::delete) }
    }
}

@Composable
private fun JournalCard(entry: LogEntry, onRepeat: (Long) -> Unit, onDelete: (Long) -> Unit) {
    SectionCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(entry.timeText, color = Palette.muted, style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f))
            if (entry.fromCache) Badge("Сохранённый курс", Palette.warnBg, Palette.warnText)
            else Badge("Курс BNM", Palette.okBg, Palette.okText)
        }
        Text(entry.text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
            color = Palette.text)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Курс на ${entry.rateDate.fmt()}", color = Palette.muted, style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f))
            TextButton(onClick = { onRepeat(entry.id) }) { Text("Повторить", color = Palette.accent) }
            TextButton(onClick = { onDelete(entry.id) }) { Text("Удалить", color = Palette.error) }
        }
    }
}

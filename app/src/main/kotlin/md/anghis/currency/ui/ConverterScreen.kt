package md.anghis.currency.ui

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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ConverterScreen(state: ConverterState, actions: ConverterActions) {
    val clipboard = LocalClipboardManager.current
    var copied by remember(state.result) { mutableStateOf(false) }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        state.error?.let {
            Banner(it, BannerKind.ERROR, actionLabel = "Повторить", onAction = actions::refreshRates)
        }
        state.notice?.let { Banner(it, BannerKind.WARN) }
        if (copied) {
            Banner("Результат скопирован в буфер обмена", BannerKind.OK, actionLabel = "✕", onAction = { copied = false })
        }

        SectionCard(title = "Конвертер валют") {
            OutlinedTextField(
                value = state.amount,
                onValueChange = actions::onAmountChange,
                label = { Text("Сумма") },
                isError = state.inputError != null,
                supportingText = state.inputError?.let { { Text(it) } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (state.canConvert) actions.convert() }),
                colors = fieldColors(),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CurrencyPicker("Из", state.from, state.currencies, actions::onFromChange, Modifier.weight(1f))
                FilledTonalButton(onClick = actions::swap, shape = RoundedCornerShape(10.dp)) {
                    Text("⇄", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
                CurrencyPicker("В", state.to, state.currencies, actions::onToChange, Modifier.weight(1f))
            }
            Button(
                onClick = actions::convert,
                enabled = state.canConvert,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Palette.accent,
                    disabledContainerColor = Palette.accentDisabled, disabledContentColor = Palette.card),
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) {
                Text("Конвертировать", fontWeight = FontWeight.SemiBold)
            }
        }

        if (state.loading) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = Palette.accent)
                Text("Загрузка курса…", color = Palette.muted)
            }
        }

        val result = state.result
        if (result != null) {
            val lines = result.lines()
            SectionCard(title = "Результат") {
                Text(lines.first(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
                    color = Palette.text)
                lines.drop(1).forEach { Text(it, color = Palette.muted, style = MaterialTheme.typography.bodyMedium) }
                SourceBadge(state)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = {
                        clipboard.setText(AnnotatedString(listOfNotNull(result, state.sourceLine).joinToString("\n")))
                        copied = true
                    }, shape = RoundedCornerShape(10.dp)) {
                        Text("Копировать", fontWeight = FontWeight.SemiBold)
                    }
                }
                Text("Конвертация сохранена во вкладке «Журнал».", color = Palette.muted,
                    style = MaterialTheme.typography.bodySmall)
            }
        } else if (state.rates != null) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SourceBadge(state)
                Text("Введите сумму, выберите валюты и нажмите «Конвертировать».", color = Palette.muted,
                    style = MaterialTheme.typography.bodySmall)
            }
        }

        TextButton(onClick = actions::refreshRates, enabled = !state.loading) {
            Text("Обновить курс", color = Palette.accent)
        }
    }
}

@Composable
private fun SourceBadge(state: ConverterState) {
    val line = state.sourceLine ?: return
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (state.fromCache) Badge("Сохранённый курс", Palette.warnBg, Palette.warnText)
        else Badge("Курс BNM", Palette.okBg, Palette.okText)
        Text(line, color = Palette.muted, style = MaterialTheme.typography.bodySmall)
    }
}

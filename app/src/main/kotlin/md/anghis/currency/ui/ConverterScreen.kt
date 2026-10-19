package md.anghis.currency.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun ConverterScreen(state: ConverterState, vm: AppViewModel) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Конвертер валют", style = MaterialTheme.typography.headlineSmall)

        OutlinedTextField(
            value = state.amount,
            onValueChange = vm::onAmountChange,
            label = { Text("Сумма") },
            isError = state.inputError != null,
            supportingText = { state.inputError?.let { Text(it) } },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CurrencyPicker("Из", state.from, state.currencies, vm::onFromChange, Modifier.weight(1f))
            OutlinedButton(onClick = vm::swap) { Text("⇄") }
            CurrencyPicker("В", state.to, state.currencies, vm::onToChange, Modifier.weight(1f))
        }
        Button(onClick = vm::convert, enabled = state.canConvert, modifier = Modifier.fillMaxWidth()) {
            Text("Конвертировать")
        }

        state.result?.let {
            Text(it, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
        if (state.loading) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CircularProgressIndicator()
                Text("Загрузка курса…")
            }
        }
        state.sourceLine?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        state.notice?.let { NoticeCard(it) }
        state.error?.let { NoticeCard(it, isError = true) }
        TextButton(onClick = vm::refreshRates, enabled = !state.loading) { Text("Обновить курс") }
    }
}

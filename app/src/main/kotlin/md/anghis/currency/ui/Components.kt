package md.anghis.currency.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Тёмная шапка: название приложения и какие курсы сейчас загружены. */
@Composable
fun AppHeader(title: String, status: String) {
    Column(
        Modifier.fillMaxWidth().background(Palette.header).statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 14.dp),
    ) {
        Text(title, color = Palette.headerText, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(status, color = Palette.headerMuted, style = MaterialTheme.typography.bodySmall)
    }
}

/** Белая карточка с рамкой, как в Лаб 2 и Лаб 3. */
@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier.fillMaxWidth().clip(shape).background(Palette.card).border(1.dp, Palette.border, shape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (title != null) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                color = Palette.text)
        }
        content()
    }
}

enum class BannerKind(val bg: Color, val fg: Color, val mark: String) {
    WARN(Palette.warnBg, Palette.warnText, "!"),
    ERROR(Palette.errorBg, Palette.error, "×"),
    OK(Palette.okBg, Palette.okText, "✓"),
}

/** Полоса-сообщение вместо всплывающего окна. Кнопка действия показывается прямо в полосе. */
@Composable
fun Banner(
    text: String,
    kind: BannerKind = BannerKind.WARN,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    secondLabel: String? = null,
    onSecond: (() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(10.dp)
    Row(
        Modifier.fillMaxWidth().height(IntrinsicSize.Min).clip(shape).background(kind.bg)
            .padding(end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(4.dp).fillMaxHeight().background(kind.fg))
        Text(kind.mark, color = kind.fg, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 12.dp, end = 8.dp))
        Text(text, color = kind.fg, style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f).padding(vertical = 12.dp))
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction) { Text(actionLabel, color = kind.fg, fontWeight = FontWeight.SemiBold) }
        }
        if (secondLabel != null && onSecond != null) {
            TextButton(onClick = onSecond) { Text(secondLabel, color = Palette.muted) }
        }
    }
}

/** Небольшая плашка: зелёная — курс BNM, оранжевая — сохранённый курс. */
@Composable
fun Badge(text: String, bg: Color, fg: Color) {
    Text(
        text, color = fg, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold,
        modifier = Modifier.clip(RoundedCornerShape(50)).background(bg).padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

@Composable
fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Palette.accent,
    unfocusedBorderColor = Palette.border,
    focusedLabelColor = Palette.accent,
    unfocusedLabelColor = Palette.muted,
    focusedContainerColor = Palette.card,
    unfocusedContainerColor = Palette.card,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencyPicker(
    label: String,
    value: String,
    options: List<String>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            colors = fieldColors(),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { code ->
                DropdownMenuItem(text = { Text(code) }, onClick = {
                    onSelect(code)
                    expanded = false
                })
            }
        }
    }
}

package md.anghis.currency

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import md.anghis.currency.ui.AppViewModel
import md.anghis.currency.ui.ComparisonScreen
import md.anghis.currency.ui.ConverterScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                val vm: AppViewModel = viewModel()
                val converter by vm.converter.collectAsStateWithLifecycle()
                val week by vm.week.collectAsStateWithLifecycle()
                val month by vm.month.collectAsStateWithLifecycle()
                var tab by rememberSaveable { mutableIntStateOf(0) }
                Scaffold(
                    bottomBar = {
                        NavigationBar {
                            listOf("⇄" to "Конвертер", "7" to "Неделя", "30" to "Месяц").forEachIndexed { i, (icon, label) ->
                                NavigationBarItem(
                                    selected = tab == i,
                                    onClick = { tab = i },
                                    icon = { Text(icon, style = MaterialTheme.typography.titleMedium) },
                                    label = { Text(label) },
                                )
                            }
                        }
                    },
                ) { padding ->
                    Box(Modifier.padding(padding)) {
                        when (tab) {
                            0 -> ConverterScreen(converter, vm)
                            1 -> ComparisonScreen(week, converter.currencies, vm)
                            else -> ComparisonScreen(month, converter.currencies, vm)
                        }
                    }
                }
            }
        }
    }
}

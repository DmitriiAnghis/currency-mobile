package md.anghis.currency

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import md.anghis.currency.ui.AppTheme
import md.anghis.currency.ui.AppViewModel
import md.anghis.currency.ui.CurrencyApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppTheme {
                val vm: AppViewModel = viewModel()
                val converter by vm.converter.collectAsStateWithLifecycle()
                val week by vm.week.collectAsStateWithLifecycle()
                val month by vm.month.collectAsStateWithLifecycle()
                val journal by vm.journal.collectAsStateWithLifecycle()
                CurrencyApp(converter, week, month, journal, vm, vm, vm)
            }
        }
    }
}

package net.hilson.qrieux.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import net.hilson.qrieux.ui.theme.QRieuxUiConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
actual fun PlatformSelectionField(
    selectedIndex: Int,
    optionLabels: List<String>,
    onOptionSelected: (Int) -> Unit,
    modifier: Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val safeSelectedIndex = selectedIndex.coerceIn(optionLabels.indices)

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = {
            focusManager.clearFocus()
            keyboardController?.hide()
            expanded = !expanded
        },
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
                .heightIn(min = QRieuxUiConfig.controlHeight)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(QRieuxUiConfig.controlCornerRadius)
                )
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = optionLabels[safeSelectedIndex],
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = QRieuxUiConfig.bodySize),
                    modifier = Modifier.weight(1f)
                )
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            optionLabels.forEachIndexed { index, label ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = label,
                            fontSize = QRieuxUiConfig.bodySize
                        )
                    },
                    onClick = {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                        onOptionSelected(index)
                        expanded = false
                    }
                )
            }
        }
    }
}

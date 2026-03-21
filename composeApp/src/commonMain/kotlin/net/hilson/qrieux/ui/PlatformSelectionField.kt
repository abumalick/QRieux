package net.hilson.qrieux.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun PlatformSelectionField(
    selectedIndex: Int,
    optionLabels: List<String>,
    onOptionSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
)

package net.hilson.qrieux.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import kotlinx.cinterop.ObjCSignatureOverride
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readValue
import net.hilson.qrieux.ui.theme.QRieuxUiConfig
import platform.CoreGraphics.CGRectZero
import platform.UIKit.*
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun PlatformSelectionField(
    selectedIndex: Int,
    optionLabels: List<String>,
    onOptionSelected: (Int) -> Unit,
    modifier: Modifier
) {
    val latestOnOptionSelected by rememberUpdatedState(onOptionSelected)
    val coordinator = remember { IosPickerCoordinator() }

    UIKitView(
        factory = {
            val pickerView = UIPickerView()
            val textField = UITextField(frame = CGRectZero.readValue()).apply {
                tintColor = UIColor.clearColor
                backgroundColor = UIColor.colorWithWhite(0.98, alpha = 1.0)
                textColor = UIColor.blackColor
                font = UIFont.systemFontOfSize(QRieuxUiConfig.bodySize.value.toDouble())
                textAlignment = NSTextAlignmentNatural
                val paddingView = UIView(frame = platform.CoreGraphics.CGRectMake(0.0, 0.0, 16.0, 0.0))
                leftView = paddingView
                leftViewMode = UITextFieldViewMode.UITextFieldViewModeAlways
                rightView = UIView(frame = platform.CoreGraphics.CGRectMake(0.0, 0.0, 16.0, 0.0))
                rightViewMode = UITextFieldViewMode.UITextFieldViewModeAlways
                layer.cornerRadius = 12.0
                layer.borderWidth = 1.0
                layer.borderColor = UIColor.colorWithWhite(0.82, alpha = 1.0).CGColor
                inputView = pickerView
                isAccessibilityElement = true
                accessibilityLabel = "selection_field"
            }

            coordinator.attach(
                textField = textField,
                pickerView = pickerView
            )
            textField
        },
        modifier = modifier
            .fillMaxWidth()
            .height(QRieuxUiConfig.controlHeight),
        update = { textField ->
            coordinator.update(
                selectedIndex = selectedIndex,
                optionLabels = optionLabels,
                onOptionSelected = latestOnOptionSelected
            )
            textField.text = optionLabels.getOrElse(selectedIndex.coerceIn(optionLabels.indices)) { "" }
        }
    )
}

private class IosPickerCoordinator : NSObject(), UIPickerViewDataSourceProtocol, UIPickerViewDelegateProtocol {
    private var optionLabels: List<String> = emptyList()
    private var onOptionSelected: (Int) -> Unit = {}
    private var textField: UITextField? = null
    private var pickerView: UIPickerView? = null

    fun attach(
        textField: UITextField,
        pickerView: UIPickerView
    ) {
        this.textField = textField
        this.pickerView = pickerView
        pickerView.dataSource = this
        pickerView.delegate = this
    }

    fun update(
        selectedIndex: Int,
        optionLabels: List<String>,
        onOptionSelected: (Int) -> Unit
    ) {
        val safeIndex = selectedIndex.coerceIn(optionLabels.indices)
        this.optionLabels = optionLabels
        this.onOptionSelected = onOptionSelected
        pickerView?.reloadAllComponents()
        pickerView?.selectRow(safeIndex.toLong(), inComponent = 0, animated = false)
        textField?.text = optionLabels.getOrElse(safeIndex) { "" }
    }

    override fun numberOfComponentsInPickerView(pickerView: UIPickerView): Long = 1

    @ObjCSignatureOverride
    override fun pickerView(pickerView: UIPickerView, numberOfRowsInComponent: Long): Long =
        optionLabels.size.toLong()

    @ObjCSignatureOverride
    override fun pickerView(
        pickerView: UIPickerView,
        titleForRow: Long,
        forComponent: Long
    ): String = optionLabels.getOrElse(titleForRow.toInt()) { "" }

    @ObjCSignatureOverride
    override fun pickerView(
        pickerView: UIPickerView,
        didSelectRow: Long,
        inComponent: Long
    ) {
        val index = didSelectRow.toInt()
        textField?.text = optionLabels.getOrElse(index) { "" }
        onOptionSelected(index)
    }
}

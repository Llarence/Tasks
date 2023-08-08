package me.llarence.common

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.TextField
import androidx.compose.runtime.*

@Composable
fun <T> RestrictedTextField(value: T, valueToText: (T) -> String, textToValue: (String) -> T?, onValueChange: (T) -> Unit, enabled: Boolean = true, keyboardOptions: KeyboardOptions = KeyboardOptions()) {
    var text by remember { mutableStateOf("") }

    val textValue = textToValue(text)
    if (textValue != null) {
        if (textValue != value) {
            text = valueToText(value)
        }
    }

    TextField(text, {
        text = it

        val currValue = textToValue(text)
        if (currValue != null) {
            onValueChange(currValue)
        }
    }, enabled = enabled, keyboardOptions = keyboardOptions)
}
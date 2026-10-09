package com.martinsterentjevs.cacheit.ui.common.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation

/** Exists as a common  component for reuse in multiple screens
 *
 */
@Composable
fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String,
    enabled: Boolean = true,
) {
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        label = { Text(label) },
        singleLine = true,
        visualTransformation =
            if (passwordVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
        trailingIcon = {
            IconButton(
                onClick = { passwordVisible = !passwordVisible }
            ) {
                Icon(
                    imageVector =
                        if (passwordVisible) {
                            Icons.Default.VisibilityOff
                        } else {
                            Icons.Default.Visibility
                        },
                    contentDescription =
                        if (passwordVisible) "Hide password" else "Show password"
                )
            }
        }
    )
}
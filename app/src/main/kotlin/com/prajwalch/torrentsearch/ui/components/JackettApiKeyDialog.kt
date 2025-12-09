package com.prajwalch.torrentsearch.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.prajwalch.torrentsearch.R

/**
 * Dialog for adding/editing Jackett API configuration.
 */
@Composable
fun JackettApiKeyDialog(
    onDismiss: () -> Unit,
    onConfirm: (baseUrl: String, apiKey: String) -> Unit,
    title: String = "Add Jackett Provider",
    initialBaseUrl: String = "",
    initialApiKey: String = "",
    modifier: Modifier = Modifier,
) {
    var baseUrl by remember { mutableStateOf(initialBaseUrl) }
    var apiKey by remember { mutableStateOf(initialApiKey) }

    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = baseUrl,
                    onValueChange = { baseUrl = it },
                    label = { Text("Jackett Base URL") },
                    placeholder = { Text("http://192.168.1.x:9117") },
                    singleLine = true,
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("API Key") },
                    placeholder = { Text("Your Jackett API key") },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            Button(
                enabled = baseUrl.isNotBlank() && apiKey.isNotBlank(),
                onClick = { onConfirm(baseUrl.trim(), apiKey.trim()) },
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

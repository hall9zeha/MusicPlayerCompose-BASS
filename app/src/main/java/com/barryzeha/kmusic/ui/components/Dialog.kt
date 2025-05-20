package com.barryzeha.kmusic.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.barryzeha.kmusic.R

/****
 * Project KMusic
 * Created by Barry Zea H. on 18/04/25.
 * Copyright (c)  All rights reserved.
 ***/

@Composable
fun CustomDialog(
    title:String,
    onConfirm:()-> Unit,
    onDismiss:()-> Unit,
    confirmText:String= stringResource(R.string.accept),
    dismissText:String= stringResource(R.string.cancel),
    confirmEnabled:Boolean=true,
    properties: DialogProperties = DialogProperties(),
    content: @Composable () -> Unit,
    ){
    AlertDialog(
        title = { Text(title) },
        text = {
            Box(modifier= Modifier.padding(16.dp).fillMaxSize()){
                content()
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = confirmEnabled) { Text(confirmText) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(dismissText) } },
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.padding(vertical = 16.dp),
        properties = properties
    )
}

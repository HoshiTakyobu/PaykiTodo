package com.example.todoalarm.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun TodoCreateButton(onClick: () -> Unit, onBatchCreate: () -> Unit) {
    val configuration = LocalViewConfiguration.current
    val longPressConfiguration = remember(configuration) {
        object : ViewConfiguration by configuration {
            override val longPressTimeoutMillis: Long = 800L
        }
    }
    CompositionLocalProvider(LocalViewConfiguration provides longPressConfiguration) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            shadowElevation = 6.dp
        ) {
            Box(
                modifier = Modifier.size(56.dp).combinedClickable(
                    role = Role.Button,
                    onClickLabel = "创建待办",
                    onLongClickLabel = "批量添加待办",
                    onClick = onClick,
                    onLongClick = onBatchCreate
                ),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "创建待办", modifier = Modifier.size(26.dp))
            }
        }
    }
}

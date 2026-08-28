package com.canni.runpod.ui.nav

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AppDrawerContent(
    currentRoute: String,
    onSelect: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .padding(top = 12.dp),
    ) {
        Text(
            text = "RunPod",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
        )
        NavigationDrawerItem(
            icon = { Icon(Icons.Filled.Home, contentDescription = null) },
            label = { Text("Pods") },
            selected = currentRoute == Routes.PODS,
            onClick = { onSelect(Routes.PODS) },
        )
        NavigationDrawerItem(
            icon = { Icon(Icons.Filled.Send, contentDescription = null) },
            label = { Text("Serverless") },
            selected = currentRoute == Routes.SERVERLESS,
            onClick = { onSelect(Routes.SERVERLESS) },
        )
        NavigationDrawerItem(
            icon = { Icon(Icons.Filled.Search, contentDescription = null) },
            label = { Text("Hub") },
            selected = currentRoute == Routes.HUB,
            onClick = { onSelect(Routes.HUB) },
        )
        NavigationDrawerItem(
            icon = { Icon(Icons.Filled.ShoppingCart, contentDescription = null) },
            label = { Text("Billing") },
            selected = currentRoute == Routes.BILLING,
            onClick = { onSelect(Routes.BILLING) },
        )
        NavigationDrawerItem(
            icon = { Icon(Icons.Filled.Build, contentDescription = null) },
            label = { Text("Storage") },
            selected = currentRoute == Routes.STORAGE,
            onClick = { onSelect(Routes.STORAGE) },
        )
        NavigationDrawerItem(
            icon = { Icon(Icons.Filled.Lock, contentDescription = null) },
            label = { Text("Secrets") },
            selected = currentRoute == Routes.SECRETS,
            onClick = { onSelect(Routes.SECRETS) },
        )
        NavigationDrawerItem(
            icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
            label = { Text("Settings") },
            selected = currentRoute == Routes.SETTINGS,
            onClick = { onSelect(Routes.SETTINGS) },
        )
    }
}

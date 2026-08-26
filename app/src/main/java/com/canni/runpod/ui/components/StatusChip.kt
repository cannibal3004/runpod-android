package com.canni.runpod.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.canni.runpod.data.api.dto.PodStatus

@Composable
fun StatusChip(
    status: PodStatus,
    modifier: Modifier = Modifier,
) {
    val color = when (status) {
        PodStatus.RUNNING -> Color(0xFF4CAF50)
        PodStatus.PROVISIONING, PodStatus.STARTING -> Color(0xFFFFC107)
        PodStatus.EXITED, PodStatus.TERMINATED -> Color(0xFF9E9E9E)
        PodStatus.ERROR -> Color(0xFFF44336)
    }
    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(50),
    ) {
        Text(
            text = status.name,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
        )
    }
}

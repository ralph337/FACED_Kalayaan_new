package com.mswdo.kalayaan.faced.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mswdo.kalayaan.faced.ui.theme.*

@Composable
fun SectionHeader(title: String, subtitle: String? = null) {
    Column {
        Text(title, style = MaterialTheme.typography.headlineSmall, color = Slate900)
        if (subtitle != null) {
            Spacer(Modifier.height(2.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Slate500)
        }
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    tint: Color,
    tintBg: Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Slate200),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier.size(46.dp).clip(RoundedCornerShape(10.dp)).background(tintBg),
                contentAlignment = Alignment.Center
            ) { Icon(icon, contentDescription = null, tint = tint) }
            Column {
                Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = Slate500)
                Text(value, style = MaterialTheme.typography.headlineSmall, color = Slate800)
            }
        }
    }
}

@Composable
fun StatusPill(text: String, bg: Color, fg: Color) {
    Box(
        modifier = Modifier.clip(RoundedCornerShape(50)).background(bg).padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(text, color = fg, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun RoleBadge(isAdmin: Boolean) {
    StatusPill(
        text = if (isAdmin) "ADMIN" else "STAFF",
        bg = if (isAdmin) Emerald100 else Slate200,
        fg = if (isAdmin) Emerald700 else Slate700
    )
}

@Composable
fun EmptyState(icon: androidx.compose.ui.graphics.vector.ImageVector, message: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = null, tint = Slate300, modifier = Modifier.size(42.dp))
        Spacer(Modifier.height(8.dp))
        Text(message, color = Slate400, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun ProgressBarRow(label: String, percent: Int, occupied: Int, capacity: Int) {
    val barColor = when {
        percent >= 85 -> Red500
        percent >= 60 -> Amber500
        else -> Emerald500
    }
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = Slate500)
            Text("$occupied / $capacity", style = MaterialTheme.typography.labelMedium, color = Slate800)
        }
        Spacer(Modifier.height(4.dp))
        Box(
            Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)).background(Slate200)
        ) {
            Box(
                Modifier.fillMaxWidth(percent / 100f).height(8.dp).clip(RoundedCornerShape(50)).background(barColor)
            )
        }
    }
}

@Composable
fun FormSectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Slate200),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = Blue700)
            Divider(color = Slate200)
            content()
        }
    }
}

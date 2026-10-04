package com.example.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class DevicePreset(val label: String, val widthDp: Dp, val heightDp: Dp, val isLandscape: Boolean = false) {
    PHONE_PORTRAIT("Phone (390×800)", 360.dp, 640.dp, false),
    PHONE_LANDSCAPE("Landscape (700×380)", 680.dp, 360.dp, true),
    TABLET("Tablet (540×720)", 540.dp, 720.dp, false),
    FULLSCREEN("Fullscreen", Dp.Unspecified, Dp.Unspecified, false)
}

@Composable
fun DeviceFrame(
    preset: DevicePreset,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    if (preset == DevicePreset.FULLSCREEN) {
        Box(modifier = modifier.fillMaxSize()) {
            content()
        }
        return
    }

    Box(
        modifier = modifier
            .padding(12.dp)
            .animateContentSize(),
        contentAlignment = Alignment.Center
    ) {
        // Device Outer Bezel
        Surface(
            modifier = Modifier
                .width(preset.widthDp)
                .height(preset.heightDp)
                .shadow(16.dp, RoundedCornerShape(36.dp)),
            shape = RoundedCornerShape(36.dp),
            color = Color(0xFF0F172A),
            tonalElevation = 8.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp) // Phone outer border
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color.Black)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Simulated Status Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0B132B))
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "9:41",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )

                        // Camera Notch
                        Box(
                            modifier = Modifier
                                .width(60.dp)
                                .height(14.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.Black)
                                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF334155))
                            )
                        }

                        // Icons
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.SignalCellularAlt,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                            Icon(
                                Icons.Default.Wifi,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                            Icon(
                                Icons.Default.BatteryFull,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }

                    // Live Web App Content
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(Color(0xFF0F172A))
                    ) {
                        content()
                    }

                    // Bottom Home Bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0B132B))
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .width(96.dp)
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color.White.copy(alpha = 0.6f))
                        )
                    }
                }
            }
        }
    }
}

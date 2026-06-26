package com.example.ui.panels

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Preset
import com.example.ui.CommanderViewModel

@Composable
fun PresetPanel(
    viewModel: CommanderViewModel,
    presets: List<Preset>,
    activeGlowColor: Color,
    onSaveRequested: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFF1E293B)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "SYSTEM PRESETS & METADATA FLOW",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFF94A3B8),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = onSaveRequested,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = activeGlowColor.copy(alpha = 0.15f),
                        contentColor = activeGlowColor
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, activeGlowColor),
                    modifier = Modifier.testTag("save_preset_button")
                ) {
                    Icon(Icons.Filled.Save, contentDescription = "Save Preset Dialog", modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("SAVE SNAPSHOT", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                // Theme Glow Color Dot Selectors
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("LED GLOW", fontSize = 10.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
                    val themes = listOf(
                        "#00FFCC" to "Neon Green",
                        "#00E5FF" to "Volt Cyan",
                        "#FF007F" to "Plasma Pink",
                        "#FFD700" to "Retro Gold",
                        "#8A2BE2" to "Cyber Purple"
                    )
                    themes.forEach { (hex, name) ->
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color(android.graphics.Color.parseColor(hex)))
                                .border(
                                    width = if (viewModel.themeGlowColor == hex) 2.dp else 0.dp,
                                    color = Color.White,
                                    shape = CircleShape
                                )
                                .clickable {
                                    viewModel.themeGlowColor = hex
                                    viewModel.logMessage("[THEME] Coordinated active stage color to $name")
                                }
                                .testTag("theme_color_dot_$name")
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            if (presets.isEmpty()) {
                Text(
                    text = "No saved presets found. Save a system snapshot to store your EQ & strobe speed.",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.3f),
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(presets) { preset ->
                        val isCurrentlyApplied = viewModel.bpmVal == preset.bpm && viewModel.faderLow == preset.faderLow
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isCurrentlyApplied) Color.White.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.03f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (isCurrentlyApplied) activeGlowColor.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.05f)
                            ),
                            modifier = Modifier
                                .clickable { viewModel.applyPreset(preset) }
                                .testTag("preset_pill_${preset.id}")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = preset.name.uppercase(),
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.width(8.dp))
                                IconButton(
                                    onClick = { viewModel.deletePreset(preset) },
                                    modifier = Modifier.size(16.dp).testTag("delete_preset_${preset.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "Delete prescription preset",
                                        tint = Color.Red.copy(alpha = 0.7f),
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

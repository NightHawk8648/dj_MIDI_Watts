package com.example.ui.panels

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.CommanderViewModel
import com.example.VerticalFaderColumn

@Composable
fun EqualizerPanel(
    viewModel: CommanderViewModel,
    activeGlowColor: Color,
    modifier: Modifier = Modifier
) {
    // Labels corresponding to each selection
    val labels3 = listOf("BASS", "MID", "TREBLE")
    val labels5 = listOf("SUB", "LOW", "MID", "VOCAL", "TREBLE")
    val labels7 = listOf("SUB", "LOW", "L-MID", "MID", "H-MID", "HIGH", "PRESENCE")
    val labels10 = listOf("31", "62", "125", "250", "500", "1K", "2K", "4K", "8K", "16K")

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFF1E293B)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Title & Dynamic sync
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "DAW AUDIO EQ & EFFECT SUITE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF94A3B8)
                    )
                    Text(
                        text = "CHOOSE BAND ALLOCATION & MASTER CHANNELS",
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White.copy(alpha = 0.4f)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "BPM: ${viewModel.bpmVal}",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = activeGlowColor,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(8.dp))
                    Switch(
                        checked = viewModel.isBpmSyncOn,
                        onCheckedChange = {
                            viewModel.isBpmSyncOn = it
                            viewModel.logMessage("[SYNC] BPM Sync ${if (it) "ENABLED" else "DISABLED"}")
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = activeGlowColor,
                            checkedTrackColor = activeGlowColor.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier
                            .scale(0.7f)
                            .testTag("eq_bpm_sync_toggle")
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // --- BAND SPECIFIER SWITCHERS (3, 5, 7, 10) ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF020617), RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val bandCountOptions = listOf(3, 5, 7, 10)
                bandCountOptions.forEach { count ->
                    val isSelected = viewModel.eqBandCount == count
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(30.dp)
                            .background(
                                color = if (isSelected) activeGlowColor.copy(alpha = 0.15f) else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                viewModel.eqBandCount = count
                                viewModel.logMessage("[DAW] Rescaled Equalisers grid to: $count Bands Mode")
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$count-BAND",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) activeGlowColor else Color.White.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // -- ACTIVE BANDS ROW DISPLAY --
            val activeBandsValues = when (viewModel.eqBandCount) {
                3 -> viewModel.eq3Bands
                7 -> viewModel.eq7Bands
                10 -> viewModel.eq10Bands
                else -> viewModel.eq5Bands
            }

            val activeLabels = when (viewModel.eqBandCount) {
                3 -> labels3
                7 -> labels7
                10 -> labels10
                else -> labels5
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                activeBandsValues.forEachIndexed { idx, valItem ->
                    val label = activeLabels.getOrNull(idx) ?: "BAND"
                    val testTagStr = "eq_band_${viewModel.eqBandCount}_$idx"

                    VerticalFaderColumn(
                        label = label,
                        value = valItem,
                        color = activeGlowColor,
                        onValueChange = { newVal ->
                            viewModel.updateEqBandValue(idx, newVal)
                        },
                        modifier = Modifier.weight(1f),
                        testTag = testTagStr
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // --- HIGH PASS FILTER (HPF) & LOW PASS FILTER (LPF) OPTIONS ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // High Pass Filter Card Box
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(Color(0xFF0A0F1D), RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp))
                        .padding(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "HIGH PASS",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Switch(
                            checked = viewModel.hpfEnabled,
                            onCheckedChange = {
                                viewModel.hpfEnabled = it
                                viewModel.logMessage("[AUDIO] High-Pass filter ${if (it) "BYPASSED ON - Freq: ${viewModel.hpfFrequency.toInt()}Hz" else "OFF"}")
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = activeGlowColor,
                                checkedTrackColor = activeGlowColor.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier.scale(0.55f).testTag("hpf_toggle")
                        )
                    }
                    Text(
                        text = if (viewModel.hpfEnabled) "${viewModel.hpfFrequency.toInt()} Hz" else "BYPASSED",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = if (viewModel.hpfEnabled) activeGlowColor else Color.Gray,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    Slider(
                        value = (viewModel.hpfFrequency - 20f) / 980f,
                        onValueChange = {
                            viewModel.hpfFrequency = 20f + (it * 980f)
                            if (viewModel.hpfEnabled) {
                                viewModel.logMessage("[AUDIO] HPF updated to: ${viewModel.hpfFrequency.toInt()}Hz")
                            }
                        },
                        enabled = viewModel.hpfEnabled,
                        colors = SliderDefaults.colors(
                            thumbColor = activeGlowColor,
                            activeTrackColor = activeGlowColor,
                            inactiveTrackColor = Color.DarkGray
                        ),
                        modifier = Modifier.fillMaxWidth().height(16.dp).testTag("hpf_slider")
                    )
                }

                // Low Pass Filter Card Box
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(Color(0xFF0A0F1D), RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp))
                        .padding(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "LOW PASS",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Switch(
                            checked = viewModel.lpfEnabled,
                            onCheckedChange = {
                                viewModel.lpfEnabled = it
                                viewModel.logMessage("[AUDIO] Low-Pass filter ${if (it) "ACTIVE - Freq: ${viewModel.lpfFrequency.toInt()}Hz" else "OFF"}")
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = activeGlowColor,
                                checkedTrackColor = activeGlowColor.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier.scale(0.55f).testTag("lpf_toggle")
                        )
                    }
                    Text(
                        text = if (viewModel.lpfEnabled) "${viewModel.lpfFrequency.toInt()} Hz" else "BYPASSED",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = if (viewModel.lpfEnabled) activeGlowColor else Color.Gray,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    Slider(
                        value = (viewModel.lpfFrequency - 500f) / 19500f,
                        onValueChange = {
                            viewModel.lpfFrequency = 500f + (it * 19500f)
                            if (viewModel.lpfEnabled) {
                                viewModel.logMessage("[AUDIO] LPF updated to: ${viewModel.lpfFrequency.toInt()}Hz")
                            }
                        },
                        enabled = viewModel.lpfEnabled,
                        colors = SliderDefaults.colors(
                            thumbColor = activeGlowColor,
                            activeTrackColor = activeGlowColor,
                            inactiveTrackColor = Color.DarkGray
                        ),
                        modifier = Modifier.fillMaxWidth().height(16.dp).testTag("lpf_slider")
                    )
                }
            }
        }
    }
}

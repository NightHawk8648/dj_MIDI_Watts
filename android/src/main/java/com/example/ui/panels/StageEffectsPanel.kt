package com.example.ui.panels

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.CommanderViewModel

@Composable
fun StageEffectsPanel(
    viewModel: CommanderViewModel,
    activeGlowColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFF1E293B)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Section Title for Separated Visual Effects Frame
            Text(
                text = "LIGHTING ENGINE & VISUAL EFFECTS GRID",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFF94A3B8),
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // 2x2 Stage Visual FX Grid
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // STROBE (Visual FX)
                    val activeStrobeColor = Color(0xFFFF007F)
                    Button(
                        onClick = {
                            viewModel.isStrobeActive = !viewModel.isStrobeActive
                            viewModel.logMessage("[DMX] Strobe pulse manually toggled: ${viewModel.isStrobeActive}")
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (viewModel.isStrobeActive) activeStrobeColor.copy(alpha = 0.25f) else Color(0x1AFF007F),
                            contentColor = activeStrobeColor
                        ),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, if (viewModel.isStrobeActive) activeStrobeColor else activeStrobeColor.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .weight(1f)
                            .height(60.dp)
                            .testTag("visual_strobe_btn")
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.FlashOn, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("DMX STROBE", fontSize = 10.sp, fontWeight = FontWeight.Black)
                            Text("FAST LIGHT TRANSIT", fontSize = 8.sp, fontFamily = FontFamily.Monospace, color = activeStrobeColor.copy(alpha = 0.6f))
                        }
                    }

                    // FOGGER (Visual FX)
                    val activeFogColor = Color(0xFFF59E0B)
                    Button(
                        onClick = { viewModel.triggerFogQuick(1500) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (viewModel.isFogActive) activeFogColor.copy(alpha = 0.25f) else Color(0x1AF59E0B),
                            contentColor = activeFogColor
                        ),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, if (viewModel.isFogActive) activeFogColor else activeFogColor.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .weight(1f)
                            .height(60.dp)
                            .testTag("visual_fog_btn")
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.BlurOn, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("DMX FOGGER", fontSize = 10.sp, fontWeight = FontWeight.Black)
                            Text("GENERATE ATMOSPHERE", fontSize = 8.sp, fontFamily = FontFamily.Monospace, color = activeFogColor.copy(alpha = 0.6f))
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // WASH LIGHT (Visual FX)
                    val activeWashColor = Color(0xFF3B82F6)
                    var washActive by remember { mutableStateOf(false) }
                    Button(
                        onClick = {
                            washActive = !washActive
                            viewModel.logMessage("[DMX] Stage wash flood: ${if (washActive) "ENABLED RGBW CH 4" else "OFF"}")
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (washActive) activeWashColor.copy(alpha = 0.25f) else Color(0x1A3B82F6),
                            contentColor = activeWashColor
                        ),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, if (washActive) activeWashColor else activeWashColor.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .weight(1f)
                            .height(60.dp)
                            .testTag("visual_wash_btn")
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.Highlight, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("RGB FLOODS", fontSize = 10.sp, fontWeight = FontWeight.Black)
                            Text("AMB WASH FLOODS", fontSize = 8.sp, fontFamily = FontFamily.Monospace, color = activeWashColor.copy(alpha = 0.6f))
                        }
                    }

                    // LASERS (Visual FX)
                    val activeLaserColor = Color(0xFF8B5CF6)
                    Button(
                        onClick = { viewModel.triggerLaserQuick() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (viewModel.isLaserActive) activeLaserColor.copy(alpha = 0.25f) else Color(0x1A8B5CF6),
                            contentColor = activeLaserColor
                        ),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, if (viewModel.isLaserActive) activeLaserColor else activeLaserColor.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .weight(1f)
                            .height(60.dp)
                            .testTag("visual_laser_btn")
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.Radio, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("LASER SWEEP", fontSize = 10.sp, fontWeight = FontWeight.Black)
                            Text("GEOMETRIC SWEEPS", fontSize = 8.sp, fontFamily = FontFamily.Monospace, color = activeLaserColor.copy(alpha = 0.6f))
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Section Title for Separated Audio synthesizers & MIDI controls
            Text(
                text = "DAW CORES & AUDIO SIGNAL PARAMETERS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFF94A3B8),
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Reverb slider to adjust audio effects
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SYNTH REVERB WETLEVEL",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White
                    )
                    Text(
                        text = "${(viewModel.reverbAmount * 100).toInt()}% WET",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        color = activeGlowColor
                    )
                }
                Slider(
                    value = viewModel.reverbAmount,
                    onValueChange = {
                        viewModel.reverbAmount = it
                        if (it % 0.25f < 0.02f) {
                            viewModel.logMessage("[DAW] Synth space wet fader: ${(it * 100).toInt()}%")
                        }
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = activeGlowColor,
                        activeTrackColor = activeGlowColor,
                        inactiveTrackColor = Color.DarkGray
                    ),
                    modifier = Modifier.fillMaxWidth().height(24.dp).testTag("audio_reverb_slider")
                )
            }

            Spacer(Modifier.height(8.dp))

            // MIDI Simulation Controllers
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "MIDI PORT: ${viewModel.selectedMidiDevice}",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White.copy(alpha = 0.5f)
                )
                var midiDropdownExpanded by remember { mutableStateOf(false) }
                Box {
                    Text(
                        text = "[SELECT OUTPORT]",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = activeGlowColor,
                        modifier = Modifier
                            .clickable { midiDropdownExpanded = true }
                            .padding(4.dp)
                    )
                    DropdownMenu(
                        expanded = midiDropdownExpanded,
                        onDismissRequest = { midiDropdownExpanded = false }
                    ) {
                        viewModel.availableMidiDevices.forEach { dev ->
                            DropdownMenuItem(
                                text = { Text(dev, fontSize = 12.sp, fontFamily = FontFamily.Monospace) },
                                onClick = {
                                    viewModel.selectedMidiDevice = dev
                                    viewModel.logMessage("[MIDI] Swapped physical midi link: $dev")
                                    midiDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // Virtual piano triggers separated as Visual and Audio notes
            Text(
                text = "HYBRID INTERACTIVE INSTRUMENT TRIGGER PADS",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = Color.LightGray.copy(alpha = 0.4f),
                modifier = Modifier.padding(bottom = 6.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    Triple(60, "VISUAL", "[ 60 ] LASERS"),
                    Triple(61, "VISUAL", "[ 61 ] STROBING"),
                    Triple(62, "AUDIO", "[ 62 ] SYNTH PAD")
                ).forEach { (note, category, label) ->
                    val isVisual = category == "VISUAL"
                    val highlightColor = if (isVisual) Color(0xFFFF007F) else Color(0xFF3B82F6)

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(55.dp)
                            .background(Color(0xFF020617), RoundedCornerShape(12.dp))
                            .border(1.dp, highlightColor.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                            .clickable { viewModel.simulateMidiNotePad(note) }
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = highlightColor,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = category,
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color.White.copy(alpha = 0.4f)
                            )
                        }
                    }
                }
            }
        }
    }
}

package com.example.ui.panels

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.CommanderViewModel
import kotlin.math.sin

@Composable
fun MediaPlayerPanel(
    viewModel: CommanderViewModel,
    activeGlowColor: Color,
    modifier: Modifier = Modifier
) {
    // Media progress ticker internally synced so visual progress is super smooth
    var tickCounter by remember { mutableStateOf(0) }
    LaunchedEffect(viewModel.isMediaPlayerPlaying) {
        if (viewModel.isMediaPlayerPlaying) {
            while (true) {
                kotlinx.coroutines.delay(100)
                tickCounter++
                val totalSec = viewModel.currentTrack.durationSec
                if (totalSec > 0) {
                    val step = 0.1f / totalSec
                    viewModel.mediaPlayerProgress = (viewModel.mediaPlayerProgress + step).coerceIn(0f, 1f)
                    if (viewModel.mediaPlayerProgress >= 1f) {
                        viewModel.skipForward()
                    }
                }
            }
        }
    }

    val elapsedSeconds = (viewModel.mediaPlayerProgress * viewModel.currentTrack.durationSec).toInt()
    val remMin = elapsedSeconds / 60
    val remSec = elapsedSeconds % 60
    val timeElapsedStr = String.format("%02d:%02d", remMin, remSec)

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1527)),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color(0xFF1E293B)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.MusicNote,
                        contentDescription = "Media Player",
                        tint = activeGlowColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "DECK A - DIGITAL MEDIA PLAYER",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable { viewModel.toggleSpotifyPreference() }
                        .padding(horizontal = 8.dp)
                ) {
                    Text(
                        text = "SPOTIFY",
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace,
                        color = if (viewModel.isSpotifyPreferred) Color(0xFF1DB954) else Color.Gray,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(4.dp))
                    Switch(
                        checked = viewModel.isSpotifyPreferred,
                        onCheckedChange = { viewModel.toggleSpotifyPreference() },
                        modifier = Modifier.scale(0.5f)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(
                            if (viewModel.isMediaPlayerPlaying) activeGlowColor.copy(alpha = 0.15f) else Color(0x33475569),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (viewModel.isMediaPlayerPlaying) activeGlowColor else Color.Gray)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = if (viewModel.isMediaPlayerPlaying) "ONLINE" else "STANDBY",
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace,
                        color = if (viewModel.isMediaPlayerPlaying) Color.White else Color.LightGray
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Main Info Frame & Waveforms
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Disk / Artwork Simulator
                val infiniteTransition = rememberInfiniteTransition(label = "disk")
                val rotationAngle by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(4000, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "rotation"
                )

                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF020617))
                        .border(1.dp, activeGlowColor.copy(alpha = 0.4f), CircleShape)
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Album,
                        contentDescription = "Spinning Tape",
                        tint = if (viewModel.isMediaPlayerPlaying) activeGlowColor else Color(0xFF475569),
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(Modifier.width(12.dp))

                // Track and Artist metadata
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = viewModel.currentTrack.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "STUDIO ARTIST : ${viewModel.currentTrack.artist}",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF64748B),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "TEMPO CONFIG : ${viewModel.currentTrack.bpm} BPM / STEREO 48KHZ",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        color = activeGlowColor.copy(alpha = 0.7f)
                    )
                }

                // Audio Mini Spectrum reactive to EQ values
                Row(
                    modifier = Modifier
                        .height(30.dp)
                        .width(60.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    val bars = 8
                    for (i in 0 until bars) {
                        val baseHeight = if (viewModel.isMediaPlayerPlaying) {
                            if (viewModel.isAudioVisualSyncOn) {
                                // calculate pulsing dynamic height related to active channels
                                val factor = when (i % 3) {
                                    0 -> viewModel.faderSub
                                    1 -> viewModel.faderMid
                                    else -> viewModel.faderHigh
                                }
                                (10 + factor * 20 * sin(tickCounter.toFloat() * 0.4f + i).coerceIn(-1.0f, 1.0f)).toInt()
                            } else {
                                (5 + sin(tickCounter.toFloat() * 0.6f + i) * 12 + 10).toInt()
                            }
                        } else {
                            2
                        }
                        val h = baseHeight.coerceIn(2, 30).dp
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(h)
                                .background(activeGlowColor.copy(alpha = if (viewModel.isMediaPlayerPlaying) 0.8f else 0.2f), RoundedCornerShape(1.dp))
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Progress Slider Bar
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = timeElapsedStr,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFFE2E8F0)
                    )
                    Text(
                        text = viewModel.currentTrack.duration,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF64748B)
                    )
                }

                Slider(
                    value = viewModel.mediaPlayerProgress,
                    onValueChange = {
                        viewModel.mediaPlayerProgress = it
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = activeGlowColor,
                        activeTrackColor = activeGlowColor,
                        inactiveTrackColor = Color(0xFF1E293B)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                        .testTag("media_player_progress_scrubber")
                )
            }

            Spacer(Modifier.height(4.dp))

            // Transport Playback Action Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Secondary switches
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.SyncAlt,
                        contentDescription = "Pulse Sync",
                        tint = if (viewModel.isAudioVisualSyncOn) activeGlowColor else Color.Gray,
                        modifier = Modifier
                            .size(16.dp)
                            .clickable {
                                viewModel.isAudioVisualSyncOn = !viewModel.isAudioVisualSyncOn
                                viewModel.logMessage("[MEDIA] Reactive A/V Synced is ${if (viewModel.isAudioVisualSyncOn) "ON" else "OFF"}")
                            }
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "AV SYNC",
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace,
                        color = if (viewModel.isAudioVisualSyncOn) Color.White else Color.Gray,
                        modifier = Modifier.clickable {
                            viewModel.isAudioVisualSyncOn = !viewModel.isAudioVisualSyncOn
                            viewModel.logMessage("[MEDIA] Reactive A/V Synced is ${if (viewModel.isAudioVisualSyncOn) "ON" else "OFF"}")
                        }
                    )
                }

                // Primary media buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Skip back
                    IconButton(
                        onClick = { viewModel.skipBackward() },
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFF020617), RoundedCornerShape(10.dp))
                            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(10.dp))
                            .testTag("media_prev_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SkipPrevious,
                            contentDescription = "Previous Track",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Play Pause
                    IconButton(
                        onClick = { viewModel.togglePlayPause() },
                        modifier = Modifier
                            .size(46.dp)
                            .background(
                                if (viewModel.isMediaPlayerPlaying) activeGlowColor else Color(0xFF020617),
                                RoundedCornerShape(12.dp)
                            )
                            .border(1.dp, if (viewModel.isMediaPlayerPlaying) activeGlowColor else Color(0xFF334155), RoundedCornerShape(12.dp))
                            .testTag("media_play_btn")
                    ) {
                        Icon(
                            imageVector = if (viewModel.isMediaPlayerPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = "Toggle Audio Deck",
                            tint = if (viewModel.isMediaPlayerPlaying) Color.Black else Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Skip next
                    IconButton(
                        onClick = { viewModel.skipForward() },
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFF020617), RoundedCornerShape(10.dp))
                            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(10.dp))
                            .testTag("media_next_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SkipNext,
                            contentDescription = "Next Track",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Playlist index brief
                Text(
                    text = "SW-CH ${viewModel.currentTrackIndex + 1}/${viewModel.mediaPlaylist.size}",
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF475569)
                )
            }
        }
    }
}

package com.example.ui.panels

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.CommanderViewModel
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun VideoPlayerPanel(
    viewModel: CommanderViewModel,
    activeGlowColor: Color,
    modifier: Modifier = Modifier
) {
    var aspectRatioMode by remember { mutableStateOf("16:9") } // "16:9", "4:3", "Cinema"
    var videoResolution by remember { mutableStateOf("1080p") } // "720p", "1080p", "2160p"
    var isTheaterMode by remember { mutableStateOf(false) }

    // Automatically switch scenes based on BPM tempo ranges when Sync is active
    LaunchedEffect(viewModel.bpmVal, viewModel.isBpmSyncOn) {
        if (viewModel.isBpmSyncOn) {
            val targetMode = when {
                viewModel.bpmVal < 100 -> 2 // Matrix Lattice for chill/ambient
                viewModel.bpmVal in 100..145 -> 0 // Cyber Helix for house/techno
                else -> 1 // Plasma Fluid for high-tempo energy
            }
            viewModel.transitionToVisualMode(targetMode)
        }
    }

    // Visual frame counter/ticker
    var videoTicker by remember { mutableStateOf(0L) }
    LaunchedEffect(viewModel.isMediaPlayerPlaying) {
        while (true) {
            val delayMs = if (viewModel.isMediaPlayerPlaying) 16L else 32L // Higher frame rate when active
            delay(delayMs)
            videoTicker += 1
        }
    }

    // Infrastructure 'Grid Pulse' Animation - Reacts to Network Latency
    val infiniteTransition = rememberInfiniteTransition()
    val pulseGlow by infiniteTransition.animateColor(
        initialValue = activeGlowColor.copy(alpha = 0.1f),
        targetValue = activeGlowColor.copy(alpha = 0.6f),
        animationSpec = infiniteRepeatable(
            animation = tween<Color>(
                durationMillis = (1500 / (1 + viewModel.networkPingMs / 30f)).toInt().coerceIn(100, 1500),
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ), label = "PulseGlow"
    )

    // Dynamic resolution metrics
    val fps = if (viewModel.isMediaPlayerPlaying) 60 else 30
    val aspectHeight = when (aspectRatioMode) {
        "4:3" -> 220.dp
        "Cinema" -> 140.dp
        else -> 180.dp // 16:9
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(
            width = 1.dp,
            // The border pulses with the grid infrastructure health
            brush = Brush.radialGradient(listOf(pulseGlow, Color(0xFF1E293B)))
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Info Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Videocam,
                        contentDescription = "Video Visual Projection",
                        tint = activeGlowColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "DECK B - VISUAL PROJECTION PLAYBACK",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = activeGlowColor.copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, activeGlowColor.copy(alpha = 0.2f)),
                    modifier = Modifier.clickable { isTheaterMode = !isTheaterMode }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isTheaterMode) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                            contentDescription = "Theater View",
                            tint = activeGlowColor,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = if (isTheaterMode) "MINIMIZE" else "THEATER",
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = activeGlowColor
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Main Kinetic Video Render Screen
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(aspectHeight)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF020617))
                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(16.dp))
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = {
                                isTheaterMode = !isTheaterMode
                            },
                            onTap = {
                                // Cycle visual themes
                                viewModel.transitionToVisualMode((viewModel.visualMode + 1) % 3)
                            }
                        )
                    }
            ) {
                // Procedural Rendering Engine Canvas
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height
                    val centerX = canvasWidth / 2f
                    val centerY = canvasHeight / 2f
                    val diagRadius = centerX.coerceAtLeast(centerY)

                    // Retrieve synchronized audio energy states
                    val lowEnergy = viewModel.faderLow.coerceIn(0.1f, 1f)
                    val subEnergy = viewModel.faderSub.coerceIn(0.1f, 1f)
                    val midEnergy = viewModel.faderMid.coerceIn(0.1f, 1f)
                    val highEnergy = viewModel.faderHigh.coerceIn(0.1f, 1f)

                    // Master speed scaling factor
                    val speedScalar = if (viewModel.isMediaPlayerPlaying) (viewModel.bpmVal / 120f) else 0.2f
                    val t = videoTicker * 0.05f * speedScalar

                    when (viewModel.visualMode) {
                        0 -> { // THEME A: NEON CYBER HELIX TUNNEL
                            val ringCount = 12
                            for (i in 0 until ringCount) {
                                // Depth perspective coefficient
                                val depth =RingFraction(i, ringCount, t)
                                val radius = depth * diagRadius * 0.95f
                                val alpha = (1f - depth) * (0.3f + lowEnergy * 0.7f)
                                
                                // Rotate the wireframe polygons offset
                                val rot = t * 0.6f + i * 0.2f
                                val vertices = 5
                                val points = mutableListOf<Offset>()
                                for (v in 0 until vertices) {
                                    val angle = (v * 2 * Math.PI / vertices) + rot
                                    val px = centerX + cos(angle).toFloat() * radius
                                    val py = centerY + sin(angle).toFloat() * radius
                                    points.add(Offset(px, py))
                                }

                                // Draw rings connecting line points
                                for (p in 0 until vertices) {
                                    val nextIdx = (p + 1) % vertices
                                    drawLine(
                                        color = activeGlowColor.copy(alpha = alpha.coerceIn(0f, 1f)),
                                        start = points[p],
                                        end = points[nextIdx],
                                        strokeWidth = (2f + (1f - depth) * 4f).coerceAtLeast(1f)
                                    )
                                }
                            }
                        }
                        1 -> { // THEME B: PLASMA RETRO VECTOR FLUID
                            val blobs = 4
                            for (i in 0 until blobs) {
                                val offsetRadX = centerX * 0.4f * sin(t * 0.4f + i * 1.5f)
                                val offsetRadY = centerY * 0.4f * cos(t * 0.3f + i * 2.1f)
                                val baseRadius = (50.dp.toPx() + midEnergy * 60.dp.toPx()) * (1f + 0.25f * sin(t * 1.5f + i))

                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            activeGlowColor.copy(alpha = 0.4f * midEnergy),
                                            Color.Magenta.copy(alpha = 0.15f * subEnergy),
                                            Color.Transparent
                                        ),
                                        center = Offset(centerX + offsetRadX, centerY + offsetRadY),
                                        radius = baseRadius
                                    ),
                                    center = Offset(centerX + offsetRadX, centerY + offsetRadY),
                                    radius = baseRadius
                                )
                            }
                        }
                        2 -> { // THEME C: QUANTUM PARTICLE LATTICE
                            val dotsX = 8
                            val dotsY = 6
                            val spacingX = canvasWidth / (dotsX + 1)
                            val spacingY = canvasHeight / (dotsY + 1)

                            for (x in 1 .. dotsX) {
                                for (y in 1 .. dotsY) {
                                    val px = x * spacingX
                                    val py = y * spacingY

                                    // Pertub coordinate based on high frequencies and beat pulse
                                    val dx = sin(t * 1.2f + x * 0.5f + y * 0.3f) * 15f * highEnergy
                                    val dy = cos(t * 0.9f + x * 0.2f + y * 0.6f) * 15f * highEnergy
                                    val sizeVal = (3f + highEnergy * 8f) * (1f + 0.5f * sin(t * 2.1f + (x + y)))

                                    drawCircle(
                                        color = if ((x + y) % 2 == 0) activeGlowColor else Color.Cyan,
                                        center = Offset(px + dx, py + dy),
                                        radius = sizeVal.dp.toPx()
                                    )
                                }
                            }
                        }
                    }

                    // Superimpose DMX Hardware FX Simulation Filters
                    // 1. STROBE FLASH
                    if (viewModel.isStrobeActive) {
                        val flashPeriod = 60000 / viewModel.strobeSpeedBpm.coerceAtLeast(1)
                        val halfPeriod = (flashPeriod / 2).coerceAtLeast(1)
                        val isFlashFrame = ((videoTicker * 16) / halfPeriod) % 2L == 0L
                        if (isFlashFrame) {
                            drawRect(
                                color = Color.White.copy(alpha = 0.35f),
                                size = size
                            )
                        }
                    }

                    // 2. LASER BURST BEAM REFLECTION
                    if (viewModel.isLaserActive) {
                        val beamCount = 3
                        for (idx in 0 until beamCount) {
                            val sweepX = centerX + sin(t * 1.5f + idx * 1.1f) * (centerX * 0.9f)
                            drawLine(
                                color = Color.Magenta.copy(alpha = 0.6f),
                                start = Offset(centerX, 0f),
                                end = Offset(sweepX, canvasHeight),
                                strokeWidth = 4.dp.toPx(),
                                cap = androidx.compose.ui.graphics.StrokeCap.Round
                            )
                        }
                    }

                    // 3. FOG AND SMOKE SCROLLING
                    if (viewModel.isFogActive || viewModel.fogDensity > 0.05f) {
                        val fogOffset = (videoTicker * 1.5f) % canvasHeight
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.White.copy(alpha = 0.15f),
                                    Color.LightGray.copy(alpha = 0.08f),
                                    Color.Transparent
                                ),
                                startY = 0f + fogOffset,
                                endY = canvasHeight + fogOffset
                            ),
                            size = size
                        )
                    }
                }

                // HUD overlay metrics
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = when (viewModel.visualMode) {
                                0 -> "SCENE: NEON TUNNEL"
                                1 -> "SCENE: RETRO PLASMA"
                                else -> "SCENE: QUANTUM GRID"
                            },
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Box(
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(if (viewModel.isMediaPlayerPlaying) activeGlowColor else Color.Gray)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "HD RENDER ${fps}FPS",
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(Modifier.width(8.dp))

                    Box(
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(pulseGlow)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "GRID PULSE: ${viewModel.networkPingMs}MS",
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                color = pulseGlow
                            )
                        }
                    }
                }

                // Interactive click-to-change overlay hint
                Text(
                    text = "TAP SCREEN TO CYCLE STYLES • DOUBLE TAP FULLSCREEN",
                    fontSize = 7.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                )

                Text(
                    text = videoResolution.uppercase() + " / AR:" + aspectRatioMode,
                    fontSize = 7.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    color = activeGlowColor.copy(alpha = 0.7f),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                )
            }

            Spacer(Modifier.height(12.dp))

            // Controls Grid: Mode, Aspect Ratio, Target resolutions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Resolution Selector
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "RENDER TARGET",
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF64748B),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF020617), RoundedCornerShape(10.dp))
                            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(10.dp))
                            .padding(2.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf("720p", "1080p", "2160p").forEach { res ->
                            val active = videoResolution == res
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (active) activeGlowColor.copy(alpha = 0.15f) else Color.Transparent)
                                    .clickable { videoResolution = res }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = res.uppercase(),
                                    fontSize = 8.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = if (active) activeGlowColor else Color.Gray
                                )
                            }
                        }
                    }
                }

                // Aspect Ratio Selector
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "ASPECT RATIO",
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF64748B),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF020617), RoundedCornerShape(10.dp))
                            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(10.dp))
                            .padding(2.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf("16:9", "4:3", "Cinema").forEach { mode ->
                            val active = aspectRatioMode == mode
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (active) activeGlowColor.copy(alpha = 0.15f) else Color.Transparent)
                                    .clickable { aspectRatioMode = mode }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = mode.uppercase(),
                                    fontSize = 8.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = if (active) activeGlowColor else Color.Gray
                                )
                            }
                        }
                    }
                }
            }

            // Expanded Theater Settings Window
            AnimatedVisibility(visible = isTheaterMode) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .background(Color(0xFF020617), RoundedCornerShape(14.dp))
                        .border(1.dp, activeGlowColor.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "AI-SYNCHRONIZED PROJECTION HUD",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = activeGlowColor
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "The render pipeline uses hardware buffer mapping for perfect MIDI clock synchronization. Render loop uses active UI memory layout to minimize latency to sub-3ms. In absolute dual-deck setup, this projection can feed directly onto live video wall setups via wireless DMX controllers.",
                        fontSize = 9.sp,
                        color = Color.LightGray.copy(alpha = 0.7f),
                        lineHeight = 12.sp
                    )
                }
            }
        }
    }
}

// Return mathematical fraction for neon rings wrapping depth movement
private fun RingFraction(index: Int, total: Int, t: Float): Float {
    val phase = (index.toFloat() / total + t * 0.1f) % 1.0f
    return if (phase < 0f) phase + 1f else phase
}

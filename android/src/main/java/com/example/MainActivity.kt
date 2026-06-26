package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.djmidiwatts.BuildConfig
import com.example.data.Preset
import com.example.ui.CommanderViewModel
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.panels.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.delay
import kotlin.math.sin

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0F1117) // Custom deep Professional Polish slate dark canvas
                ) {
                    CommanderScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommanderScreen() {
    val viewModel: CommanderViewModel = viewModel()
    val presets by viewModel.allPresets.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    // Screen Ticker for fluid visualizer and strobe calculations
    var tickerTime by remember { mutableStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) {
            tickerTime += 16
            delay(16)
        }
    }

    // Dynamic color parsing helper
    val activeGlowColor = remember(viewModel.themeGlowColor) {
        try {
            Color(android.graphics.Color.parseColor(viewModel.themeGlowColor))
        } catch (e: Exception) {
            Color(0xFF00FFCC) // Fallback green neon
        }
    }

    // State for preset name dialog
    var showSaveDialog by remember { mutableStateOf(false) }
    var presetNameInput by remember { mutableStateOf("") }

    // Scroll automatically to bottom of terminal logs
    val logsListState = rememberLazyListState()
    LaunchedEffect(viewModel.hardwareLogs.size) {
        if (viewModel.hardwareLogs.isNotEmpty()) {
            logsListState.scrollToItem(viewModel.hardwareLogs.size - 1)
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(end = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "ULTIMA-GRID V1.0.0",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = activeGlowColor,
                                letterSpacing = 1.5.sp,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                             Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .background(Color(0xFF1E293B), RoundedCornerShape(12.dp))
                                    .border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(activeGlowColor)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "SYSTEM READY",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFFE2E8F0)
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .background(Color(0xFF0F202A), RoundedCornerShape(12.dp))
                                    .border(1.dp, activeGlowColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Language,
                                    contentDescription = "Web Portal",
                                    tint = activeGlowColor,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "WEB REMOTE: http://${viewModel.localIpAddress}:${BuildConfig.WEB_PORT}",
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = String.format("%.1f", viewModel.bpmVal.toFloat()),
                                    fontSize = 22.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Black,
                                    color = activeGlowColor,
                                    letterSpacing = (-1).sp
                                )
                                Text(
                                    text = "BPM SYNC",
                                    fontSize = 8.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF64748B),
                                    letterSpacing = 1.sp
                                )
                            }
                            IconButton(
                                onClick = { viewModel.logMessage("[BRIDGE] Re-scanned Web MIDI peripherals.") },
                                modifier = Modifier.testTag("midi_refresh")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Refresh,
                                    contentDescription = "Sync MIDI",
                                    tint = activeGlowColor
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0F1117),
                    titleContentColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFF0F1117))
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- HEADER: PRESET SELECTOR & SYSTEM GLOW COLOR LEDS ---
            PresetPanel(
                viewModel = viewModel,
                presets = presets,
                activeGlowColor = activeGlowColor,
                onSaveRequested = { showSaveDialog = true }
            )

            // --- BASS-STRIP BLOCK: AUDIO FADERS / SLIDERS ---
            EqualizerPanel(
                viewModel = viewModel,
                activeGlowColor = activeGlowColor
            )

            // --- VISUALIZER-STAGE BLOCK: REALTIME RENDERING CANVAS ---
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color(0xFF1E293B)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Flash / Strobe Pulse Filter
                    val isStrobePulseOn = remember(tickerTime, viewModel.isStrobeActive, viewModel.strobeSpeedBpm) {
                        val bpm = viewModel.strobeSpeedBpm.coerceAtLeast(1)
                        val flashPeriod = 60000 / bpm
                        val halfPeriod = (flashPeriod / 2).coerceAtLeast(1)
                        viewModel.isStrobeActive && ((tickerTime / halfPeriod) % 2 == 0L)
                    }

                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .drawBehind {
                                if (isStrobePulseOn) {
                                    drawRect(
                                        color = activeGlowColor.copy(alpha = 0.15f),
                                        size = size
                                    )
                                }
                            }
                    ) {
                        val canvasWidth = size.width
                        val canvasHeight = size.height
                        val midY = canvasHeight / 2f

                        // 1. Draw Simulated Laser Sweeps (High frequency projection beams)
                        if (viewModel.isLaserActive) {
                            val laserSweepX1 = canvasWidth / 2f + sin(tickerTime * 0.005f) * (canvasWidth / 2f)
                            val laserSweepX2 = canvasWidth / 2f + sin(tickerTime * 0.007f + 2f) * (canvasWidth / 2f)
                            drawLine(
                                color = Color.Magenta.copy(alpha = 0.7f),
                                start = Offset(canvasWidth / 2f, 0f),
                                end = Offset(laserSweepX1, canvasHeight),
                                strokeWidth = 3.dp.toPx()
                            )
                            drawLine(
                                color = Color.Cyan.copy(alpha = 0.7f),
                                start = Offset(canvasWidth / 2f, 0f),
                                end = Offset(laserSweepX2, canvasHeight),
                                strokeWidth = 3.dp.toPx()
                            )
                        }

                        // 2. Draw Simulated Floating Fog Particles (glowing overlapping clouds)
                        if (viewModel.isFogActive || viewModel.fogDensity > 0.05f) {
                            val cloudSpeed = tickerTime * 0.02f
                            val circles = listOf(
                                Offset(100f + cloudSpeed % canvasWidth, canvasHeight - 80f),
                                Offset(canvasWidth - (cloudSpeed * 0.8f) % canvasWidth, canvasHeight - 110f),
                                Offset((canvasWidth / 2f) + sin(tickerTime * 0.001f) * 150f, canvasHeight - 60f)
                            )
                            circles.forEach { offset ->
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            Color.White.copy(alpha = 0.25f),
                                            Color.LightGray.copy(alpha = 0.05f),
                                            Color.Transparent
                                        ),
                                        center = offset,
                                        radius = 120.dp.toPx()
                                    ),
                                    center = offset,
                                    radius = 120.dp.toPx()
                                )
                            }
                        }

                        // 3. Draw DJ Frequency bars dancing (scaled by faders low, mid, high)
                        val numBars = 20
                        val barSpacing = 8.dp.toPx()
                        val barWidth = ((canvasWidth - (barSpacing * (numBars + 1))) / numBars).coerceAtLeast(0f)

                        for (i in 0 until numBars) {
                            // Scale factor depending on frequency range
                            val scaleFactor = when {
                                i < 5 -> viewModel.faderSub * 0.8f + viewModel.faderLow * 0.2f // Sub-Bass
                                i < 11 -> viewModel.faderLow * 0.3f + viewModel.faderMid * 0.7f // Mid-vocal
                                i < 16 -> viewModel.focalScalar(viewModel.faderVocal, viewModel.faderMid)
                                else -> viewModel.faderHigh // Highs treble
                            }

                            // Dynamic formula using sinusoidal time ticking
                            val phase = i * 0.4f
                            val amplitude = (sin(tickerTime * 0.005f + phase) * 0.4f + 0.6f) * scaleFactor
                            val barHeight = (canvasHeight * 0.75f) * amplitude

                            val startX = barSpacing + i * (barWidth + barSpacing)
                            val startY = canvasHeight - barHeight

                            drawRoundRect(
                                color = activeGlowColor.copy(alpha = if (isStrobePulseOn) 0.9f else 0.7f),
                                topLeft = Offset(startX, startY),
                                size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                            )
                        }

                        // Draw neat geometric target lines
                        drawLine(
                            color = activeGlowColor.copy(alpha = 0.2f),
                            start = Offset(0f, canvasHeight * 0.25f),
                            end = Offset(canvasWidth, canvasHeight * 0.25f),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    // On-screen Visualizer Text Labels
                    Text(
                        text = "LIVE SPECTROGRAPH VISUALIZER STAGE",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                    )

                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (viewModel.isStrobeActive) {
                            VisualStatusIndicator(label = "STROBE ACTIVE", glowColor = Color.Yellow)
                        }
                        if (viewModel.isFogActive) {
                            VisualStatusIndicator(label = "FOG APPLIED", glowColor = Color.White)
                        }
                        if (viewModel.isLaserActive) {
                            VisualStatusIndicator(label = "LASER SWEEP", glowColor = Color.Magenta)
                        }
                    }
                }
            }

            // --- FX-GRID BLOCK: HARDWARE STAGE FX & VIRTUAL MIDI KEYBOARD ---
            StageEffectsPanel(
                viewModel = viewModel,
                activeGlowColor = activeGlowColor
            )

            // --- DECK B - PROCEDURAL VIDEO & CINEMATIC PROJECTION SYSTEM ---
            VideoPlayerPanel(
                viewModel = viewModel,
                activeGlowColor = activeGlowColor
            )

            // --- MEDIA DECK A REPLACES INTERACTIVE CONSOLE TERMINAL ---
            MediaPlayerPanel(
                viewModel = viewModel,
                activeGlowColor = activeGlowColor
            )

            // --- CO-COMMAND SIDEWALK ---
            AIPanel(
                viewModel = viewModel,
                activeGlowColor = activeGlowColor
            )
        }
    }

    // Modal dialog to save presets snapshot
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Save Snapshot Preset", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Provide a secure descriptive name for this hardware preset:", fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                    TextField(
                        value = presetNameInput,
                        onValueChange = { presetNameInput = it },
                        placeholder = { Text("e.g. Dubstep Blast, Cyan Lounge") },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(focusedIndicatorColor = activeGlowColor),
                        modifier = Modifier.testTag("preset_name_input_dialog")
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (presetNameInput.trim().isNotEmpty()) {
                            viewModel.savePresetSnapshot(presetNameInput.trim())
                            presetNameInput = ""
                            showSaveDialog = false
                        }
                    },
                    modifier = Modifier.testTag("confirm_save_preset")
                ) {
                    Text("SAVE", color = activeGlowColor)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("CANCEL")
                }
            }
        )
    }
}

// Inline Status LED indicator
@Composable
fun VisualStatusIndicator(label: String, glowColor: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(glowColor)
        )
        Text(
            text = label,
            fontSize = 8.sp,
            fontFamily = FontFamily.Monospace,
            color = Color.White
        )
    }
}

// Customized Neo Fader Bar with Numeric readout
@Composable
fun VerticalFaderColumn(
    label: String,
    value: Float,
    color: Color,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = ""
) {
    val currentOnValueChange by rememberUpdatedState(onValueChange)

    Column(
        modifier = modifier.testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            color = Color(0xFF94A3B8),
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        // Track container
        BoxWithConstraints(
            modifier = Modifier
                .width(42.dp)
                .height(130.dp)
                .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val heightPx = size.height.toFloat()
                            if (heightPx > 0f) {
                                val computed = 1f - (offset.y / heightPx)
                                currentOnValueChange(computed.coerceIn(0f, 1f))
                            }
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val heightPx = size.height.toFloat()
                            if (heightPx > 0f) {
                                val computed = 1f - (change.position.y / heightPx)
                                currentOnValueChange(computed.coerceIn(0f, 1f))
                            }
                        }
                    )
                }
        ) {
            val trackHeight = maxHeight
            val valueHeight = trackHeight * value

            // Value filled track
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(valueHeight)
                    .align(Alignment.BottomCenter)
                    .background(color.copy(alpha = 0.15f), RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
            )

            // Neon center vertical guide line
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .align(Alignment.Center)
                    .background(Color(0xFF1E293B), RoundedCornerShape(2.dp))
            )

            // Dynamic active color line showing current fill level
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(valueHeight)
                    .align(Alignment.BottomCenter)
                    .background(color, RoundedCornerShape(2.dp))
            )

            // Knob handles bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
                    .height(8.dp)
                    .align(Alignment.TopCenter)
                    .graphicsLayer(translationY = with(LocalDensity.current) { ((1f - value) * trackHeight.toPx()) - 4.dp.toPx() })
                    .background(color, RoundedCornerShape(2.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(2.dp))
            )
        }

        Spacer(Modifier.height(8.dp))

        // Numerical Percent Pill
        Box(
            modifier = Modifier
                .width(42.dp)
                .background(Color(0xFF0F172A), RoundedCornerShape(6.dp))
                .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${(value * 100).toInt()}%",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// Helper expansion block for ViewModel
fun CommanderViewModel.focalScalar(vocal: Float, mid: Float): Float {
    return vocal * 0.7f + mid * 0.3f
}

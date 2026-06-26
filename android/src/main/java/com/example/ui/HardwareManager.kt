package com.example.ui

import android.content.Context
import android.media.midi.*
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.ui.CommanderViewModel.AccountType

/**
 * The Hardware Manager: Gatekeeper of the Physical and Virtual Grid.
 * Handles MIDI routing, Audio Input authorization, and Paid Access tiers.
 */
class HardwareManager(private val viewModel: CommanderViewModel, private val context: Context) {

    /**
     * Device Mapping Library: Defines how physical controls map to Grid parameters.
     */
    data class DeviceProfile(
        val manufacturer: String,
        val model: String,
        val ccMap: Map<Int, String>,
        val noteMap: Map<Int, String>,
        val requiredFirmware: String = "1.0.0"
    )

    private val MAPPING_LIBRARY = mapOf(
        "Novation LaunchControl" to DeviceProfile(
            "Novation", "LaunchControl",
            ccMap = mapOf(21 to "low", 22 to "mid", 23 to "high", 24 to "vocal", 25 to "sub", 26 to "strobe", 27 to "fog"),
            noteMap = mapOf(9 to "laser", 10 to "strobe_pulse", 11 to "fog_pulse")
        ),
        "AKAI APC Mini" to DeviceProfile(
            "AKAI", "APC Mini",
            ccMap = mapOf(48 to "low", 49 to "mid", 50 to "high", 56 to "sub"),
            noteMap = mapOf(0 to "laser")
        ),
        "GENERIC_USB" to DeviceProfile(
            "Generic", "MIDI Device",
            ccMap = mapOf(20 to "low", 21 to "strobe", 22 to "sub", 23 to "fog"),
            noteMap = mapOf(60 to "laser", 61 to "strobe_pulse", 62 to "fog_pulse")
        )
    )

    private var activeProfile: DeviceProfile = MAPPING_LIBRARY["GENERIC_USB"]!!

    private val midiManager: MidiManager? = context.getSystemService(Context.MIDI_SERVICE) as? MidiManager
    
    // Driver / Firmware status tracking
    var driverStatus by mutableStateOf("SYSTEM_DRIVERS_OPTIMAL")
    var updateRequired by mutableStateOf(false)
    var updateUrl by mutableStateOf("")

    private val openPorts = mutableListOf<MidiOutputPort>()

    private val deviceCallback = object : MidiManager.DeviceCallback() {
        override fun onDeviceAdded(deviceInfo: MidiDeviceInfo) {
            openMidiDevice(deviceInfo)
        }
        override fun onDeviceRemoved(deviceInfo: MidiDeviceInfo) {
            viewModel.logMessage("[HARDWARE] Device detached: ${deviceInfo.properties.getString(MidiDeviceInfo.PROPERTY_NAME)}")
        }
    }

    private inner class GridMidiReceiver : MidiReceiver() {
        override fun onSend(data: ByteArray, offset: Int, count: Int, timestamp: Long) {
            var i = offset
            while (i < offset + count) {
                val status = data[i].toInt() and 0xFF
                if (status >= 0x80) { // Status byte detected
                    val type = status and 0xF0
                    if (i + 2 < offset + count) {
                        val d1 = data[i + 1].toInt() and 0x7F
                        val d2 = data[i + 2].toInt() and 0x7F
                        when (type) {
                            0x90 -> if (d2 > 0) handleMappedNote(d1) // Note On
                            0xB0 -> processControlSignal(d1, d2 / 127f) // Control Change
                        }
                        i += 3
                    } else i++
                } else i++
            }
        }
    }

    // Metadata for hardware capabilities
    var isPhysicalMidiDetected by mutableStateOf(false)
    var activeInputSource by mutableStateOf("VIRTUAL_EMULATOR")

    /**
     * Processes incoming control signals. 
     * This is where "Paid Access" levels are enforced.
     */
    fun processControlSignal(cc: Int, normalizedValue: Float) {
        val param = activeProfile.ccMap[cc] ?: return
        
        // Access Control Logic
        val canAccessHighRes = viewModel.accountType == AccountType.PREMIUM || viewModel.arePremiumFadersUnlocked
        
        val processedValue = if (!canAccessHighRes) {
            normalizedValue.coerceIn(0f, 0.8f) // Cap for Free/Basic users
        } else {
            normalizedValue
        }

        viewModel.logMessage("[HARDWARE] Signal CC_$cc -> $param: $processedValue")
        
        when (param) {
            "low" -> viewModel.faderLow = processedValue
            "mid" -> viewModel.faderMid = processedValue
            "high" -> viewModel.faderHigh = processedValue
            "vocal" -> viewModel.faderVocal = processedValue
            "sub" -> viewModel.faderSub = processedValue
            "strobe" -> viewModel.strobeSpeedBpm = 40 + (processedValue * 200).toInt()
            "fog" -> {
                viewModel.fogDensity = processedValue
                viewModel.isFogActive = processedValue > 0.05f
            }
        }
    }

    private fun handleMappedNote(note: Int) {
        // Basic users might only have access to 1 FX trigger pad; Premium gets the full grid.
        if (viewModel.accountType == AccountType.FREE && note > 60) {
            viewModel.logMessage("[SECURITY] Trigger Note $note restricted. Upgrade to unlock full Pad Grid.")
            viewModel.speak("Hardware restriction active. Upgrade required.")
            return
        }

        val action = activeProfile.noteMap[note] ?: return
        viewModel.logMessage("[HARDWARE] Trigger: $action")
        
        when (action) {
            "laser" -> viewModel.triggerLaserQuick()
            "strobe_pulse" -> {
                viewModel.isStrobeActive = true
                Handler(Looper.getMainLooper()).postDelayed({ viewModel.isStrobeActive = false }, 150)
            }
            "fog_pulse" -> viewModel.triggerFogQuick(1200)
        }
    }
    
    private fun openMidiDevice(deviceInfo: MidiDeviceInfo) {
        midiManager?.openDevice(deviceInfo, { device ->
            if (device == null) return@openDevice
            val name = deviceInfo.properties.getString(MidiDeviceInfo.PROPERTY_NAME) ?: "USB MIDI Interface"
            
            // Identify Profile
            activeProfile = MAPPING_LIBRARY.entries.find { name.contains(it.key, ignoreCase = true) }?.value 
                ?: MAPPING_LIBRARY["GENERIC_USB"]!!
            
            viewModel.logMessage("[HARDWARE] Link established: ${activeProfile.manufacturer} ${activeProfile.model}")
            checkDriverIntegrity(deviceInfo)
            
            for (i in 0 until deviceInfo.outputPortCount) {
                val port = device.openOutputPort(i)
                if (port != null) {
                    port.connect(GridMidiReceiver())
                    openPorts.add(port)
                }
            }
            
            isPhysicalMidiDetected = true
            activeInputSource = name
            viewModel.isMidiHardwareConnected = true
        }, Handler(Looper.getMainLooper()))
    }

    /**
     * Simulates a driver/firmware integrity check.
     */
    private fun checkDriverIntegrity(deviceInfo: MidiDeviceInfo) {
        val version = deviceInfo.properties.getString(MidiDeviceInfo.PROPERTY_VERSION) ?: "1.0.0"
        if (version < activeProfile.requiredFirmware) {
            driverStatus = "FIRMWARE_OUTDATED"
            updateRequired = true
            updateUrl = "https://support.${activeProfile.manufacturer.lowercase()}.com/downloads"
            viewModel.logMessage("[WARN] ${activeProfile.model} requires firmware ${activeProfile.requiredFirmware}. Current: $version")
            viewModel.speak("Hardware firmware update required for ${activeProfile.model}.")
        } else {
            driverStatus = "SYSTEM_DRIVERS_OPTIMAL"
            updateRequired = false
        }
    }

    /**
     * Discovers Bluetooth LE MIDI devices.
     */
    fun scanForBluetoothMidi() {
        viewModel.logMessage("[SYSTEM] Scanning for Bluetooth LE MIDI controllers...")
        // Bluetooth MIDI discovery requires a specific intent or the BluetoothLeScanner.
        // This is a bridge to the system picker.
        viewModel.speak("Scanning for wireless controllers.")
    }

    fun handleNoteTrigger(note: Int) {
        handleMappedNote(note)
    }

    fun syncPhysicalDevices() {
        viewModel.logMessage("[SYSTEM] Enumerating physical MIDI interfaces...")
        midiManager?.registerDeviceCallback(deviceCallback, Handler(Looper.getMainLooper()))
        val infos = midiManager?.devices ?: emptyArray()
        for (info in infos) {
            openMidiDevice(info)
        }
    }

    fun close() {
        midiManager?.unregisterDeviceCallback(deviceCallback)
        openPorts.forEach { port ->
            try { port.close() } catch (e: Exception) {}
        }
        openPorts.clear()
    }
}
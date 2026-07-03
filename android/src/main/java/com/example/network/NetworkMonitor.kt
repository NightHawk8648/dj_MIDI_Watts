package com.example.network

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.example.R // adjust according to actual R class location

class NetworkMonitor(private val context: Context) {

    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    
    companion object {
        private const val CHANNEL_ID = "dj_grid_network_alerts"
        private const val NOTIFICATION_ID = 1001
        private const val TAG = "NetworkMonitor"
    }

    private val _isGridAvailable = MutableStateFlow(true)
    val isGridAvailable: StateFlow<Boolean> = _isGridAvailable.asStateFlow()

    init {
        createNotificationChannel()
    }

    fun startMonitoring() {
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(networkRequest, object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                super.onCapabilitiesChanged(network, networkCapabilities)
                
                val isCellular = networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                val isVpn = networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
                val isWifi = networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)

                if (isCellular && !isWifi) {
                    _isGridAvailable.value = false
                    Log.w(TAG, "Switched to Mobile Data. Entering Offline Mode.")
                    showWarningNotification(
                        "DJ Grid: Offline Mode Active", 
                        "You are on Mobile Data. Local features remain available, but PC connection is paused."
                    )
                } else if (isVpn) {
                    _isGridAvailable.value = false
                    Log.w(TAG, "VPN Detected. Entering Offline Mode due to subnet mismatch.")
                    showWarningNotification(
                        "DJ Grid: Offline Mode Active", 
                        "Your VPN is active. Local app features remain available, but PC connection is paused."
                    )
                } else if (isWifi) {
                    _isGridAvailable.value = true
                }
            }
        })
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "DJ Grid Network Alerts"
            val descriptionText = "Alerts you when network changes block local DJ Grid access"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun showWarningNotification(title: String, content: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "POST_NOTIFICATIONS permission not granted. Cannot show warning.")
                return
            }
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert) // Built-in icon fallback
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(context)) {
            notify(NOTIFICATION_ID, builder.build())
        }
    }
}

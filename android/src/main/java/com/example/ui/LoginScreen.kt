package com.example.ui.auth

import android.app.Activity
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.djmidiwatts.BuildConfig
import com.example.ui.CommanderViewModel
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.ApiException

@Composable
fun LoginScreen(
    viewModel: CommanderViewModel,
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    val oneTapClient = remember { Identity.getSignInClient(context) }
    
    val WEB_CLIENT_ID = BuildConfig.UG_S2

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            try {
                val credential = oneTapClient.getSignInCredentialFromIntent(result.data)
                val idToken = credential.googleIdToken
                if (idToken != null) {
                    viewModel.signInWithGoogle(idToken, credential.displayName, CommanderViewModel.AccountType.FREE)
                    onLoginSuccess()
                }
            } catch (e: ApiException) {
                Log.e("AUTH", "One Tap error: ${e.localizedMessage}")
                viewModel.logMessage("[ERROR] Auth handshake failed: ${e.localizedMessage}")
            }
        }
    }

    if (viewModel.isSevereActionPending) {
        AlertDialog(
            onDismissRequest = { viewModel.isSevereActionPending = false },
            containerColor = Color(0xFF1E293B),
            title = {
                Text(
                    text = "CRITICAL GRID OVERRIDE",
                    color = Color.Red,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "A severe update has been requested: ${viewModel.pendingSevereMessage}. Proceed with the reconfiguration, Program?",
                    color = Color.White,
                    fontFamily = FontFamily.Monospace
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmSevereAction() }) {
                    Text("CONFIRM", color = Color(0xFF00FFCC), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.isSevereActionPending = false }) {
                    Text("ABORT", color = Color.Gray)
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0F172A), Color(0xFF020617))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Branding Header
            Text(
                text = "ULTIMA-GRID",
                fontSize = 32.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF00FFCC),
                letterSpacing = 4.sp
            )
            
            Text(
                text = "V1.0.0 COMMANDER SUITE",
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                color = Color.LightGray.copy(alpha = 0.6f),
                modifier = Modifier.padding(bottom = 48.dp)
            )

            // Login Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.5f)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Authorized Access Only",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        text = "Initialize system link to establish operational oversight. Authorized operators must authenticate via secure handshake.",
                        color = Color.Gray,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = {
                            val signInRequest = BeginSignInRequest.builder()
                                .setGoogleIdTokenRequestOptions(
                                    BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                                        .setSupported(true)
                                        .setServerClientId(WEB_CLIENT_ID)
                                        .setFilterByAuthorizedAccounts(false)
                                        .build()
                                )
                                .setAutoSelectEnabled(true)
                                .build()

                            oneTapClient.beginSignIn(signInRequest)
                                .addOnSuccessListener { result ->
                                    val intentSenderRequest = IntentSenderRequest.Builder(result.pendingIntent.intentSender).build()
                                    launcher.launch(intentSenderRequest)
                                }
                                .addOnFailureListener { e ->
                                    Log.e("AUTH", "One Tap prompt failed: ${e.localizedMessage}")
                                    viewModel.logMessage("[WARN] One Tap unavailable, using fallback.")
                                }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFCC)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(56.dp)
                    ) {
                        Text(
                            text = "SIGN IN WITH GOOGLE",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}
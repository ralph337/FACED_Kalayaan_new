package com.mswdo.kalayaan.faced.ui.login

import android.app.Activity
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.mswdo.kalayaan.faced.data.entity.UserEntity
import com.mswdo.kalayaan.faced.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(viewModel: LoginViewModel, onLoggedIn: (UserEntity) -> Unit) {
    val state by viewModel.state.collectAsState()
    val registerState by viewModel.registerState.collectAsState()
    val twoFactorState by viewModel.twoFactorState.collectAsState()

    var showRegister by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(state.loggedInUser) {
        state.loggedInUser?.let { onLoggedIn(it) }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Slate900),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp)
                .background(Color.White, RoundedCornerShape(20.dp))
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier.size(56.dp).clip(RoundedCornerShape(14.dp)).background(Blue600),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Filled.Shield, contentDescription = null, tint = Color.White) }

            Spacer(Modifier.height(14.dp))
            Text("MSWDO Kalayaan", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Disaster & FACED System", style = MaterialTheme.typography.bodyMedium, color = Slate500)
            Spacer(Modifier.height(34.dp))

            if (state.requires2FA && !twoFactorState.isVerified) {
                // --- EMAIL VERIFICATION SCREEN ---
                Text("Email Verification Required", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                Text(
                    "We have sent an official verification link to ${state.email.ifBlank { "your email" }}.\n\nPlease check your inbox/spam folder and click the link to verify your account before logging in.",
                    style = MaterialTheme.typography.bodySmall, color = Slate500, textAlign = TextAlign.Center
                )

                if (twoFactorState.error != null) {
                    Spacer(Modifier.height(10.dp))
                    Text(twoFactorState.error!!, color = Red600, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                }

                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = viewModel::checkEmailVerified,
                    modifier = Modifier.fillMaxWidth().height(46.dp)
                ) {
                    Text("Check Verification Status & Log In")
                }
                
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = viewModel::sendVerificationEmail,
                    modifier = Modifier.fillMaxWidth().height(46.dp)
                ) {
                    Text("Resend Verification Email")
                }
            } else if (showRegister) {
                // --- REGISTER SCREEN ---
                Text("Create Account", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = registerState.fullName,
                    onValueChange = viewModel::onRegisterNameChange,
                    label = { Text("Full Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = registerState.email,
                    onValueChange = viewModel::onRegisterEmailChange,
                    label = { Text("Email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = registerState.password,
                    onValueChange = viewModel::onRegisterPasswordChange,
                    label = { Text("Password (min 6 chars)") },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, contentDescription = null)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                if (registerState.error != null) {
                    Spacer(Modifier.height(10.dp))
                    Text(registerState.error!!, color = Red600, style = MaterialTheme.typography.bodySmall)
                }

                if (registerState.registered) {
                    Spacer(Modifier.height(10.dp))
                    Text("Account created successfully! You can now log in.", color = Emerald600, style = MaterialTheme.typography.bodySmall)
                }

                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = viewModel::register,
                    enabled = !registerState.isLoading,
                    modifier = Modifier.fillMaxWidth().height(46.dp)
                ) {
                    if (registerState.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text("Register")
                    }
                }

                Spacer(Modifier.height(12.dp))
                TextButton(onClick = { showRegister = false; viewModel.resetRegistration() }) {
                    Text("Back to Login")
                }

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 14.dp)) {
                    Divider(modifier = Modifier.weight(1f), color = Slate200)
                    Text("  or  ", style = MaterialTheme.typography.labelSmall, color = Slate400)
                    Divider(modifier = Modifier.weight(1f), color = Slate200)
                }

                OutlinedButton(
                    onClick = {
                        coroutineScope.launch {
                            try {
                                val credentialManager = CredentialManager.create(context)
                                val getGoogleIdOption = GetGoogleIdOption.Builder()
                                    .setFilterByAuthorizedAccounts(false)
                                    .setServerClientId("YOUR_WEB_CLIENT_ID.apps.googleusercontent.com") // Replace with actual Web Client ID
                                    .setAutoSelectEnabled(true)
                                    .build()

                                val request = GetCredentialRequest.Builder()
                                    .addCredentialOption(getGoogleIdOption)
                                    .build()

                                val result = credentialManager.getCredential(context as Activity, request)
                                val credential = result.credential

                                if (credential is GoogleIdTokenCredential) {
                                    viewModel.signInWithGoogle(credential)
                                }
                            } catch (e: NoCredentialException) {
                                Toast.makeText(context, "No Google accounts found.", Toast.LENGTH_SHORT).show()
                            } catch (e: GetCredentialException) {
                                Log.e("LoginScreen", "Google Sign-In failed", e)
                                Toast.makeText(context, "Google Sign-In failed.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(46.dp)
                ) {
                    Text("Register with Google")
                }
            } else {
                // --- LOGIN SCREEN ---
                OutlinedTextField(
                    value = state.email,
                    onValueChange = viewModel::onLoginEmailChange,
                    label = { Text("Email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = state.password,
                    onValueChange = viewModel::onLoginPasswordChange,
                    label = { Text("Password") },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, contentDescription = null)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                if (state.error != null) {
                    Spacer(Modifier.height(10.dp))
                    Text(state.error!!, color = Red600, style = MaterialTheme.typography.bodySmall)
                }

                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = viewModel::loginWithEmail,
                    enabled = !state.isLoading,
                    modifier = Modifier.fillMaxWidth().height(46.dp)
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text("Log In")
                    }
                }

                Spacer(Modifier.height(12.dp))
                TextButton(onClick = { showRegister = true }) {
                    Text("Don't have an account? Register")
                }

                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 14.dp)) {
                    Divider(modifier = Modifier.weight(1f), color = Slate200)
                    Text("  or  ", style = MaterialTheme.typography.labelSmall, color = Slate400)
                    Divider(modifier = Modifier.weight(1f), color = Slate200)
                }

                OutlinedButton(
                    onClick = viewModel::continueOffline,
                    modifier = Modifier.fillMaxWidth().height(46.dp)
                ) {
                    Icon(Icons.Filled.WifiOff, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Continue Offline (Guest Mode)")
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "No account needed. Everything you file is saved on this device and can be synced to your account the next time you log in online.",
                    style = MaterialTheme.typography.labelSmall,
                    color = Slate400,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

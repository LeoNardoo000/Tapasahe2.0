package com.example.rfidpaymentsys.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rfidpaymentsys.viewmodel.MainViewModel
import com.example.rfidpaymentsys.viewmodel.UserRole

@Composable
fun ChangePasswordDialog(viewModel: MainViewModel) {
    if (viewModel.showChangePasswordDialog) {
        var oldPass by remember { mutableStateOf("") }
        var newPass by remember { mutableStateOf("") }
        var confirmPass by remember { mutableStateOf("") }
        var localError by remember { mutableStateOf<String?>(null) }
        val role = viewModel.currentUserRole

        AlertDialog(
            onDismissRequest = { viewModel.showChangePasswordDialog = false },
            title = {
                Text(text = "Change Password")
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    if (role != UserRole.ADMIN) {
                        PasswordField(
                            value = oldPass,
                            onValueChange = { oldPass = it },
                            label = "Current Password",
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    PasswordField(
                        value = newPass,
                        onValueChange = { newPass = it },
                        label = "New Password",
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    PasswordField(
                        value = confirmPass,
                        onValueChange = { confirmPass = it },
                        label = "Confirm New Password",
                        modifier = Modifier.fillMaxWidth()
                    )
                    localError?.let {
                        Text(text = it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (newPass != confirmPass) {
                        localError = "New passwords don't match"
                    } else if (newPass.length < 6) {
                        localError = "Password must be at least 6 characters"
                    } else {
                        viewModel.changePassword(oldPass, newPass)
                    }
                }) {
                    Text(text = "UPDATE")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    viewModel.showChangePasswordDialog = false 
                }) {
                    Text(text = "CANCEL")
                }
            }
        )
    }
}

@Composable
fun LogoutConfirmationDialog(viewModel: MainViewModel) {
    if (viewModel.showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.showLogoutDialog = false },
            title = {
                Text(text = "Confirm Logout")
            },
            text = {
                Text(text = "Are you sure you want to log out?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.showLogoutDialog = false
                        viewModel.logout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text(text = "LOGOUT", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.showLogoutDialog = false }) {
                    Text(text = "CANCEL")
                }
            }
        )
    }
}

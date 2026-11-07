package com.example.rfidpaymentsys.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rfidpaymentsys.viewmodel.MainViewModel
import kotlinx.coroutines.delay

/** "Welcome back, Name!" slides in from the top after login and disappears by itself. */
@Composable
fun WelcomeBanner(viewModel: MainViewModel, modifier: Modifier = Modifier) {
    val message = viewModel.welcomeMessage
    var lastMessage by remember { mutableStateOf("") }

    LaunchedEffect(message) {
        if (message != null) {
            lastMessage = message
            delay(3500)
            viewModel.clearWelcome()
        }
    }

    AnimatedVisibility(
        visible = message != null,
        enter = slideInVertically { -it } + fadeIn(),
        exit = slideOutVertically { -it } + fadeOut(),
        modifier = modifier
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.primary,
            shadowElevation = 12.dp,
            modifier = Modifier.padding(16.dp).fillMaxWidth()
        ) {
            Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(44.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(lastMessage, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                    Text("You are now logged in.", color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp)
                }
            }
        }
    }
}

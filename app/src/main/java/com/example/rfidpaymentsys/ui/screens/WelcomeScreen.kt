package com.example.rfidpaymentsys.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rfidpaymentsys.R
import com.example.rfidpaymentsys.navigation.Screen
import com.example.rfidpaymentsys.ui.theme.BrandAmber
import com.example.rfidpaymentsys.ui.theme.BrandAmberDark
import com.example.rfidpaymentsys.ui.theme.BrandBlueDark
import com.example.rfidpaymentsys.viewmodel.MainViewModel

/** The first screen. It sits on the brand gradient, which is drawn behind it by MainActivity. */
@Composable
fun WelcomeScreen(viewModel: MainViewModel) {
    val shape = RoundedCornerShape(16.dp)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.logo_mark),
            contentDescription = "Tapasahe logo",
            modifier = Modifier.size(132.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Tapasahe",
            fontSize = 40.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White
        )
        Text(
            text = "Tap. Pay. Go.",
            fontSize = 16.sp,
            color = Color.White.copy(alpha = 0.85f)
        )

        Spacer(modifier = Modifier.height(44.dp))

        Button(
            onClick = { viewModel.navigateTo(Screen.ClientAuth.route) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = shape,
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = BrandBlueDark)
        ) {
            Text("CLIENT ACCESS", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = { viewModel.navigateTo(Screen.OperatorAuth.route) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = shape,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White.copy(alpha = 0.16f),
                contentColor = Color.White
            ),
            border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.7f))
        ) {
            Text("OPERATOR ACCESS", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
        }

        Spacer(modifier = Modifier.height(4.dp))

        TextButton(
            onClick = { viewModel.navigateTo(Screen.AdminAuth.route) },
            colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
        ) {
            Text("ADMIN LOGIN", fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = { viewModel.navigateTo(Screen.CardScanner.route) },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = shape,
            colors = ButtonDefaults.buttonColors(containerColor = BrandAmber, contentColor = BrandAmberDark)
        ) {
            Text("SCAN CARD ID", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Find your card's ID or check if it is already registered",
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.8f)
        )
    }
}

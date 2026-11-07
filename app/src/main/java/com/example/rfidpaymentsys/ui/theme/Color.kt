package com.example.rfidpaymentsys.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Tapasahe brand palette (taken from the app icon)
val BrandBlue = Color(0xFF1B6FE8)        // main colour
val BrandBlueLight = Color(0xFF2F86FF)   // top of the icon gradient
val BrandBlueDark = Color(0xFF0A3F94)    // bottom of the icon gradient
val BrandAmber = Color(0xFFF59E0B)       // the "tap" waves
val BrandAmberDark = Color(0xFF2B1B00)   // text on amber

// Meaning colours
val SuccessGreen = Color(0xFF2E7D32)
val WarningOrange = Color(0xFFE65100)
val DangerRed = Color(0xFFC62828)

/** The icon's blue gradient, used for headers and cards. */
val BrandGradient: Brush = Brush.linearGradient(listOf(BrandBlueLight, BrandBlueDark))

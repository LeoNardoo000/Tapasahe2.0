package com.example.rfidpaymentsys.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun WeeklyProfitChart(data: List<Pair<String, Double>>) {
    val maxProfit = data.maxOfOrNull { it.second } ?: 1.0
    val chartHeight = 180.dp 

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .height(chartHeight),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            data.forEach { (day, profit) ->
                val barHeightFraction = if (maxProfit > 0) (profit / maxProfit).toFloat().coerceIn(0.01f, 1f) else 0.01f
                
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                    modifier = Modifier.weight(1f).fillMaxHeight()
                ) {
                    // Display amount above bar if it's not 0
                    if (profit > 0) {
                        Text(
                            text = "₱${profit.toInt()}", 
                            fontSize = 9.sp, 
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxHeight(barHeightFraction * 0.6f) // Use only 60% for the bar to avoid overlap
                            .width(22.dp)
                            .background(
                                color = if (profit > 0) MaterialTheme.colorScheme.primary else Color.LightGray.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                            )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = day, 
                        fontSize = 11.sp, 
                        fontWeight = FontWeight.Medium,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

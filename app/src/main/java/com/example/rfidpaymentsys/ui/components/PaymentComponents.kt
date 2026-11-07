package com.example.rfidpaymentsys.ui.components
import com.example.rfidpaymentsys.ui.theme.BrandGradient

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rfidpaymentsys.data.entity.Transaction
import com.example.rfidpaymentsys.data.entity.displayTitle
import com.example.rfidpaymentsys.data.entity.isDebit
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 1234.5 -> "₱1,234.50" */
fun formatPeso(amount: Double): String = "₱" + String.format(Locale.US, "%,.2f", amount)

/** 09171234567 -> "0917***4567" */
fun maskMobile(number: String): String =
    if (number.length == 11) number.take(4) + "***" + number.takeLast(4) else number

fun formatDateTime(timestamp: Long): String =
    SimpleDateFormat("MMM dd, yyyy  h:mm a", Locale.US).format(Date(timestamp))

@Composable
fun GradientBalanceCard(
    title: String,
    amountText: String,
    modifier: Modifier = Modifier,
    footer: String? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(BrandGradient)
            .padding(24.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                trailing?.invoke()
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(amountText, color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold)
            if (footer != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(footer, color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun ActionTile(icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(16.dp)
    Surface(
        modifier = modifier.clip(shape).clickable(onClick = onClick),
        shape = shape,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
        }
    }
}

/** A tappable card that shows a highlighted border when selected. */
@Composable
fun OptionCard(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    Card(
        modifier = modifier.clip(shape).clickable(enabled = enabled, onClick = onClick),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            if (selected) 2.dp else 1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
        Spacer(modifier = Modifier.width(16.dp))
        Text(value, fontWeight = FontWeight.Medium, fontSize = 14.sp, textAlign = TextAlign.End)
    }
}

@Composable
fun TransactionRow(tx: Transaction) {
    val debit = tx.isDebit()
    val tint = if (debit) Color(0xFFC62828) else Color(0xFF2E7D32)
    val container = if (debit) Color(0xFFFDECEA) else Color(0xFFE6F4EA)
    val icon = when {
        tx.method == "RFID" -> Icons.Default.DirectionsBus
        tx.method.startsWith("CASH_OUT") || tx.method.startsWith("WITHDRAW_") -> Icons.Default.ArrowUpward
        else -> Icons.Default.ArrowDownward
    }
    val date = remember(tx.timestamp) {
        SimpleDateFormat("MMM dd, h:mm a", Locale.US).format(Date(tx.timestamp))
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(CircleShape).background(container),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(tx.displayTitle(), fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text(date, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(
            (if (debit) "- " else "+ ") + formatPeso(tx.amount),
            color = tint,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
        )
    }
}

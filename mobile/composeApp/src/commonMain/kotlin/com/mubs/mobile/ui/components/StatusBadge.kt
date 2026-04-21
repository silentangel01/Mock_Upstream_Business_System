package com.mubs.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mubs.mobile.data.model.TicketStatus

@Composable
fun StatusBadge(status: TicketStatus, modifier: Modifier = Modifier) {
    val (bg, text, label) = statusStyle(status)
    Text(
        text = label,
        color = text,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        modifier = modifier
            .background(bg, RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    )
}

private data class StatusStyle(val bg: Color, val text: Color, val label: String)

private fun statusStyle(status: TicketStatus): StatusStyle = when (status) {
    TicketStatus.PENDING -> StatusStyle(Color(0xFFFFF3E0), Color(0xFFE65100), "Pending")
    TicketStatus.DISPATCHED -> StatusStyle(Color(0xFFE3F2FD), Color(0xFF1565C0), "Dispatched")
    TicketStatus.ACCEPTED -> StatusStyle(Color(0xFFE8F5E9), Color(0xFF2E7D32), "Accepted")
    TicketStatus.IN_PROGRESS -> StatusStyle(Color(0xFFFFF8E1), Color(0xFFF9A825), "In Progress")
    TicketStatus.RESOLVED -> StatusStyle(Color(0xFFE8F5E9), Color(0xFF1B5E20), "Resolved")
    TicketStatus.CLOSED -> StatusStyle(Color(0xFFECEFF1), Color(0xFF546E7A), "Closed")
    TicketStatus.RETURNED -> StatusStyle(Color(0xFFFFEBEE), Color(0xFFC62828), "Returned")
}

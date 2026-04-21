package com.mubs.mobile.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.mubs.mobile.data.model.Fieldworker
import com.mubs.mobile.data.model.Ticket
import com.mubs.mobile.data.model.TicketStatus
import com.mubs.mobile.data.model.TimelineEntry
import com.mubs.mobile.di.AppModule
import com.mubs.mobile.ui.components.PhotoPickerButton
import com.mubs.mobile.ui.components.StatusBadge

data class TicketDetailScreen(val ticketId: String) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val model = remember {
            TicketDetailScreenModel(
                ticketId,
                AppModule.instance.ticketRepository,
                AppModule.instance.authRepository
            )
        }
        val state by model.state.collectAsState()

        if (state.showNoteDialog) {
            NoteDialog(
                onConfirm = { model.confirmStatusUpdate(it) },
                onDismiss = model::dismissDialog
            )
        }

        if (state.showReassignSheet) {
            ReassignSheet(
                fieldworkers = state.fieldworkers,
                currentUser = state.ticket?.assignedUser,
                onConfirm = { user, note -> model.confirmReassign(user, note) },
                onDismiss = model::dismissDialog
            )
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("工单详情") },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Text("<", color = MaterialTheme.colorScheme.onPrimary)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        ) { padding ->
            when {
                state.isLoading -> Box(
                    Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }

                state.error != null && state.ticket == null -> Box(
                    Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) { Text(state.error!!, color = MaterialTheme.colorScheme.error) }

                state.ticket != null -> TicketContent(
                    ticket = state.ticket!!,
                    role = state.role,
                    actionInProgress = state.actionInProgress,
                    onStatusUpdate = model::requestStatusUpdate,
                    onReassign = model::showReassign,
                    onPhotoUpload = model::uploadPhoto,
                    modifier = Modifier.padding(padding)
                )
            }
        }
    }
}

@Composable
private fun TicketContent(
    ticket: Ticket,
    role: String?,
    actionInProgress: Boolean,
    onStatusUpdate: (String) -> Unit,
    onReassign: () -> Unit,
    onPhotoUpload: (String, ByteArray) -> Unit,
    modifier: Modifier = Modifier
) {
    val eventTypeLabels = mapOf(
        "smoke_flame" to "烟火检测",
        "parking_violation" to "违停检测",
        "common_space_utilization" to "公共空间占用"
    )

    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                eventTypeLabels[ticket.eventType] ?: ticket.eventType,
                style = MaterialTheme.typography.titleLarge
            )
            StatusBadge(ticket.status)
        }

        // Info card
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoRow("摄像头", ticket.cameraId)
                ticket.location?.let { InfoRow("位置", it) }
                ticket.assignedTeam?.let { InfoRow("负责团队", it) }
                ticket.assignedUser?.let { InfoRow("负责人", it) }
                InfoRow("置信度", "${(ticket.confidence * 100).toInt()}%")
                ticket.description?.let { InfoRow("描述", it) }
            }
        }

        // Action buttons
        if (!actionInProgress) {
            ActionButtons(ticket, role, onStatusUpdate, onReassign)
        } else {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        // Photo upload
        if (ticket.status == TicketStatus.IN_PROGRESS || ticket.status == TicketStatus.ACCEPTED) {
            PhotoPickerButton(onPhotoPicked = onPhotoUpload)
        }

        // Handle photos
        if (ticket.handlePhotos.isNotEmpty()) {
            Text("处理照片 (${ticket.handlePhotos.size})", style = MaterialTheme.typography.titleMedium)
            ticket.handlePhotos.forEach { url ->
                Text(url, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
        }

        // Timeline
        if (ticket.timeline.isNotEmpty()) {
            Text("处理时间线", style = MaterialTheme.typography.titleMedium)
            ticket.timeline.forEachIndexed { index, entry ->
                TimelineItem(entry)
                if (index < ticket.timeline.lastIndex) {
                    HorizontalDivider(Modifier.padding(vertical = 4.dp))
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(80.dp)
        )
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ActionButtons(
    ticket: Ticket,
    role: String?,
    onStatusUpdate: (String) -> Unit,
    onReassign: () -> Unit
) {
    val isFieldworker = role == "FIELDWORKER"
    val isDispatcher = role == "DISPATCHER" || role == "ADMIN"

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        when (ticket.status) {
            TicketStatus.DISPATCHED -> if (isFieldworker) {
                Button(onClick = { onStatusUpdate("ACCEPTED") }, Modifier.fillMaxWidth()) {
                    Text("接单")
                }
            }
            TicketStatus.ACCEPTED -> if (isFieldworker) {
                Button(onClick = { onStatusUpdate("IN_PROGRESS") }, Modifier.fillMaxWidth()) {
                    Text("开始处理")
                }
            }
            TicketStatus.IN_PROGRESS -> if (isFieldworker) {
                Button(onClick = { onStatusUpdate("RESOLVED") }, Modifier.fillMaxWidth()) {
                    Text("完成处理")
                }
            }
            else -> {}
        }

        if (isDispatcher && ticket.status != TicketStatus.CLOSED && ticket.status != TicketStatus.RESOLVED) {
            OutlinedButton(
                onClick = onReassign,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Text("改派工单")
            }
        }
    }
}

@Composable
private fun TimelineItem(entry: TimelineEntry) {
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(entry.action, style = MaterialTheme.typography.bodyMedium)
            Text(
                entry.timestamp.take(16).replace("T", " "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            "操作人: ${entry.actor}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        entry.note?.let {
            Text(it, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun NoteDialog(onConfirm: (String?) -> Unit, onDismiss: () -> Unit) {
    var note by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加备注") },
        text = {
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("备注（可选）") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(note.ifBlank { null }) }) {
                Text("确认")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReassignSheet(
    fieldworkers: List<Fieldworker>,
    currentUser: String?,
    onConfirm: (String, String?) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedUser by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    val available = fieldworkers.filter { it.username != currentUser }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("改派工单", style = MaterialTheme.typography.titleMedium)

            if (available.isEmpty()) {
                Text("加载人员列表中...", style = MaterialTheme.typography.bodySmall)
            } else {
                available.forEach { fw ->
                    val label = "${fw.displayName ?: fw.username} (${fw.team ?: "-"})"
                    OutlinedButton(
                        onClick = { selectedUser = fw.username },
                        modifier = Modifier.fillMaxWidth(),
                        colors = if (selectedUser == fw.username)
                            ButtonDefaults.outlinedButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        else ButtonDefaults.outlinedButtonColors()
                    ) {
                        Text(label)
                    }
                }
            }

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("备注（可选）") },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = { if (selectedUser.isNotEmpty()) onConfirm(selectedUser, note.ifBlank { null }) },
                enabled = selectedUser.isNotEmpty(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("确认改派")
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

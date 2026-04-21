package com.mubs.mobile.ui.tickets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.mubs.mobile.data.model.TicketStatus
import com.mubs.mobile.di.AppModule
import com.mubs.mobile.ui.components.TicketCard
import com.mubs.mobile.ui.detail.TicketDetailScreen
import com.mubs.mobile.ui.login.LoginScreen
import kotlinx.coroutines.launch

class TicketListScreen : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()
        val model = remember { TicketListScreenModel(AppModule.instance.ticketRepository) }
        val state by model.state.collectAsState()
        val listState = rememberLazyListState()
        val username = remember { mutableStateOf<String?>(null) }

        LaunchedEffect(Unit) {
            username.value = AppModule.instance.authRepository.getUsername()
        }

        val shouldLoadMore by remember {
            derivedStateOf {
                val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                lastVisible >= state.tickets.size - 3
            }
        }

        LaunchedEffect(shouldLoadMore) {
            if (shouldLoadMore && state.hasMore && !state.isLoading) {
                model.loadMore()
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Tickets")
                            username.value?.let {
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    actions = {
                        TextButton(onClick = {
                            scope.launch {
                                AppModule.instance.authRepository.logout()
                                navigator.replaceAll(LoginScreen())
                            }
                        }) {
                            Text("Logout", color = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                )
            }
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding)) {
                StatusFilterRow(
                    selected = state.selectedStatus,
                    onSelect = model::filterByStatus
                )

                PullToRefreshBox(
                    isRefreshing = state.isRefreshing,
                    onRefresh = model::refresh,
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (state.tickets.isEmpty() && !state.isLoading) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = state.error ?: "No tickets",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(state.tickets, key = { it.id ?: it.hvasEventId }) { ticket ->
                                TicketCard(ticket) {
                                    ticket.id?.let { navigator.push(TicketDetailScreen(it)) }
                                }
                            }
                            if (state.isLoading && state.tickets.isNotEmpty()) {
                                item {
                                    Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private val statusFilters = listOf(
    null to "All",
    TicketStatus.PENDING to "Pending",
    TicketStatus.DISPATCHED to "Dispatched",
    TicketStatus.IN_PROGRESS to "In Progress",
    TicketStatus.RESOLVED to "Resolved"
)

@Composable
private fun StatusFilterRow(selected: TicketStatus?, onSelect: (TicketStatus?) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(statusFilters) { (status, label) ->
            FilterChip(
                selected = selected == status,
                onClick = { onSelect(status) },
                label = { Text(label) }
            )
        }
    }
}

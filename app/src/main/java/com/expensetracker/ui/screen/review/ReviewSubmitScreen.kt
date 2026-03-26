package com.expensetracker.ui.screen.review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewSubmitScreen(
    batchId: Long,
    onBack: () -> Unit,
    onSubmitted: () -> Unit,
    viewModel: ReviewSubmitViewModel = hiltViewModel()
) {
    LaunchedEffect(batchId) { viewModel.setBatchId(batchId) }

    val batch by viewModel.batch.collectAsState()
    val expenseItems by viewModel.expenseItems.collectAsState()
    val mileageEntries by viewModel.mileageEntries.collectAsState()
    val grandTotal by viewModel.grandTotal.collectAsState()
    val totalExpenses by viewModel.totalExpenses.collectAsState()
    val totalMileage by viewModel.totalMileage.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val pendingPdf by viewModel.pendingPdfFile.collectAsState()
    val context = LocalContext.current
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Review & Submit") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Batch header
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            batch?.title ?: "",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        batch?.let {
                            Text(
                                dateFormat.format(Date(it.createdAt)),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (settings.senderName.isNotBlank()) {
                            Text(
                                "From: ${settings.senderName}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            // Expenses summary
            if (expenseItems.isNotEmpty()) {
                item {
                    Text("Expenses", style = MaterialTheme.typography.titleMedium)
                }
                items(expenseItems) { item ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    item.category.replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    dateFormat.format(Date(item.date)),
                                    style = MaterialTheme.typography.bodySmall
                                )
                                if (item.description.isNotBlank()) {
                                    Text(item.description, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            Text(
                                currencyFormat.format(item.amount),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Expenses Subtotal", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            currencyFormat.format(totalExpenses),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }

            // Mileage summary
            if (mileageEntries.isNotEmpty()) {
                item {
                    Text("Mileage", style = MaterialTheme.typography.titleMedium)
                }
                items(mileageEntries) { entry ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "${entry.distance} miles",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    "${dateFormat.format(Date(entry.date))} @ ${currencyFormat.format(entry.rate)}/mi",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Text(
                                currencyFormat.format(entry.calculatedAmount),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Mileage Subtotal", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            currencyFormat.format(totalMileage),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }

            // Grand total
            item {
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("GRAND TOTAL", style = MaterialTheme.typography.titleLarge)
                        Text(
                            currencyFormat.format(grandTotal),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Signature preview
            item {
                if (settings.signatureImageUri != null) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Signature", style = MaterialTheme.typography.titleSmall)
                            Spacer(modifier = Modifier.height(8.dp))
                            AsyncImage(
                                model = settings.signatureImageUri,
                                contentDescription = "Signature",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                }
            }

            // Recipients
            item {
                if (settings.emailRecipients.isNotBlank()) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Send to:", style = MaterialTheme.typography.titleSmall)
                            Text(
                                settings.emailRecipients,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            // Submit button
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { viewModel.submitBatch(context, onSubmitted) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = !isGenerating
                ) {
                    if (isGenerating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Text("  Generating PDF...")
                    } else {
                        Icon(Icons.Default.Send, contentDescription = null)
                        Text("  Submit Expense Report")
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Save PDF to Downloads dialog
    if (pendingPdf != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissSaveDialog() },
            title = { Text("Save PDF") },
            text = { Text("Would you like to save a copy of the expense report to your Downloads folder?") },
            confirmButton = {
                Button(onClick = { viewModel.savePdfToDownloads(context) }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissSaveDialog() }) {
                    Text("No thanks")
                }
            }
        )
    }
}

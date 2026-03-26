package com.expensetracker.ui.component

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.regex.Pattern
import kotlin.coroutines.resume

data class ScannedReceiptData(
    val totalAmount: Double? = null,
    val date: String? = null,
    val vendor: String? = null,
    val lineItems: List<ScannedLineItem> = emptyList(),
    val rawText: String = ""
)

data class ScannedLineItem(
    val description: String,
    val amount: Double
)

suspend fun scanReceipt(context: Context, imageUri: Uri): ScannedReceiptData {
    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    val image = InputImage.fromFilePath(context, imageUri)

    val text = suspendCancellableCoroutine { continuation ->
        recognizer.process(image)
            .addOnSuccessListener { result -> continuation.resume(result.text) }
            .addOnFailureListener { continuation.resume("") }
    }

    return parseReceiptText(text)
}

fun parseReceiptText(text: String): ScannedReceiptData {
    val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }
    val amountPattern = Pattern.compile("\\$?\\d+\\.\\d{2}")
    val datePattern = Pattern.compile("\\d{1,2}[/\\-]\\d{1,2}[/\\-]\\d{2,4}")

    // Extract vendor (usually first non-empty line)
    val vendor = lines.firstOrNull()

    // Extract date
    var date: String? = null
    for (line in lines) {
        val dateMatcher = datePattern.matcher(line)
        if (dateMatcher.find()) {
            date = dateMatcher.group()
            break
        }
    }

    // Extract line items with amounts
    val lineItems = mutableListOf<ScannedLineItem>()
    val amounts = mutableListOf<Double>()

    for (line in lines) {
        val matcher = amountPattern.matcher(line)
        if (matcher.find()) {
            val amountStr = matcher.group().removePrefix("$")
            val amount = amountStr.toDoubleOrNull()
            if (amount != null) {
                amounts.add(amount)
                val description = line.substring(0, matcher.start()).trim()
                    .ifBlank { line.substring(matcher.end()).trim() }
                if (description.isNotBlank()) {
                    lineItems.add(ScannedLineItem(description, amount))
                }
            }
        }
    }

    // Total is typically the largest amount, or look for "total" keyword
    var totalAmount: Double? = null
    for (line in lines) {
        if (line.lowercase().contains("total") && !line.lowercase().contains("subtotal")) {
            val matcher = amountPattern.matcher(line)
            if (matcher.find()) {
                totalAmount = matcher.group().removePrefix("$").toDoubleOrNull()
                break
            }
        }
    }
    if (totalAmount == null && amounts.isNotEmpty()) {
        totalAmount = amounts.max()
    }

    return ScannedReceiptData(
        totalAmount = totalAmount,
        date = date,
        vendor = vendor,
        lineItems = lineItems,
        rawText = text
    )
}

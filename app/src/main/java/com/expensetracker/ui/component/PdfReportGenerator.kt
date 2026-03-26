package com.expensetracker.ui.component

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import com.expensetracker.data.local.entity.BatchEntity
import com.expensetracker.data.local.entity.ExpenseItemEntity
import com.expensetracker.data.local.entity.MileageEntryEntity
import com.expensetracker.data.local.entity.SettingsEntity
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PdfReportGenerator(private val context: Context) {

    // US Letter dimensions in PostScript points (72 dpi)
    private val pageWidth = 612
    private val pageHeight = 792

    private val marginLeft = 48f
    private val marginRight = 48f
    private val marginTop = 48f
    private val marginBottom = 48f
    private val contentWidth = pageWidth - marginLeft - marginRight

    private val headerHeight = 80f
    private val footerHeight = 30f

    // Colors
    private val brandDark = Color.parseColor("#1A2744")
    private val brandMedium = Color.parseColor("#1565C0")
    private val brandLight = Color.parseColor("#E8F0FE")
    private val textDark = Color.parseColor("#1A1C1E")
    private val textMedium = Color.parseColor("#5F6368")
    private val tableBorder = Color.parseColor("#DADCE0")
    private val tableHeaderBg = Color.parseColor("#1A2744")
    private val tableHeaderText = Color.WHITE
    private val tableAltRow = Color.parseColor("#F8F9FA")
    private val white = Color.WHITE

    // Paints
    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = brandDark
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textSize = 22f
    }
    private val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = textMedium
        textSize = 10f
    }
    private val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = brandDark
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textSize = 11f
    }
    private val sectionTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = brandDark
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textSize = 13f
    }
    private val tableHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = tableHeaderText
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textSize = 9f
    }
    private val tableCellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = textDark
        textSize = 9f
    }
    private val tableCellBoldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = textDark
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textSize = 9f
    }
    private val totalLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = brandDark
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textSize = 14f
    }
    private val totalAmountPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = brandMedium
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textSize = 14f
    }
    private val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = textMedium
        textSize = 8f
    }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = tableBorder
        strokeWidth = 0.5f
        style = Paint.Style.STROKE
    }
    private val accentLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = brandMedium
        strokeWidth = 2f
        style = Paint.Style.STROKE
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val sigLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = textMedium
        textSize = 9f
    }
    private val noteTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = brandDark
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textSize = 10f
    }
    private val noteBodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = textDark
        textSize = 9f
    }

    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)

    private var logoBitmap: Bitmap? = null
    private var signatureBitmap: Bitmap? = null

    fun generate(
        batch: BatchEntity,
        expenses: List<ExpenseItemEntity>,
        mileage: List<MileageEntryEntity>,
        settings: SettingsEntity,
        grandTotal: Double
    ): File {
        // Load assets
        logoBitmap = try {
            context.assets.open("logo.png").use { BitmapFactory.decodeStream(it) }
        } catch (_: Exception) { null }

        signatureBitmap = settings.signatureImageUri?.let { uri ->
            try {
                val file = File(uri)
                if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
            } catch (_: Exception) { null }
        }

        val document = PdfDocument()
        val pages = mutableListOf<PageContent>()

        // Build page 1 content
        val page1 = PageContent(isFirstPage = true)

        // Expenses table
        if (expenses.isNotEmpty()) {
            page1.sections.add(Section.SectionTitle("EXPENSES"))
            page1.sections.add(Section.ExpenseTable(expenses))
            page1.sections.add(Section.Subtotal("Expenses Subtotal", expenses.sumOf { it.amount }))
            page1.sections.add(Section.Spacer(12f))
        }

        // Mileage table
        if (mileage.isNotEmpty()) {
            page1.sections.add(Section.SectionTitle("MILEAGE"))
            page1.sections.add(Section.MileageTable(mileage))
            page1.sections.add(Section.Subtotal("Mileage Subtotal", mileage.sumOf { it.calculatedAmount }))
            page1.sections.add(Section.Spacer(12f))
        }

        // Grand total
        page1.sections.add(Section.GrandTotal(grandTotal))
        page1.sections.add(Section.Spacer(16f))

        // Signature
        page1.sections.add(Section.Signature)

        pages.add(page1)

        // Receipt pages — collect all expenses with receipt images
        val receiptsWithImages = expenses.filter { it.receiptImageUri != null }

        // Notes — collect all items with notes
        val notesItems = mutableListOf<Pair<String, String>>()
        if (batch.notes.isNotBlank()) {
            notesItems.add("Batch Notes" to batch.notes)
        }
        for (expense in expenses) {
            if (expense.notes.isNotBlank()) {
                val label = "${expense.category.replaceFirstChar { it.uppercase() }} - ${expense.description}".take(60)
                notesItems.add(label to expense.notes)
            }
        }
        for (entry in mileage) {
            if (entry.notes.isNotBlank()) {
                notesItems.add("Mileage ${entry.distance} mi (${dateFormat.format(Date(entry.date))})" to entry.notes)
            }
        }

        // Count total attachment pages (receipts + notes)
        val attachmentPageCount = receiptsWithImages.size + if (notesItems.isNotEmpty()) 1 else 0

        if (attachmentPageCount > 0) {
            page1.sections.add(Section.Spacer(12f))
            page1.sections.add(Section.ReceiptReference(attachmentPageCount))
        }

        for (expense in receiptsWithImages) {
            val receiptPage = PageContent(isFirstPage = false)
            receiptPage.sections.add(Section.ReceiptImage(expense))
            pages.add(receiptPage)
        }

        if (notesItems.isNotEmpty()) {
            val notesPage = PageContent(isFirstPage = false)
            notesPage.sections.add(Section.SectionTitle("NOTES & COMMENTS"))
            notesPage.sections.add(Section.Spacer(8f))
            notesPage.sections.add(Section.NotesList(notesItems))
            pages.add(notesPage)
        }

        val totalPages = pages.size

        // Render each page
        for ((index, pageContent) in pages.withIndex()) {
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, index + 1).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            drawHeader(canvas, batch, settings)
            drawFooter(canvas, index + 1, totalPages)
            drawContent(canvas, pageContent)

            document.finishPage(page)
        }

        // Save to file
        val reportsDir = File(context.filesDir, "reports").apply { mkdirs() }
        val fileName = "ExpenseReport_${batch.title.replace(Regex("[^a-zA-Z0-9]"), "_")}_${System.currentTimeMillis()}.pdf"
        val file = File(reportsDir, fileName)
        FileOutputStream(file).use { document.writeTo(it) }
        document.close()

        logoBitmap?.recycle()
        signatureBitmap?.recycle()

        return file
    }

    private fun drawHeader(canvas: Canvas, batch: BatchEntity, settings: SettingsEntity) {
        // Background stripe at top
        fillPaint.color = brandDark
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), 4f, fillPaint)

        val headerY = marginTop

        // Logo (left side)
        logoBitmap?.let { logo ->
            val logoHeight = 50f
            val logoWidth = logoHeight * (logo.width.toFloat() / logo.height.toFloat())
            val destRect = RectF(marginLeft, headerY, marginLeft + logoWidth, headerY + logoHeight)
            canvas.drawBitmap(logo, null, destRect, null)
        }

        // Title + info (right aligned)
        val rightX = pageWidth - marginRight
        canvas.drawText("Expense Report", rightX - titlePaint.measureText("Expense Report"), headerY + 20f, titlePaint)

        if (settings.senderName.isNotBlank()) {
            canvas.drawText(settings.senderName, rightX - namePaint.measureText(settings.senderName), headerY + 36f, namePaint)
        }

        val dateStr = dateFormat.format(Date(batch.createdAt))
        canvas.drawText(dateStr, rightX - subtitlePaint.measureText(dateStr), headerY + 50f, subtitlePaint)

        val batchTitle = batch.title
        canvas.drawText(batchTitle, rightX - subtitlePaint.measureText(batchTitle), headerY + 63f, subtitlePaint)

        // Accent line below header
        val lineY = headerY + headerHeight - 8f
        canvas.drawLine(marginLeft, lineY, rightX, lineY, accentLinePaint)
    }

    private fun drawFooter(canvas: Canvas, pageNum: Int, totalPages: Int) {
        val footerY = pageHeight - marginBottom

        // Thin line above footer
        canvas.drawLine(marginLeft, footerY - footerHeight + 5f, pageWidth - marginRight, footerY - footerHeight + 5f, linePaint)

        val pageText = "Page $pageNum of $totalPages"
        canvas.drawText(pageText, marginLeft, footerY, footerPaint)

        val appText = "Generated by Expense Tracker"
        canvas.drawText(appText, pageWidth - marginRight - footerPaint.measureText(appText), footerY, footerPaint)
    }

    private fun drawContent(canvas: Canvas, pageContent: PageContent) {
        var y = marginTop + headerHeight + 8f
        val maxY = pageHeight - marginBottom - footerHeight - 10f

        for (section in pageContent.sections) {
            if (y >= maxY) break

            when (section) {
                is Section.SectionTitle -> {
                    y += 4f
                    canvas.drawText(section.title, marginLeft, y + 13f, sectionTitlePaint)
                    y += 22f
                }
                is Section.ExpenseTable -> {
                    y = drawExpenseTable(canvas, section.items, y, maxY)
                }
                is Section.MileageTable -> {
                    y = drawMileageTable(canvas, section.entries, y, maxY)
                }
                is Section.Subtotal -> {
                    y += 4f
                    val amountStr = currencyFormat.format(section.amount)
                    fillPaint.color = brandLight
                    canvas.drawRect(marginLeft, y, marginLeft + contentWidth, y + 18f, fillPaint)
                    canvas.drawText(section.label, marginLeft + 8f, y + 13f, tableCellBoldPaint)
                    canvas.drawText(amountStr, marginLeft + contentWidth - tableCellBoldPaint.measureText(amountStr) - 8f, y + 13f, tableCellBoldPaint)
                    y += 22f
                }
                is Section.GrandTotal -> {
                    y += 6f
                    // Draw a prominent total box
                    fillPaint.color = brandDark
                    val boxHeight = 32f
                    canvas.drawRoundRect(RectF(marginLeft, y, marginLeft + contentWidth, y + boxHeight), 4f, 4f, fillPaint)

                    val label = "GRAND TOTAL"
                    val amount = currencyFormat.format(section.amount)
                    val whitePaint = Paint(totalLabelPaint).apply { color = white }
                    val whiteAmountPaint = Paint(totalAmountPaint).apply { color = white }
                    canvas.drawText(label, marginLeft + 12f, y + 21f, whitePaint)
                    canvas.drawText(amount, marginLeft + contentWidth - whiteAmountPaint.measureText(amount) - 12f, y + 21f, whiteAmountPaint)
                    y += boxHeight + 8f
                }
                is Section.Signature -> {
                    y += 8f
                    // Signature line and label
                    val sigBoxWidth = 200f
                    val sigBoxHeight = 60f
                    val sigX = marginLeft

                    // Dashed line for signature
                    val dashPaint = Paint(linePaint).apply {
                        pathEffect = DashPathEffect(floatArrayOf(4f, 3f), 0f)
                        color = textMedium
                    }

                    if (signatureBitmap != null) {
                        val destRect = RectF(sigX, y, sigX + sigBoxWidth, y + sigBoxHeight)
                        canvas.drawBitmap(signatureBitmap!!, null, destRect, null)
                        y += sigBoxHeight + 4f
                    } else {
                        y += sigBoxHeight
                    }

                    canvas.drawLine(sigX, y, sigX + sigBoxWidth, y, dashPaint)
                    y += 12f
                    canvas.drawText("Signature", sigX, y, sigLabelPaint)

                    // Date next to signature — auto-populated with send date
                    val dateX = sigX + sigBoxWidth + 40f
                    val dateLineWidth = 140f
                    val sendDateStr = dateFormat.format(Date())
                    canvas.drawText(sendDateStr, dateX, y - 16f, tableCellBoldPaint)
                    canvas.drawLine(dateX, y - 12f, dateX + dateLineWidth, y - 12f, dashPaint)
                    canvas.drawText("Date", dateX, y, sigLabelPaint)

                    y += 16f
                }
                is Section.ReceiptReference -> {
                    y += 4f
                    val refText = "Receipt images and notes attached on the following ${section.count} page(s)."
                    subtitlePaint.color = textMedium
                    canvas.drawText(refText, marginLeft, y + 10f, subtitlePaint)
                    y += 18f
                }
                is Section.ReceiptImage -> {
                    y = drawReceiptImage(canvas, section.expense, y, maxY)
                }
                is Section.NotesList -> {
                    y = drawNotesList(canvas, section.notes, y, maxY)
                }
                is Section.Spacer -> {
                    y += section.height
                }
            }
        }
    }

    private fun drawExpenseTable(canvas: Canvas, items: List<ExpenseItemEntity>, startY: Float, maxY: Float): Float {
        var y = startY
        val colWidths = floatArrayOf(
            contentWidth * 0.16f,  // Date
            contentWidth * 0.18f,  // Category
            contentWidth * 0.46f,  // Description
            contentWidth * 0.20f   // Amount
        )
        val headers = arrayOf("Date", "Category", "Description", "Amount")
        val rowHeight = 20f

        // Header row
        fillPaint.color = tableHeaderBg
        canvas.drawRoundRect(RectF(marginLeft, y, marginLeft + contentWidth, y + rowHeight), 3f, 3f, fillPaint)
        var colX = marginLeft + 6f
        for (i in headers.indices) {
            if (i == headers.lastIndex) {
                // Right-align amount header
                canvas.drawText(headers[i], marginLeft + contentWidth - tableHeaderPaint.measureText(headers[i]) - 6f, y + 14f, tableHeaderPaint)
            } else {
                canvas.drawText(headers[i], colX, y + 14f, tableHeaderPaint)
            }
            colX += colWidths[i]
        }
        y += rowHeight

        // Data rows
        for ((idx, item) in items.withIndex()) {
            if (y + rowHeight > maxY) break

            // Alternating row background
            if (idx % 2 == 1) {
                fillPaint.color = tableAltRow
                canvas.drawRect(marginLeft, y, marginLeft + contentWidth, y + rowHeight, fillPaint)
            }

            colX = marginLeft + 6f
            canvas.drawText(dateFormat.format(Date(item.date)), colX, y + 14f, tableCellPaint)
            colX += colWidths[0]
            canvas.drawText(item.category.replaceFirstChar { it.uppercase() }, colX, y + 14f, tableCellPaint)
            colX += colWidths[1]

            // Truncate description to fit
            val maxDescWidth = colWidths[2] - 10f
            val desc = ellipsize(item.description, tableCellPaint, maxDescWidth)
            canvas.drawText(desc, colX, y + 14f, tableCellPaint)

            // Amount right-aligned
            val amountStr = currencyFormat.format(item.amount)
            canvas.drawText(amountStr, marginLeft + contentWidth - tableCellBoldPaint.measureText(amountStr) - 6f, y + 14f, tableCellBoldPaint)

            // Bottom border
            canvas.drawLine(marginLeft, y + rowHeight, marginLeft + contentWidth, y + rowHeight, linePaint)
            y += rowHeight
        }

        return y
    }

    private fun drawMileageTable(canvas: Canvas, entries: List<MileageEntryEntity>, startY: Float, maxY: Float): Float {
        var y = startY
        val colWidths = floatArrayOf(
            contentWidth * 0.18f,  // Date
            contentWidth * 0.20f,  // Miles
            contentWidth * 0.22f,  // Rate
            contentWidth * 0.20f,  // Amount
            contentWidth * 0.20f   // Notes
        )
        val headers = arrayOf("Date", "Miles", "Rate", "Amount", "Notes")
        val rowHeight = 20f

        // Header row
        fillPaint.color = tableHeaderBg
        canvas.drawRoundRect(RectF(marginLeft, y, marginLeft + contentWidth, y + rowHeight), 3f, 3f, fillPaint)
        var colX = marginLeft + 6f
        for (i in headers.indices) {
            if (i == 3) {
                // Right-align amount header
                val amtHeaderX = colX + colWidths[i] - tableHeaderPaint.measureText(headers[i]) - 6f
                canvas.drawText(headers[i], amtHeaderX, y + 14f, tableHeaderPaint)
            } else {
                canvas.drawText(headers[i], colX, y + 14f, tableHeaderPaint)
            }
            colX += colWidths[i]
        }
        y += rowHeight

        // Data rows
        for ((idx, entry) in entries.withIndex()) {
            if (y + rowHeight > maxY) break

            if (idx % 2 == 1) {
                fillPaint.color = tableAltRow
                canvas.drawRect(marginLeft, y, marginLeft + contentWidth, y + rowHeight, fillPaint)
            }

            colX = marginLeft + 6f
            canvas.drawText(dateFormat.format(Date(entry.date)), colX, y + 14f, tableCellPaint)
            colX += colWidths[0]
            canvas.drawText("${entry.distance}", colX, y + 14f, tableCellPaint)
            colX += colWidths[1]
            canvas.drawText("${currencyFormat.format(entry.rate)}/mi", colX, y + 14f, tableCellPaint)
            colX += colWidths[2]

            // Amount right-aligned within its column
            val amountStr = currencyFormat.format(entry.calculatedAmount)
            val amountX = colX + colWidths[3] - tableCellBoldPaint.measureText(amountStr) - 6f
            canvas.drawText(amountStr, amountX, y + 14f, tableCellBoldPaint)
            colX += colWidths[3]

            // Notes truncated
            val maxNoteWidth = colWidths[4] - 10f
            val note = ellipsize(entry.notes, tableCellPaint, maxNoteWidth)
            canvas.drawText(note, colX, y + 14f, tableCellPaint)

            canvas.drawLine(marginLeft, y + rowHeight, marginLeft + contentWidth, y + rowHeight, linePaint)
            y += rowHeight
        }

        return y
    }

    private fun drawReceiptImage(canvas: Canvas, expense: ExpenseItemEntity, startY: Float, maxY: Float): Float {
        var y = startY

        // Label
        val label = "${expense.category.replaceFirstChar { it.uppercase() }} - ${expense.description} (${currencyFormat.format(expense.amount)})"
        canvas.drawText(label, marginLeft, y + 13f, sectionTitlePaint)
        y += 22f

        // Load and draw receipt image
        expense.receiptImageUri?.let { uriStr ->
            try {
                val bitmap = loadAndScaleReceipt(uriStr)
                if (bitmap != null) {
                    val availableHeight = maxY - y - 20f
                    val availableWidth = contentWidth

                    val scale = minOf(
                        availableWidth / bitmap.width.toFloat(),
                        availableHeight / bitmap.height.toFloat(),
                        1f
                    )
                    val drawWidth = bitmap.width * scale
                    val drawHeight = bitmap.height * scale

                    // Center horizontally
                    val drawX = marginLeft + (contentWidth - drawWidth) / 2f

                    // Light border around image
                    fillPaint.color = white
                    canvas.drawRect(drawX - 1f, y - 1f, drawX + drawWidth + 1f, y + drawHeight + 1f, linePaint)
                    canvas.drawBitmap(bitmap, null, RectF(drawX, y, drawX + drawWidth, y + drawHeight), null)

                    y += drawHeight + 8f
                    bitmap.recycle()
                }
            } catch (_: Exception) {
                canvas.drawText("[Receipt image could not be loaded]", marginLeft, y + 12f, subtitlePaint)
                y += 20f
            }
        }

        return y
    }

    private fun drawNotesList(canvas: Canvas, notes: List<Pair<String, String>>, startY: Float, maxY: Float): Float {
        var y = startY

        for ((title, body) in notes) {
            if (y + 40f > maxY) break

            // Note title
            canvas.drawText(title, marginLeft, y + 12f, noteTitlePaint)
            y += 18f

            // Note body — word wrap
            val wrappedLines = wrapText(body, noteBodyPaint, contentWidth - 16f)
            for (line in wrappedLines) {
                if (y + 14f > maxY) break
                canvas.drawText(line, marginLeft + 8f, y + 12f, noteBodyPaint)
                y += 14f
            }

            // Subtle separator
            y += 6f
            canvas.drawLine(marginLeft, y, marginLeft + contentWidth * 0.3f, y, linePaint)
            y += 10f
        }

        return y
    }

    private fun loadAndScaleReceipt(uriStr: String): Bitmap? {
        return try {
            val maxDim = 1920

            // Try as content:// URI first, then fall back to file path
            if (uriStr.startsWith("content://")) {
                val uri = Uri.parse(uriStr)

                // First pass: get dimensions
                val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, boundsOptions)
                }

                var sampleSize = 1
                while (boundsOptions.outWidth / sampleSize > maxDim || boundsOptions.outHeight / sampleSize > maxDim) {
                    sampleSize *= 2
                }

                // Second pass: decode scaled bitmap
                val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, decodeOptions)
                }
            } else {
                // Plain file path
                val file = File(uriStr)
                if (!file.exists()) return null

                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(file.absolutePath, options)

                var sampleSize = 1
                while (options.outWidth / sampleSize > maxDim || options.outHeight / sampleSize > maxDim) {
                    sampleSize *= 2
                }

                val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
                BitmapFactory.decodeFile(file.absolutePath, decodeOptions)
            }
        } catch (_: Exception) { null }
    }

    private fun ellipsize(text: String, paint: Paint, maxWidth: Float): String {
        if (text.isEmpty()) return text
        if (paint.measureText(text) <= maxWidth) return text
        var end = text.length
        while (end > 0 && paint.measureText(text.substring(0, end) + "...") > maxWidth) {
            end--
        }
        return if (end > 0) text.substring(0, end) + "..." else "..."
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var current = ""

        for (word in words) {
            val test = if (current.isEmpty()) word else "$current $word"
            if (paint.measureText(test) <= maxWidth) {
                current = test
            } else {
                if (current.isNotEmpty()) lines.add(current)
                current = word
            }
        }
        if (current.isNotEmpty()) lines.add(current)
        return lines
    }

    // --- Data structures ---

    private data class PageContent(
        val isFirstPage: Boolean,
        val sections: MutableList<Section> = mutableListOf()
    )

    private sealed class Section {
        data class SectionTitle(val title: String) : Section()
        data class ExpenseTable(val items: List<ExpenseItemEntity>) : Section()
        data class MileageTable(val entries: List<MileageEntryEntity>) : Section()
        data class Subtotal(val label: String, val amount: Double) : Section()
        data class GrandTotal(val amount: Double) : Section()
        data object Signature : Section()
        data class ReceiptReference(val count: Int) : Section()
        data class ReceiptImage(val expense: ExpenseItemEntity) : Section()
        data class NotesList(val notes: List<Pair<String, String>>) : Section()
        data class Spacer(val height: Float) : Section()
    }
}

package com.expensetracker.ui.screen.signature

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.expensetracker.ui.component.PathPoint
import com.expensetracker.ui.component.SignatureCanvas
import com.expensetracker.ui.screen.settings.ControlPanelViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignatureCaptureScreen(
    onBack: () -> Unit,
    onSignatureSaved: () -> Unit,
    viewModel: ControlPanelViewModel = hiltViewModel()
) {
    val points = remember { mutableStateListOf<PathPoint>() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Capture Signature") },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Draw your signature below",
                style = MaterialTheme.typography.bodyLarge
            )

            SignatureCanvas(
                points = points,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { points.clear() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Clear")
                }
                Button(
                    onClick = {
                        scope.launch {
                            val uri = saveSignatureBitmap(context.filesDir, points.toList())
                            if (uri != null) {
                                viewModel.updateSignatureUri(uri)
                                onSignatureSaved()
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = points.isNotEmpty()
                ) {
                    Text("Save")
                }
            }
        }
    }
}

private suspend fun saveSignatureBitmap(filesDir: File, paths: List<PathPoint>): String? {
    if (paths.isEmpty()) return null
    return withContext(Dispatchers.IO) {
        val width = 800
        val height = 400
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        canvas.drawColor(android.graphics.Color.WHITE)

        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.BLACK
            strokeWidth = 4f
            style = android.graphics.Paint.Style.STROKE
            strokeCap = android.graphics.Paint.Cap.ROUND
            strokeJoin = android.graphics.Paint.Join.ROUND
            isAntiAlias = true
        }

        // Scale paths to bitmap size
        val maxX = paths.maxOf { it.x }.coerceAtLeast(1f)
        val maxY = paths.maxOf { it.y }.coerceAtLeast(1f)
        val scaleX = width.toFloat() / maxX
        val scaleY = height.toFloat() / maxY
        val scale = minOf(scaleX, scaleY) * 0.9f

        val path = android.graphics.Path()
        for (point in paths) {
            val x = point.x * scale
            val y = point.y * scale
            if (point.isNewStroke) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }
        canvas.drawPath(path, paint)

        val sigDir = File(filesDir, "signatures").apply { mkdirs() }
        val file = File(sigDir, "signature_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { fos ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
        }
        bitmap.recycle()
        file.absolutePath
    }
}

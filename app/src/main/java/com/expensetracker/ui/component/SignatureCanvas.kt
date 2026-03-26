package com.expensetracker.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

data class PathPoint(val x: Float, val y: Float, val isNewStroke: Boolean = false)

@Composable
fun SignatureCanvas(
    points: SnapshotStateList<PathPoint>,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .background(Color.White)
            .border(1.dp, MaterialTheme.colorScheme.outline)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        points.add(PathPoint(offset.x, offset.y, isNewStroke = true))
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        points.add(PathPoint(change.position.x, change.position.y))
                    }
                )
            }
    ) {
        if (points.isEmpty()) return@Canvas

        val drawPath = Path()
        var started = false

        for (point in points) {
            if (point.isNewStroke || !started) {
                drawPath.moveTo(point.x, point.y)
                started = true
            } else {
                drawPath.lineTo(point.x, point.y)
            }
        }

        drawPath(
            path = drawPath,
            color = Color.Black,
            style = Stroke(
                width = 4f,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}

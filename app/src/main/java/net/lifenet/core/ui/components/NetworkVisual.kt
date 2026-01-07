package net.lifenet.core.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import net.lifenet.core.ui.theme.CyanNeon
import net.lifenet.core.ui.theme.ElectricBlue

@Composable
fun NetworkVisual(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "Pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "Scale"
    )

    Canvas(modifier = modifier.fillMaxWidth().height(300.dp)) {
        val center = center
        val nodeCount = 6
        val radius = size.minDimension / 3

        // Draw Central Node (Ghost)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(ElectricBlue, Color.Transparent),
                center = center,
                radius = 100f * pulseScale
            ),
            radius = 60f * pulseScale,
            center = center
        )
        drawCircle(
            color = CyanNeon,
            radius = 10f,
            center = center
        )

        // Draw Satellite Nodes
        for (i in 0 until nodeCount) {
            val angle = (2 * Math.PI / nodeCount) * i
            val x = center.x + radius * Math.cos(angle).toFloat()
            val y = center.y + radius * Math.sin(angle).toFloat()
            val nodePos = Offset(x, y)

            // Connection Line
            drawLine(
                color = ElectricBlue.copy(alpha = 0.3f),
                start = center,
                end = nodePos,
                strokeWidth = 2f
            )

            // Node
            drawCircle(
                color = ElectricBlue,
                radius = 8f,
                center = nodePos
            )
            
            // Outer Ring
            drawCircle(
                color = ElectricBlue.copy(alpha = 0.1f),
                radius = 12f,
                center = nodePos,
                style = Stroke(width = 1f)
            )
        }
    }
}

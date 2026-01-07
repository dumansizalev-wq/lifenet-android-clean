package net.lifenet.core.ui.components

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import net.lifenet.core.ui.theme.ElectricBlue
import net.lifenet.core.ui.theme.GlassBorder
import net.lifenet.core.ui.theme.GlassSurface

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        GlassSurface.copy(alpha = 0.1f),
                        GlassSurface.copy(alpha = 0.05f)
                    )
                )
            )
            .border(
                BorderStroke(1.dp, Brush.linearGradient(
                    colors = listOf(
                        GlassBorder.copy(alpha = 0.5f),
                        Color.Transparent
                    )
                )),
                RoundedCornerShape(16.dp)
            )
            .padding(16.dp)
    ) {
        content()
    }
}

@Composable
fun BlurContainer(
    modifier: Modifier = Modifier,
    blur: Dp = 10.dp,
    content: @Composable () -> Unit
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Box(
            modifier = modifier
                .graphicsLayer {
                    renderEffect = RenderEffect
                        .createBlurEffect(
                            blur.toPx(),
                            blur.toPx(),
                            Shader.TileMode.DECAL
                        )
                        .asComposeRenderEffect()
                }
        ) {
            content()
        }
    } else {
        // Fallback for older Android
        Box(modifier = modifier) {
            content()
        }
    }
}

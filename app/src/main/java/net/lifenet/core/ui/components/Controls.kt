package net.lifenet.core.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.lifenet.core.ui.theme.ElectricBlue
import net.lifenet.core.ui.theme.NeonKapali
import net.lifenet.core.ui.theme.SuccessGreen

@Composable
fun NeonToggle(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedColor by animateColorAsState(
        if (checked) SuccessGreen else NeonKapali, label = "Color"
    )
    val scale by animateFloatAsState(if (checked) 1.05f else 1.0f, label = "Scale")

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(60.dp, 36.dp)
                .scale(scale)
                .shadow(8.dp, RoundedCornerShape(18.dp), ambientColor = animatedColor, spotColor = animatedColor)
                .clip(RoundedCornerShape(18.dp))
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable { onCheckedChange(!checked) }
                .padding(4.dp),
            contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
        ) {
            // Track
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(1.dp, animatedColor.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
            )
            // Thumb
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(animatedColor)
                    .shadow(4.dp, CircleShape)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = if (checked) ElectricBlue else Color.Gray,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun GlassAlertDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(title, color = ElectricBlue, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                Text(message, color = Color.White, fontSize = 16.sp, lineHeight = 24.sp)
                Spacer(modifier = Modifier.height(24.dp))
                
                Box(
                    modifier = Modifier
                        .clickable { onDismiss() }
                        .background(ElectricBlue.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    Text("TAMAM", color = ElectricBlue)
                }
            }
        }
    }
}

@Composable
fun InfoButton(
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.1f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = "Info",
            tint = ElectricBlue,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
fun StylisticGlassButton(
    text: String,
    isActive: Boolean,
    onClick: () -> Unit,
    onInfoClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    // Enhanced tactile scale
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        label = "ButtonScale"
    )
    
    val borderColor by animateColorAsState(
        targetValue = if (isActive) ElectricBlue else ElectricBlue.copy(alpha = 0.3f),
        label = "BorderColor"
    )

    // Gradient container for depth
    val containerBrush = if (isActive) {
        androidx.compose.ui.graphics.Brush.verticalGradient(
            colors = listOf(ElectricBlue.copy(alpha = 0.2f), ElectricBlue.copy(alpha = 0.05f))
        )
    } else {
        androidx.compose.ui.graphics.Brush.verticalGradient(
            colors = listOf(Color.White.copy(alpha = 0.05f), Color.Transparent)
        )
    }

    Box(
        modifier = modifier
            .scale(scale)
            .height(64.dp) // Slightly taller for better touch target
            .clip(RoundedCornerShape(16.dp))
            .background(containerBrush)
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null, 
                onClick = onClick
            )
            .padding(horizontal = 4.dp), // Reduce padding slightly to fit grid
        contentAlignment = Alignment.Center
    ) {
        // Inner depth shadow via subtle overlay
        if (!isPressed) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        androidx.compose.ui.graphics.Brush.linearGradient(
                            colors = listOf(Color.White.copy(alpha = 0.05f), Color.Transparent),
                            start = androidx.compose.ui.geometry.Offset(0f, 0f),
                            end = androidx.compose.ui.geometry.Offset(0f, 100f)
                        )
                    )
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 12.dp)
        ) {
            Text(
                text = text,
                color = if (isActive) Color.White else Color.Gray,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black, // Bold -> Black for impact
                letterSpacing = 1.sp,
                maxLines = 1
            )
            Spacer(modifier = Modifier.width(6.dp))
            
            // Integrated Info Icon
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.1f))
                    .clickable(onClick = onInfoClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Info",
                    tint = if (isActive) ElectricBlue else Color.Gray,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
        
        // Active Glow Effect
        if (isActive) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        androidx.compose.ui.graphics.Brush.radialGradient(
                            colors = listOf(ElectricBlue.copy(alpha = 0.15f), Color.Transparent),
                            radius = 120f
                        )
                    )
            )
        }
    }
}

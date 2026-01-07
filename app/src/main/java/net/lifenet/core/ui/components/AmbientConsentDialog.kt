package net.lifenet.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import net.lifenet.core.ui.theme.CyanNeon
import net.lifenet.core.ui.theme.DeepBlue
import net.lifenet.core.ui.theme.ElectricBlue

@Composable
fun AmbientConsentDialog(
    onConfirm: () -> Unit
) {
    Dialog(
        onDismissRequest = { /* Prevent dismissal */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = DeepBlue)
        ) {
            Column(
                modifier = Modifier
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(DeepBlue, Color(0xFF001F3F))
                        )
                    )
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Waves,
                    contentDescription = "Signal Surf",
                    tint = CyanNeon,
                    modifier = Modifier.size(48.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Evrensel Sinyal Sörfü",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = ElectricBlue
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "LIFENET, çevresel Wi-Fi sinyallerini (VSIE) kullanarak internetsiz iletişim ağı kurar.\n\n" +
                            "Bu özellik, tüm kullanıcılar için aktiftir ve cihazınızın çevresindeki sinyalleri anonim olarak okuyup iletmesini sağlar.\n\n" +
                            "Ağa katkıda bulunmak ve sinyal sörfüne izin vermek için lütfen onaylayın.",
                    color = Color.White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onConfirm,
                    colors = ButtonDefaults.buttonColors(containerColor = CyanNeon),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Sinyal Sörfüne İzin Ver", color = DeepBlue, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

package net.lifenet.core.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.Icon
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import net.lifenet.core.ui.components.*
import net.lifenet.core.ui.theme.*

@Composable
fun SettingsScreen() {
    var stealthMode by remember { mutableStateOf(false) }
    var lowPower by remember { mutableStateOf(false) }
    var dataSaver by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(DeepBlue, LighterBlue)
                )
            )
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        Text("AYARLAR", color = ElectricBlue, fontSize = 24.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
        Spacer(modifier = Modifier.height(24.dp))

        // Section 0.5: Profile
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(60.dp).clip(CircleShape).background(ElectricBlue.copy(alpha=0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Face, contentDescription = "Avatar", tint = Color.White, modifier = Modifier.size(40.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("KULLANICI: ALPHA_1", color = ElectricBlue, fontWeight = FontWeight.Bold)
                    Text("GÜVEN PUANI: 98/100", color = SuccessGreen, fontSize = 12.sp)
                    Text("RÜTBE: OPERATÖR", color = Color.Gray, fontSize = 12.sp)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))

        // Section 1: General
        Text("GENEL", color = CyanNeon, fontSize = 12.sp, modifier = Modifier.padding(start = 8.dp, bottom = 8.dp))

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(8.dp)) {
                SettingsRow("Düşük Güç Modu", lowPower) { lowPower = it }
                Spacer(modifier = Modifier.height(16.dp))
                SettingsRow("Veri Tasarrufu", dataSaver) { dataSaver = it }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Section 2: Privacy
        Text("GİZLİLİK", color = CyanNeon, fontSize = 12.sp, modifier = Modifier.padding(start = 8.dp, bottom = 8.dp))
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(8.dp)) {
                SettingsRow("Hayalet Modu (Stealth)", stealthMode) { stealthMode = it }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        // Version info
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text("LIFENET v5.0 // KULLANMA", color = Color.Gray.copy(alpha = 0.5f), fontSize = 10.sp)
        }
        
        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
fun SettingsRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.White, fontSize = 16.sp)
        NeonToggle("", checked, onCheckedChange, modifier = Modifier.scale(0.8f))
    }
}

fun Modifier.scale(scale: Float) = this.then(Modifier.graphicsLayer(scaleX = scale, scaleY = scale))

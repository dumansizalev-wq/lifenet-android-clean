package net.lifenet.core.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.lifenet.core.ui.components.*
import net.lifenet.core.ui.theme.*

@Composable
fun AutonomousScreen() {
    var showTooltip by remember { mutableStateOf<String?>(null) }
    var agentActive by remember { mutableStateOf(false) }
    var autoHealing by remember { mutableStateOf(true) }
    
    if (showTooltip != null) {
        GlassAlertDialog(
            title = "Bilgi",
            message = showTooltip!!,
            onDismiss = { showTooltip = null }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(DeepBlue, LighterBlue)
                )
            )
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        // 1. Autonomous Status Section
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("OTONOM AJAN DURUMU", color = ElectricBlue, fontSize = 12.sp)
                    InfoButton { showTooltip = "Cihazın ağ içindeki kendi durumunu ve aktif görevlerini gösterir." }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    if (agentActive) "AKTİF" else "BEKLEMEDE", 
                    color = if (agentActive) SuccessGreen else NeonKapali, 
                    fontSize = 24.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Network Map / Visualization
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
             NetworkVisual(modifier = Modifier.fillMaxSize())
             // Overlay Info Button
             Box(
                 modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
             ) {
                 InfoButton { showTooltip = "Ağdaki diğer düğümleri ve bağlantı durumlarını animasyonlu olarak görselleştirir." }
             }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. QoS and Resilience Info (New Section)
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("QoS ve DAYANIKLILIK", color = ElectricBlue, fontSize = 12.sp)
                    InfoButton { showTooltip = "Otonom ağ çekirdeğinin kalite ve dayanıklılık ölçümlerini kullanıcıya sunar." }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Güven Puanı: 98/100", color = Color.White, fontSize = 14.sp)
                    Text("Gecikme: 24ms", color = Color.White, fontSize = 14.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4. Control Toggles (Upgraded to StylisticGlassButton for consistency/tooltips)
        GlassCard(modifier = Modifier.fillMaxWidth()) {
             Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StylisticGlassButton(
                    text = "AJAN",
                    isActive = agentActive,
                    onClick = { agentActive = !agentActive },
                    onInfoClick = { showTooltip = "Cihazın otonom karar alma veya mesaj yönlendirme özelliklerini açıp kapatmaya yarar." },
                    modifier = Modifier.weight(1f)
                )
                StylisticGlassButton(
                    text = "İYİLEŞTİRME",
                    isActive = autoHealing,
                    onClick = { autoHealing = !autoHealing },
                    onInfoClick = { showTooltip = "Ağ kopmalarında otomatik onarım modülünü devreye alır." },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(80.dp))
    }
}

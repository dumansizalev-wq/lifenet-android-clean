package net.lifenet.core.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
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

data class SecurityAlert(val title: String, val time: String, val severity: String)

@Composable
fun SecurityScreen() {
    var encryptionEnabled by remember { mutableStateOf(true) }
    var firewallEnabled by remember { mutableStateOf(true) }
    
    val alerts = listOf(
        SecurityAlert("Yetkisiz Erişim Engellendi", "10:42", "High"),
        SecurityAlert("Port Taraması Tespit Edildi", "09:15", "Medium"),
        SecurityAlert("Güvenli Bağlantı Kuruldu", "08:30", "Low")
    )

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

        // Status Panel
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("GÜVENLİK DURUMU", color = ElectricBlue, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("MAKSİMUM", color = SuccessGreen, fontSize = 24.sp, letterSpacing = 1.sp)
                }
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Secure",
                    tint = SuccessGreen,
                    modifier = Modifier.size(40.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Toggles
        GlassCard(modifier = Modifier.fillMaxWidth()) {
             Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                NeonToggle("ŞİFRELEME", encryptionEnabled, { encryptionEnabled = it })
                NeonToggle("GÜVENLİK DUVARI", firewallEnabled, { firewallEnabled = it })
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Alerts List
        Text("GÜVENLİK KAYITLARI", color = ElectricBlue, fontSize = 14.sp, modifier = Modifier.align(Alignment.Start))
        Spacer(modifier = Modifier.height(8.dp))
        
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(alerts) { alert ->
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning, 
                            contentDescription = "Alert",
                            tint = if (alert.severity == "High") AlertRed else ElectricBlue,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(alert.title, color = Color.White, fontSize = 14.sp)
                            Text(alert.time, color = Color.Gray, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(80.dp))
    }
}

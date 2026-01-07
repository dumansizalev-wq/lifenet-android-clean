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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import androidx.core.content.ContextCompat
import net.lifenet.core.ui.components.*
import net.lifenet.core.ui.theme.DeepBlue
import net.lifenet.core.ui.theme.ElectricBlue
import net.lifenet.core.ui.theme.LighterBlue
import net.lifenet.core.ui.theme.SuccessGreen

@Composable
fun HomeScreen() {
    var wifiEnabled by remember { mutableStateOf(true) }
    var meshEnabled by remember { mutableStateOf(false) }
    var autonomousMode by remember { mutableStateOf(false) }
    var torEnabled by remember { mutableStateOf(false) }
    
    var showTooltip by remember { mutableStateOf<String?>(null) }

    if (showTooltip != null) {
        GlassAlertDialog(
            title = "Bilgi",
            message = showTooltip ?: "",
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
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        // 1. Status Panel (Durum Paneli)
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "SİSTEM DURUMU", color = ElectricBlue, fontSize = 12.sp)
                    InfoButton { showTooltip = "Sistem durumu, bağlantı kalitesi ve güvenlik protokollerinin aktiflik durumunu gösterir." }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (torEnabled) "GİZLİ BAĞLANTI" else if (wifiEnabled) "BAĞLI" else "BEKLEMEDE",
                    color = Color.White,
                    fontSize = 24.sp,
                    letterSpacing = 2.sp
                )
                Text(
                    text = if (torEnabled) "TOR / ANONİM MOD AKTİF" else "AĞ GÜVENLİĞİ AKTİF",
                    color = if (torEnabled) SuccessGreen else ElectricBlue.copy(alpha = 0.7f),
                    fontSize = 10.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 2. Animated Network Visual
        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
             NetworkVisual()
        }

        Spacer(modifier = Modifier.height(24.dp))

    // Contact Picker Integration
    val context = androidx.compose.ui.platform.LocalContext.current
    val contactLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.PickContact()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            // In a real app, query the name/number. For now, simulate connection.
            torEnabled = true
            showTooltip = "Güvenli hat başlatılıyor: ${uri.lastPathSegment}"
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            contactLauncher.launch(null)
        } else {
            showTooltip = "Kişilere erişim izni reddedildi. Güvenli arama başlatılamaz."
        }
    }

    // ... UI Structure ...
        // 3. Controls (Buttons)
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StylisticGlassButton(
                        text = "MESH",
                        isActive = meshEnabled,
                        onClick = { meshEnabled = !meshEnabled },
                        onInfoClick = { showTooltip = "Mesh Ağı Bağlantısı: Cihazlar arası doğrudan veri köprüsü." },
                        modifier = Modifier.weight(1f)
                    )
                    StylisticGlassButton(
                        text = "WIFI",
                        isActive = wifiEnabled,
                        onClick = { wifiEnabled = !wifiEnabled },
                        onInfoClick = { showTooltip = "WiFi Modülü: Yerel ağ taraması ve bağlantısı." },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StylisticGlassButton(
                        text = "OTONOM",
                        isActive = autonomousMode,
                        onClick = { autonomousMode = !autonomousMode },
                        onInfoClick = { showTooltip = "Otonom Ajan: Arka siber güvenlik ve optimizasyon asistanı." },
                        modifier = Modifier.weight(1f)
                    )
                    StylisticGlassButton(
                        text = "TOR / SES",
                        isActive = torEnabled,
                        onClick = { 
                            if (torEnabled) {
                                torEnabled = false
                                showTooltip = "Güvenli hat sonlandırıldı."
                            } else {
                                // Check permission
                                val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                                    context, 
                                    android.Manifest.permission.READ_CONTACTS
                                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                
                                if (hasPermission) {
                                    contactLauncher.launch(null)
                                } else {
                                    permissionLauncher.launch(android.Manifest.permission.READ_CONTACTS)
                                }
                            }
                        },
                        onInfoClick = { showTooltip = "Anonim Sesli Görüşme: Kişi seçerek güvenli P2P arama başlatın." },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(80.dp)) // Bottom padding for Nav Bar
    }
}

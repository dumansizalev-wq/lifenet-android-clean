package net.lifenet.core.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.lifenet.core.controller.NetworkMode
import net.lifenet.core.voice.VoicePacket

// ---------- MESSAGE TELEMETRY ----------
data class Telemetry(
    val hops: Int,
    val ttlMinutes: Int,
    val node: String
)

@Composable
fun TelemetryRow(t: Telemetry) {
    Row {
        Text("[Hop:${t.hops}] ", fontSize = 10.sp, color = Color.White)
        Text("[TTL:${t.ttlMinutes}m] ", fontSize = 10.sp, color = Color.White)
        Text("[Node:${t.node}]", fontSize = 10.sp, color = Color.White)
    }
}


// ---------- COMPOSE UI ----------
@Composable
fun LifeneDashboard(
    mode: NetworkMode,
    peerCount: Int,
    onionAddress: String?,
    onToggle: (Boolean) -> Unit,
    onStartTorCall: () -> Unit
) {
    // Crossfade or Content Transform
    AnimatedContent(
        targetState = mode, 
        label = "DashboardModeTransition"
    ) { state ->
        when (state) {
            NetworkMode.ASTRA -> AstraUI(onionAddress, onToggle, onStartTorCall)
            NetworkMode.HOP_TO_HOP -> HopToHopUI(peerCount, onToggle)
        }
    }
}

@Composable
fun AstraUI(onionAddress: String?, onToggle: (Boolean) -> Unit, onStartCall: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A1AFF)) // Deep Blue
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("ASTRA MODE", color = Color.Cyan, fontSize = 24.sp)
                ToggleButton(true, onToggle)
            }
            Text("Connected to Tor Network", color = Color.White)
            Text(
                 text = onionAddress?.let { "My Address: $it" } ?: "Initializing Tor...", 
                 color = if (onionAddress != null) Color.Green else Color.Gray, 
                 fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(onClick = { onStartCall() }) {
                Text("Start Secure Call (Tor)")
            }
        }
    }
}

@Composable
fun HopToHopUI(peerCount: Int, onToggle: (Boolean) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black) // Dark Mode
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
             Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("HOP-TO-HOP MODE", color = Color.Red, fontSize = 24.sp)
                ToggleButton(false, onToggle)
            }
            Text("Offline Mesh Active", color = Color.White)
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = { /* Push to Talk Logic */ },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
            ) {
                Text("PUSH TO TALK")
            }
            
            Spacer(modifier = Modifier.height(10.dp))
            // Dummy Waveform Data for Visualization (Simulates noise)
            val dummyWave = List(50) { kotlin.math.sin(it * 0.5).toFloat() * 0.5f + 0.5f }
            Waveform(samples = dummyWave)
            
            Spacer(modifier = Modifier.height(10.dp))
            // Live Telemetry
            TelemetryRow(Telemetry(3, 5, "Peer-A"))
            
            Spacer(modifier = Modifier.height(30.dp))
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                // Minimum 1 node (Self) or just peers
                val displayCount = if (peerCount > 0) peerCount else 1
                RadialNodes(count = displayCount) 
            }
            Text("Active Peers: $peerCount", color = Color.Green, modifier = Modifier.align(androidx.compose.ui.Alignment.CenterHorizontally))
        }
    }
}

@Composable
fun RadialNodes(count: Int) {
    androidx.compose.foundation.Canvas(modifier = Modifier.size(200.dp)) {
        repeat(count) {
            drawCircle(
                color = Color.Green,
                radius = 6f,
                center = androidx.compose.ui.geometry.Offset(
                    size.width / 2 + kotlin.math.cos(it * 2 * kotlin.math.PI / count).toFloat() * 140, // Scaled for visibility
                    size.height / 2 + kotlin.math.sin(it * 2 * kotlin.math.PI / count).toFloat() * 140
                )
            )
        }
    }
}

@Composable
fun Waveform(samples: List<Float>) {
    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxWidth().height(60.dp)) {
        val widthPerSample = size.width / samples.size
        samples.forEachIndexed { i, v ->
            drawLine(
                color = Color.Cyan,
                start = androidx.compose.ui.geometry.Offset(i * widthPerSample, size.height / 2),
                end = androidx.compose.ui.geometry.Offset(i * widthPerSample, size.height / 2 - v * size.height / 2), // Scaled amplitude
                strokeWidth = 2f
            )
        }
    }
}

@Composable
fun ToggleButton(isOn: Boolean, onToggle: (Boolean) -> Unit) {
    Switch(
        checked = isOn,
        onCheckedChange = { onToggle(it) },
        colors = SwitchDefaults.colors(
            checkedThumbColor = Color.Cyan,
            uncheckedThumbColor = Color.Red,
            checkedTrackColor = Color.Blue,
            uncheckedTrackColor = Color.DarkGray
        )
    )
}

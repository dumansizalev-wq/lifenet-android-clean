package net.lifenet.core.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.lifenet.core.contact.ContactManager
import net.lifenet.core.messaging.MeshMessenger
import net.lifenet.core.ui.components.*
import net.lifenet.core.ui.theme.*

data class ChatMessage(val sender: String, val content: String, val isMe: Boolean, val ttl: Int = 0, val hops: Int = 0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(deviceId: String, navController: androidx.navigation.NavController? = null) {
    val contact = remember { ContactManager.findContact(deviceId) }
    val displayName = contact?.displayName ?: "Bilinmeyen Cihaz"
    
    val messages = remember { mutableStateListOf<ChatMessage>() }
    var inputText by remember { mutableStateOf("") }
    
    // SSOT: Observe Repository Flow
    // This handles both initial load AND updates automatically.
    val allMessages by net.lifenet.core.data.MessageRepository.messagesFlow.collectAsState()
    
    // Filter for THIS chat
    val displayMessages = remember(allMessages) {
        allMessages.filter {
            it.targetId == deviceId || it.senderId == deviceId || (it.targetId == "BROADCAST")
        }.sortedBy { it.timestamp }
    }
    
    // Populate UI Models
    LaunchedEffect(displayMessages) {
        messages.clear()
        displayMessages.forEach { 
             messages.add(ChatMessage(
                 sender = if (it.senderId == deviceId) displayName else "Ben",
                 content = it.content,
                 isMe = (it.senderId != deviceId),
                 ttl = it.ttl,
                 hops = it.hops
             ))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(displayName, color = ElectricBlue, fontSize = 18.sp)
                        Text(deviceId, color = Color.Gray, fontSize = 10.sp)
                    }
                },
                navigationIcon = {
                     IconButton(onClick = { navController?.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = ElectricBlue)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DeepBlue,
                    titleContentColor = ElectricBlue,
                    navigationIconContentColor = ElectricBlue
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(DeepBlue, LighterBlue)
                    )
                )
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Message List
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                reverseLayout = false
            ) {
                items(messages) { msg ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (msg.isMe) Arrangement.End else Arrangement.Start
                    ) {
                        Column(horizontalAlignment = if (msg.isMe) Alignment.End else Alignment.Start) {
                             Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (msg.isMe) ElectricBlue else GlassSurface)
                                    .padding(12.dp)
                            ) {
                                Text(msg.content, color = Color.White)
                            }
                            // Telemetry Footer
                            if (!msg.isMe) {
                                Text("TTL: ${msg.ttl} | Hops: ${msg.hops}", color = Color.Gray, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
    
            Spacer(modifier = Modifier.height(8.dp))
    
            // Input
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.weight(1f),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = GlassSurface,
                        unfocusedContainerColor = GlassSurface,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                IconButton(onClick = {
                    if (inputText.isNotBlank()) {
                        messages.add(ChatMessage("Ben", inputText, true))
                        MeshMessenger.sendMessage(deviceId, inputText)
                        inputText = ""
                    }
                }) {
                    Icon(Icons.Default.Send, contentDescription = "Send", tint = ElectricBlue)
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

package net.lifenet.core.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.barcode.common.Barcode
import androidx.core.content.ContextCompat
import androidx.compose.ui.window.Dialog
import java.util.concurrent.Executors
import android.util.Size
import net.lifenet.core.contact.ContactManager
import net.lifenet.core.messaging.MeshMessenger
import net.lifenet.core.data.LifenetContact
import net.lifenet.core.ui.theme.*

@Composable
fun MessagesScreen(navController: NavController? = null) {
    // Contact List Screen
    var showAddDialog by remember { mutableStateOf(false) }
    var showQRDialog by remember { mutableStateOf(false) }
    var contacts by remember { mutableStateOf(ContactManager.getAllContacts()) }

    if (showAddDialog) {
        var newId by remember { mutableStateOf("") }
        var newName by remember { mutableStateOf("") }
        
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Manuel Kişi Ekle") },
            text = {
                Column {
                    TextField(value = newId, onValueChange = { newId = it }, label = { Text("Cihaz ID (Hex)") })
                    Spacer(modifier = Modifier.height(8.dp))
                    TextField(value = newName, onValueChange = { newName = it }, label = { Text("İsim") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (newId.isNotBlank() && newName.isNotBlank()) {
                        ContactManager.addContact(LifenetContact(newId, newName))
                        contacts = ContactManager.getAllContacts() // Refresh
                        showAddDialog = false
                    }
                }) {
                    Text("Ekle")
                }
            }
        )
    }
    
    // Discovered Peers from ViewModel
    // val discoveredPeers by MeshMessenger.discoveredPeers.collectAsState() - Removed in favor of ViewModel
    var showChoiceDialog by remember { mutableStateOf(false) }
    var showNearbyDialog by remember { mutableStateOf(false) }

    // Choice Dialog
    if (showChoiceDialog) {
        AlertDialog(
            onDismissRequest = { showChoiceDialog = false },
            title = { Text("Kişi Ekleme Yöntemi") },
            text = { Text("Kişiyi nasıl eklemek istersiniz?") },
            confirmButton = {
                TextButton(onClick = { 
                    showChoiceDialog = false
                    showNearbyDialog = true 
                }) {
                    Text("Yakındakini Ekle (Wi-Fi Aware)")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showChoiceDialog = false
                    showQRDialog = true 
                }) {
                    Text("Uzaktakini Ekle (QR/ID)")
                }
            }
        )
    }

    // Nearby Dialog
    if (showNearbyDialog) {
         AlertDialog(
            onDismissRequest = { showNearbyDialog = false },
            title = { Text("Yakındaki Cihazlar") },
            text = {
                LazyColumn {
                    if (discoveredPeers.isEmpty()) {
                        item { Text("Cihaz aranıyor...", color = Color.Gray) }
                    } else {
                        items(nearbyDevices) { device ->
                             Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp)
                                    .clickable {
                                        ContactManager.addContact(LifenetContact(device.address, "User-${device.address.take(4)}"))
                                        contacts = ContactManager.getAllContacts()
                                        showNearbyDialog = false
                                    }
                            ) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = ElectricBlue)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(device.name, fontSize = 14.sp, color = Color.White)
                                    Text(device.address, fontSize = 10.sp, color = Color.Gray)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showNearbyDialog = false }) { Text("Kapat") }
            }
        )
    }

    // Real CameraX QR Dialog
    if (showQRDialog) {
        Dialog(onDismissRequest = { showQRDialog = false }) {
            QRScannerDialog(
                onDismiss = { showQRDialog = false },
                onContactDetected = { id, name ->
                    if (id.length == 12 || id.length == 24) {
                         ContactManager.addContact(LifenetContact(id, name))
                         contacts = ContactManager.getAllContacts()
                        showQRDialog = false
                    }
                }
            )
        }
    }

    // VSIE/BLE Peers from ViewModel (Passed via composition in real app, but for now we rely on the ViewModel being scoped to Activity/NavGraph)
    // Ideally we pass the ViewModel or State. For this fix, let's assuming we are observing the MeshMessenger or a global state if ViewModel isn't passed.
    // However, MessagesScreen signature is (NavController).
    // Let's rely on MeshMessenger.discoveredPeers for now if it's wired, OR fix the VsieManager usage. 
    // Since VsieManager is an instance, we can't access it statically.
    // We will use ContactManager for existing contacts.
    // For Nearby, we will use a workaround or simply rely on MeshMessenger if it was updated. 
    
    // BUT user asked to "Show live discovered peers".
    // Let's use the viewModel if possible. We can grab it via `androidx.lifecycle.viewmodel.compose.viewModel`.
    
    val viewModel: net.lifenet.core.ui.viewmodel.DashboardViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val nearbyDevices by viewModel.nearbyDevices.observeAsState(emptyList())
    
    // Filter for Display
    // Note: nearbyDevices contains both BLE and VSIE
    val discoveredPeers = nearbyDevices.map { it.address } // Just IDs for list

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showChoiceDialog = true },
                containerColor = ElectricBlue,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Contact")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(brush = Brush.verticalGradient(listOf(DeepBlue, LighterBlue)))
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            
            // Mailbox Indicator
            val vsieCount = nearbyDevices.count { it.type == "VSIE" }
            if (vsieCount > 0) {
                 Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .background(Color(0xFF003300), RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Email, contentDescription = "Mailbox", tint = Color.Green)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "${nearbyDevices.count { it.type == "VSIE" }} Passive Mailboxes Detected", 
                        color = Color.Green, 
                        fontSize = 12.sp
                    )
                }
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("KİŞİLER", color = ElectricBlue, fontSize = 20.sp)
            }
            
            LazyColumn {
                items(contacts) { contact ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = GlassSurface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                navController?.navigate("chat/${contact.deviceId}")
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = CyanNeon)
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(contact.displayName, color = Color.White, fontSize = 16.sp)
                                Text(contact.deviceId, color = Color.Gray, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}


@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class) 
@Composable
fun QRScannerDialog(
    onDismiss: () -> Unit,
    onContactDetected: (String, String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val previewView = remember { PreviewView(context) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AndroidView({ previewView }, modifier = Modifier.fillMaxSize())
        
        // Overlay / Close Button
        IconButton(
             onClick = onDismiss,
             modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(32.dp)) 
        }
    }

    LaunchedEffect(cameraProviderFuture) {
        val cameraProvider = cameraProviderFuture.get()
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }

        val barcodeScanner = BarcodeScanning.getClient()

        val analysisUseCase = ImageAnalysis.Builder()
            .setTargetResolution(Size(1280, 720))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also { useCase ->
                useCase.setAnalyzer(ContextCompat.getMainExecutor(context)) { imageProxy ->
                    val mediaImage = imageProxy.image
                    if (mediaImage != null) {
                        val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                        barcodeScanner.process(inputImage)
                            .addOnSuccessListener { barcodes ->
                                for (barcode in barcodes) {
                                    val rawValue = barcode.rawValue
                                    // QR format: LIFENET:ID:NAME
                                    if (!rawValue.isNullOrEmpty() && rawValue.startsWith("LIFENET:")) {
                                        val parts = rawValue.split(":")
                                        if (parts.size >= 3) {
                                            onContactDetected(parts[1], parts[2])
                                        }
                                    }
                                }
                            }
                            .addOnCompleteListener { imageProxy.close() }
                    } else {
                        imageProxy.close()
                    }
                }
            }

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysisUseCase
            )
        } catch (exc: Exception) {
            exc.printStackTrace()
        }
    }
}

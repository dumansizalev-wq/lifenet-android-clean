package net.lifenet.core.voice

import android.annotation.SuppressLint
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import kotlinx.coroutines.*
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.Socket

class TorVoiceEngine(private val torManager: net.lifenet.core.tor.TorManager) {
    
    // Production: This would come from Tor OnionProxyManager
    val myOnionAddress: String?
        get() = torManager.onionAddress
        
    private var socket: Socket? = null
    private val codec = OpusCodec()
    // Scope for the call duration
    private var callScope: CoroutineScope? = null

    @SuppressLint("MissingPermission")
    private val minTrackBufferSize = AudioTrack.getMinBufferSize(16000, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
    
    @SuppressLint("MissingPermission")
    private val audioTrack = AudioTrack(
        AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build(),
        AudioFormat.Builder()
            .setSampleRate(16000)
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build(),
        maxOf(minTrackBufferSize, 1280), // Low Latency Buffer (approx 80ms min)
        AudioTrack.MODE_STREAM,
        AudioManager.AUDIO_SESSION_ID_GENERATE
    ).apply { play() }

    @SuppressLint("MissingPermission")
    fun startCall(onion: String, onCallFailed: (() -> Unit)? = null) {
        // Cancel any existing call first
        disconnect()
        
        callScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        
        callScope?.launch {
            try {
                Log.d("TorVoice", "Connecting to $onion...")
                val proxy = Proxy(Proxy.Type.SOCKS, InetSocketAddress("127.0.0.1", 9050))
                socket = Socket(proxy).apply {
                    connect(InetSocketAddress(onion, 12345), 20000)
                    keepAlive = true
                    tcpNoDelay = true // CRITICAL: Disable Nagle for RTT < 200ms
                    soTimeout = 0 // Infinite read timeout (we block until voice comes)
                }
                Log.i("TorVoice", "Pipe Established! Full Duplex Active.")

                val input = socket!!.getInputStream()
                val output = socket!!.getOutputStream()

                // TX Loop
                val txJob = launch {
                    val bufferSize = AudioRecord.getMinBufferSize(16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
                    val recorder = AudioRecord(
                        MediaRecorder.AudioSource.MIC,
                        16000,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        bufferSize
                    )
                    
                    try {
                        recorder.startRecording()
                        val pcmBuffer = ShortArray(320) // 20ms
                        val headerBuffer = ByteArray(3) // Magic(1) + Len(2)

                        while (isActive && socket?.isConnected == true) {
                             val read = recorder.read(pcmBuffer, 0, pcmBuffer.size)
                             if (read > 0) {
                                 val encoded = codec.encode(pcmBuffer)
                                 if (encoded.isNotEmpty()) {
                                     // Protocol: [MAGIC 0x78] [LEN_HI] [LEN_LO] [PAYLOAD]
                                     headerBuffer[0] = 0x78.toByte() // ASCII 'x'
                                     headerBuffer[1] = ((encoded.size shr 8) and 0xFF).toByte()
                                     headerBuffer[2] = (encoded.size and 0xFF).toByte()
                                     
                                     output.write(headerBuffer)
                                     output.write(encoded)
                                 }
                             }
                        }
                    } catch (e: Exception) {
                        Log.e("TorVoice", "TX Broken", e)
                        throw e 
                    } finally {
                        try { recorder.stop() } catch (e: Exception) {}
                        recorder.release()
                    }
                }

                // RX Loop (Robust Reassembly)
                val rxJob = launch {
                    val headerByte = ByteArray(1)
                    val lengthHeader = ByteArray(2)
                    
                    try {
                        audioTrack.play()
                        while (isActive && socket?.isConnected == true) {
                            // 1. Scan for MAGIC (0x78)
                            var seeking = true
                            while (seeking) {
                                val r = input.read(headerByte)
                                if (r == -1) throw java.io.EOFException("Stream End")
                                if (headerByte[0] == 0x78.toByte()) {
                                    seeking = false
                                } else {
                                    Log.w("TorVoice", "Frame slip! Seeking sync...")
                                }
                            }

                            // 2. Read Length
                            var totalRead = 0
                            while (totalRead < 2) {
                                val r = input.read(lengthHeader, totalRead, 2 - totalRead)
                                if (r == -1) throw java.io.EOFException("Stream End")
                                totalRead += r
                            }
                            
                            val length = ((lengthHeader[0].toInt() and 0xFF) shl 8) or (lengthHeader[1].toInt() and 0xFF)
                            
                            // 3. Read Body
                            val payload = ByteArray(length)
                            totalRead = 0
                            while (totalRead < length) {
                                val r = input.read(payload, totalRead, length - totalRead)
                                if (r == -1) throw java.io.EOFException("Stream End")
                                totalRead += r
                            }

                            // 4. Decode & Play
                            try {
                                val pcm = codec.decode(payload)
                                audioTrack.write(pcm, 0, pcm.size)
                            } catch (e: Exception) {
                                Log.e("TorVoice", "Bad Frame", e) // Loss tolerance: Skip frame, don't crash RX
                            }
                        }
                    } catch (e: Exception) {
                         Log.e("TorVoice", "RX Broken", e)
                         throw e 
                    }
                }
                
                // Wait for either to fail or cancel
                joinAll(txJob, rxJob)

            } catch (e: Exception) {
               Log.e("TorVoice", "Call Terminated: ${e.message}")
               try { onCallFailed?.invoke() } catch (ex: Exception) {}
            } finally {
                disconnect() // Ensure cleanup
            }
        }
    }

    fun disconnect() {
        try {
            socket?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        callScope?.cancel()
        callScope = null
    }
}

package net.lifenet.core.voice

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.media.AudioManager
import android.media.AudioAttributes
import net.lifenet.core.transport.vsie.VsieManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
import java.util.UUID

class HopVoiceEngine(
    private val vsieManager: VsieManager,
    private val scope: CoroutineScope
) {
    private val codec = OpusCodec()
    private var seq = 0
    private val deviceId = UUID.randomUUID().toString() 
    private var isTransmitting = false

    private val jitter = JitterBuffer()
    
    @SuppressLint("MissingPermission")
    private val audioTrack = AudioTrack(
        AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
            .build(),
        AudioFormat.Builder()
            .setSampleRate(16000)
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build(),
        16000,
        AudioTrack.MODE_STREAM,
        AudioManager.AUDIO_SESSION_ID_GENERATE
    ).apply { play() }

    @SuppressLint("MissingPermission")
    fun startTransmit() {
        if (isTransmitting) return
        isTransmitting = true
        
        scope.launch(Dispatchers.IO) {
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
                val pcmData = ShortArray(320) // 20ms at 16kHz
                
                while (isActive && isTransmitting) {
                    val read = recorder.read(pcmData, 0, pcmData.size)
                    if (read > 0) {
                        val encoded = codec.encode(pcmData)
                        val packet = VoicePacket(
                            streamId = deviceId,
                            seq = seq++,
                            ttl = 3,
                            hop = 0,
                            payload = encoded
                        )
                        vsieManager.broadcastVoice(packet)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                try { recorder.stop() } catch (e: Exception) {}
                recorder.release()
            }
        }
    }

    fun stopTransmit() {
        isTransmitting = false
    }

    // Real Receiver Logic with Jitter Buffer
    fun onReceive(packet: VoicePacket) {
        jitter.push(packet)

        val playable = jitter.pop() ?: return
        val pcm = codec.decode(playable.payload)
        audioTrack.write(pcm, 0, pcm.size)

        vsieManager.relayVoice(
            playable.copy(ttl = playable.ttl - 1, hop = playable.hop + 1)
        )
    }
}

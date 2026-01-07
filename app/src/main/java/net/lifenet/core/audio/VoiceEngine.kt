package net.lifenet.core.audio

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class VoiceEngine(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private var player: MediaPlayer? = null
    private var currentOutputFile: File? = null

    /**
     * Starts recording a PTT voice clip in AMR-NB format.
     */
    fun startRecording(): File? {
        val outputFile = File(context.cacheDir, "ptt_${System.currentTimeMillis()}.amr")
        currentOutputFile = outputFile

        recorder = MediaRecorder(context).apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.AMR_NB)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            setAudioSamplingRate(8000)
            setAudioEncodingBitRate(4750) // Ultra-low bandwidth
            setOutputFile(outputFile.absolutePath)

            try {
                prepare()
                start()
            } catch (e: IOException) {
                Log.e("VoiceEngine", "prepare() failed", e)
                return null
            }
        }
        return outputFile
    }

    /**
     * Stops current recording and returns the file.
     */
    fun stopRecording(): File? {
        try {
            recorder?.apply {
                stop()
                release()
            }
        } catch (e: RuntimeException) {
            Log.e("VoiceEngine", "stop() failed", e)
        }
        recorder = null
        return currentOutputFile
    }

    /**
     * Plays a received AMR voice clip from bytes.
     */
    fun playVoiceClip(audioData: ByteArray) {
        val tempFile = File(context.cacheDir, "temp_play.amr")
        FileOutputStream(tempFile).use { it.write(audioData) }

        player = MediaPlayer().apply {
            try {
                setDataSource(tempFile.absolutePath)
                prepare()
                start()
                setOnCompletionListener {
                    it.release()
                    tempFile.delete()
                }
            } catch (e: IOException) {
                Log.e("VoiceEngine", "playVoiceClip failed", e)
            }
        }
    }
}

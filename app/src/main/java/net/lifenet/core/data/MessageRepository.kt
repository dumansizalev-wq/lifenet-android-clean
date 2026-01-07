package net.lifenet.core.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

object MessageRepository {
    private const val FILE_NAME = "lifenet_messages.json"
    private val messages = mutableListOf<LifenetMessage>()
    private val gson = Gson()
    private lateinit var messagesFile: File

    private val _messagesFlow = MutableStateFlow<List<LifenetMessage>>(emptyList())
    val messagesFlow: StateFlow<List<LifenetMessage>> = _messagesFlow

    fun initialize(context: Context) {
        messagesFile = File(context.filesDir, FILE_NAME)
        loadMessages()
    }

    private fun loadMessages() {
        if (messagesFile.exists()) {
            val json = messagesFile.readText()
            val type = object : TypeToken<List<LifenetMessage>>() {}.type
            val loaded: List<LifenetMessage>? = gson.fromJson(json, type)
            if (loaded != null) {
                messages.clear()
                messages.addAll(loaded)
                _messagesFlow.value = messages.toList()
            }
        }
    }

    private fun saveMessages() {
        val json = gson.toJson(messages)
        messagesFile.writeText(json)
        _messagesFlow.value = messages.toList()
    }

    fun addMessage(message: LifenetMessage) {
        messages.add(message)
        saveMessages()
    }

    fun updateMessageStatus(id: String, status: MessageStatus) {
        val msg = messages.find { it.id == id }
        if (msg != null) {
            msg.status = status
            saveMessages()
        }
    }
    
    fun getMessagesForDevice(deviceId: String): List<LifenetMessage> {
        return messages.filter { 
            it.targetId == deviceId || it.senderId == deviceId || it.targetId == "BROADCAST"
        }.sortedBy { it.timestamp }
    }
}

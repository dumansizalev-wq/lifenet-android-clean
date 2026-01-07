package net.lifenet.core.controller

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.lifenet.core.transport.vsie.VsieManager
import net.lifenet.core.voice.TorVoiceEngine
import net.lifenet.core.voice.HopVoiceEngine

enum class NetworkMode { ASTRA, HOP }

class ModeController {

    private val _mode = MutableStateFlow(NetworkMode.ASTRA)
    val mode: StateFlow<NetworkMode> = _mode.asStateFlow()

    fun switchToHop() {
        _mode.value = NetworkMode.HOP
    }

    fun switchToAstra() {
        _mode.value = NetworkMode.ASTRA
    }
}

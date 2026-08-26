package com.atruedev.kmpnfc.sample.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.atruedev.kmpnfc.error.NfcException
import com.atruedev.kmpnfc.hce.AidCategory
import com.atruedev.kmpnfc.hce.AidRegistration
import com.atruedev.kmpnfc.hce.DeactivationException
import com.atruedev.kmpnfc.hce.HceCapabilities
import com.atruedev.kmpnfc.hce.HceConfig
import com.atruedev.kmpnfc.hce.HceService
import com.atruedev.kmpnfc.tag.ApduCommand
import com.atruedev.kmpnfc.tag.ApduResponse
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ApduLogEntry(
    val direction: String,
    val summary: String,
)

class HceViewModel(
    private val hce: HceService,
) : ViewModel() {
    val capabilities: HceCapabilities = hce.capabilities

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _logs = MutableStateFlow<List<ApduLogEntry>>(emptyList())
    val logs: StateFlow<List<ApduLogEntry>> = _logs.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var sessionJob: Job? = null

    fun start(aid: String) {
        if (_isRunning.value) return
        _error.value = null
        _logs.value = emptyList()
        sessionJob =
            viewModelScope.launch {
                _isRunning.value = true
                try {
                    hce.start(
                        HceConfig(
                            aids =
                                listOf(
                                    AidRegistration(
                                        aid = aid.uppercase(),
                                        category = AidCategory.OTHER,
                                    ),
                                ),
                        ),
                    ) { command ->
                        appendLog("RX", describeCommand(command))
                        val response =
                            when (command.ins) {
                                0xA4.toByte() -> ApduResponse.success(byteArrayOf(0x01, 0x02, 0x03))
                                0xCA.toByte() -> ApduResponse.success("kmp-nfc".encodeToByteArray())
                                else -> ApduResponse.instructionNotSupported()
                            }
                        appendLog("TX", "SW=${response.sw1.toString(16)}${response.sw2.toString(16)}")
                        response
                    }
                } catch (e: DeactivationException) {
                    appendLog("SYS", "Reader disconnected: ${e.reason}")
                } catch (e: NfcException) {
                    _error.value = e.error.message
                } catch (e: Exception) {
                    _error.value = e.message ?: "HCE session failed"
                } finally {
                    _isRunning.value = false
                }
            }
    }

    fun stop() {
        hce.stop()
        sessionJob?.cancel()
        sessionJob = null
        _isRunning.value = false
    }

    private fun appendLog(
        direction: String,
        summary: String,
    ) {
        _logs.value = _logs.value + ApduLogEntry(direction, summary)
    }

    private fun describeCommand(command: ApduCommand): String =
        "CLA=${command.cla.toString(16)} INS=${command.ins.toString(16)} " +
            "P1=${command.p1.toString(16)} P2=${command.p2.toString(16)}"

    override fun onCleared() {
        stop()
    }
}

package com.atruedev.kmpnfc.sample.viewmodel

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.atruedev.kmpnfc.error.NdefFormatError
import com.atruedev.kmpnfc.error.NfcException
import com.atruedev.kmpnfc.error.TransceiveError
import com.atruedev.kmpnfc.ndef.NdefMessage
import com.atruedev.kmpnfc.ndef.ndefMessage
import com.atruedev.kmpnfc.reader.NfcTag
import com.atruedev.kmpnfc.sample.util.parseHexBytes
import com.atruedev.kmpnfc.sample.util.toHexString
import com.atruedev.kmpnfc.tag.TagTechnology
import com.atruedev.kmpnfc.tag.TagType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TagViewModel(
    private val tag: NfcTag,
) : ViewModel() {
    val identifier: ByteArray = tag.identifier
    val type: TagType = tag.type
    val technologies: Set<TagTechnology> = tag.technologies

    private val _ndefMessage = MutableStateFlow<NdefMessage?>(null)
    val ndefMessage: StateFlow<NdefMessage?> = _ndefMessage.asStateFlow()

    private val _lastApduResponse = MutableStateFlow<ByteArray?>(null)
    val lastApduResponse: StateFlow<ByteArray?> = _lastApduResponse.asStateFlow()

    private val _error = MutableStateFlow<NfcException?>(null)
    val error: StateFlow<NfcException?> = _error.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun readNdef() {
        viewModelScope.launch {
            runTagOp {
                _ndefMessage.value = tag.readNdef()
            }
        }
    }

    fun writeNdef(
        uri: String?,
        text: String?,
    ) {
        viewModelScope.launch {
            runTagOp {
                val message =
                    ndefMessage {
                        if (!uri.isNullOrBlank()) uri(uri.trim())
                        if (!text.isNullOrBlank()) text(text.trim())
                    }
                require(message.records.isNotEmpty()) { "Enter a URI or text to write" }
                tag.writeNdef(message)
                _ndefMessage.value = message
            }
        }
    }

    fun transceive(commandHex: String) {
        viewModelScope.launch {
            runTagOp {
                val bytes =
                    commandHex.parseHexBytes()
                        ?: throw NfcException(TransceiveError("Invalid hex APDU command"))
                _lastApduResponse.value = tag.transceive(bytes)
            }
        }
    }

    private suspend fun runTagOp(block: suspend () -> Unit) {
        _isLoading.value = true
        _error.value = null
        try {
            block()
        } catch (e: NfcException) {
            _error.value = e
        } catch (e: IllegalArgumentException) {
            _error.value = NfcException(NdefFormatError(e.message ?: "Invalid input"))
        } finally {
            _isLoading.value = false
        }
    }
}

fun tagViewModelKey(tag: NfcTag): String = tag.identifier.toHexString("")

@Composable
fun rememberTagViewModel(tag: NfcTag): TagViewModel = viewModel(key = tagViewModelKey(tag)) { TagViewModel(tag) }

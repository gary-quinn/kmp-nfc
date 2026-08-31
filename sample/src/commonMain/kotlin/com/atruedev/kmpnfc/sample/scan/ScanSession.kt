package com.atruedev.kmpnfc.sample.scan

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.atruedev.kmpnfc.adapter.NfcAdapter
import com.atruedev.kmpnfc.error.NfcException
import com.atruedev.kmpnfc.reader.NfcTag
import com.atruedev.kmpnfc.reader.ReaderOptions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ScanSnapshot(
    val isScanning: Boolean = false,
    val lastTag: NfcTag? = null,
    val scanError: NfcException? = null,
)

class ScanSession(
    private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow(ScanSnapshot())
    val state: StateFlow<ScanSnapshot> = _state.asStateFlow()

    private var scanJob: Job? = null

    fun start(
        adapter: NfcAdapter,
        options: ReaderOptions,
    ) {
        stop(closeTag = true)
        _state.update { ScanSnapshot(isScanning = true) }
        scanJob =
            scope.launch {
                try {
                    adapter.tags(options).collect { tag ->
                        _state.update { snapshot ->
                            if (snapshot.lastTag !== tag) {
                                snapshot.lastTag?.close()
                            }
                            snapshot.copy(lastTag = tag, scanError = null)
                        }
                        if (!options.isMultiTagSession) {
                            stop(closeTag = false)
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: NfcException) {
                    _state.update { it.copy(scanError = e) }
                    stop(closeTag = false)
                }
            }
    }

    fun stop(closeTag: Boolean = true) {
        scanJob?.cancel()
        scanJob = null
        _state.update { snapshot ->
            if (closeTag) {
                snapshot.lastTag?.close()
                snapshot.copy(isScanning = false, lastTag = null)
            } else {
                snapshot.copy(isScanning = false)
            }
        }
    }
}

@Composable
fun rememberScanSession(): ScanSession {
    val scope = rememberCoroutineScope()
    return remember(scope) { ScanSession(scope) }
}

package com.atruedev.kmpnfc.sample

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.atruedev.kmpnfc.adapter.NfcAdapter
import com.atruedev.kmpnfc.hce.HceService
import com.atruedev.kmpnfc.reader.ReaderOptions
import com.atruedev.kmpnfc.sample.model.Screen
import com.atruedev.kmpnfc.sample.scan.rememberScanSession
import com.atruedev.kmpnfc.sample.ui.components.NfcAdapterBanner
import com.atruedev.kmpnfc.sample.ui.screens.ApduConsoleScreen
import com.atruedev.kmpnfc.sample.ui.screens.CapabilitiesScreen
import com.atruedev.kmpnfc.sample.ui.screens.HceServerScreen
import com.atruedev.kmpnfc.sample.ui.screens.HomeScreen
import com.atruedev.kmpnfc.sample.ui.screens.NdefReaderScreen
import com.atruedev.kmpnfc.sample.ui.screens.NdefWriterScreen
import com.atruedev.kmpnfc.sample.ui.screens.SimulateTagScreen
import com.atruedev.kmpnfc.sample.ui.screens.TagDetailScreen
import com.atruedev.kmpnfc.testing.FakeNfcAdapter

@Composable
fun App() {
    val scope = rememberCoroutineScope()
    val scanSession = rememberScanSession()
    val realAdapter = remember { NfcAdapter() }
    val fakeAdapter = remember { FakeNfcAdapter() }
    var simulateMode by remember { mutableStateOf(false) }
    val adapter: NfcAdapter = if (simulateMode) fakeAdapter else realAdapter
    val hce = remember { HceService() }
    var readerOptions by remember { mutableStateOf(ReaderOptions(alertMessage = "Hold near NFC tag")) }
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }

    SampleTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                NfcAdapterBanner(adapter)

                when (val screen = currentScreen) {
                    Screen.Home ->
                        HomeScreen(
                            adapter = adapter,
                            capabilities = adapter.capabilities,
                            simulateMode = simulateMode,
                            scanSession = scanSession,
                            readerOptions = readerOptions,
                            onReaderOptionsChange = { readerOptions = it },
                            onSimulateModeChange = { simulateMode = it },
                            onTagSelected = { currentScreen = Screen.TagDetail(it) },
                            onCapabilities = { currentScreen = Screen.Capabilities },
                            onHceServer = { currentScreen = Screen.HceServer },
                            onSimulateTag = { currentScreen = Screen.SimulateTag },
                        )

                    Screen.Capabilities ->
                        CapabilitiesScreen(
                            capabilities = adapter.capabilities,
                            onBack = { currentScreen = Screen.Home },
                        )

                    Screen.HceServer ->
                        HceServerScreen(
                            hce = hce,
                            onBack = { currentScreen = Screen.Home },
                        )

                    Screen.SimulateTag ->
                        SimulateTagScreen(
                            fakeAdapter = if (simulateMode) fakeAdapter else null,
                            scanSession = scanSession,
                            scope = scope,
                            onBack = { currentScreen = Screen.Home },
                            onTagEmitted = { currentScreen = Screen.Home },
                        )

                    is Screen.TagDetail ->
                        TagDetailScreen(
                            tag = screen.tag,
                            onBack = { currentScreen = Screen.Home },
                            onNdefReader = { currentScreen = Screen.NdefReader(screen.tag) },
                            onNdefWriter = { currentScreen = Screen.NdefWriter(screen.tag) },
                            onApduConsole = { currentScreen = Screen.ApduConsole(screen.tag) },
                        )

                    is Screen.NdefReader ->
                        NdefReaderScreen(
                            tag = screen.tag,
                            onBack = { currentScreen = Screen.TagDetail(screen.tag) },
                        )

                    is Screen.NdefWriter ->
                        NdefWriterScreen(
                            tag = screen.tag,
                            onBack = { currentScreen = Screen.TagDetail(screen.tag) },
                        )

                    is Screen.ApduConsole ->
                        ApduConsoleScreen(
                            tag = screen.tag,
                            onBack = { currentScreen = Screen.TagDetail(screen.tag) },
                        )
                }
            }
        }
    }

    DisposableEffect(realAdapter) {
        onDispose {
            scanSession.stop()
            realAdapter.close()
            fakeAdapter.close()
            hce.stop()
        }
    }
}

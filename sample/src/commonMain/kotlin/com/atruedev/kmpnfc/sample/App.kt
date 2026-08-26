package com.atruedev.kmpnfc.sample

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.atruedev.kmpnfc.adapter.NfcAdapter
import com.atruedev.kmpnfc.hce.HceService
import com.atruedev.kmpnfc.sample.model.Screen
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
    val realAdapter = remember { NfcAdapter() }
    val fakeAdapter = remember { FakeNfcAdapter() }
    var simulateMode by remember { mutableStateOf(false) }
    val adapter: NfcAdapter = if (simulateMode) fakeAdapter else realAdapter
    val hce = remember { HceService() }
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }

    SampleTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                NfcAdapterBanner(adapter)

                when (val screen = currentScreen) {
                    Screen.Home ->
                        HomeScreen(
                            adapter = adapter,
                            fakeAdapter = if (simulateMode) fakeAdapter else null,
                            capabilities = adapter.capabilities,
                            simulateMode = simulateMode,
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
                            adapter = adapter,
                            fakeAdapter = if (simulateMode) fakeAdapter else null,
                            onBack = { currentScreen = Screen.Home },
                            onTagDiscovered = { currentScreen = Screen.TagDetail(it) },
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
            realAdapter.close()
            fakeAdapter.close()
            hce.stop()
        }
    }
}

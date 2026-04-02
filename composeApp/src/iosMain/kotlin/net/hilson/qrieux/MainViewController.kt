package net.hilson.qrieux

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIImage

// Legacy entry points (kept for screenshot mode)
fun MainViewController() = ComposeUIViewController { App() }
fun MainViewControllerWithImage(image: UIImage) = ComposeUIViewController { App(sharedImage = image) }
fun MainViewControllerWithText(text: String) = ComposeUIViewController { App(sharedText = text) }

// Tab-specific entry points for SwiftUI TabView
fun makeScanViewController() = ComposeUIViewController { ScanScreen() }
fun makeScanViewControllerWithImage(image: UIImage) = ComposeUIViewController { ScanScreen(sharedImage = image) }
fun makeCreateViewController() = ComposeUIViewController { CreateScreen() }
fun makeCreateViewControllerWithText(text: String) = ComposeUIViewController { CreateScreen(initialText = text) }
fun makeHelpViewController() = ComposeUIViewController { HelpScreenHost() }

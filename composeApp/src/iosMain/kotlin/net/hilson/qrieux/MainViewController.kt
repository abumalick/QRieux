package net.hilson.qrieux

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIImage

// Tab-specific entry points for SwiftUI TabView
fun makeScanViewController() = ComposeUIViewController { ScanScreen() }
fun makeScanViewControllerWithImage(image: UIImage) = ComposeUIViewController { ScanScreen(sharedImage = image) }
fun makeCreateViewController() = ComposeUIViewController { CreateScreen() }
fun makeCreateViewControllerWithText(text: String) = ComposeUIViewController { CreateScreen(initialText = text) }
fun makeHelpViewController() = ComposeUIViewController { HelpScreenHost() }

package net.hilson.qrieux

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIImage

fun MainViewController() = ComposeUIViewController { App() }

fun MainViewControllerWithImage(image: UIImage) = ComposeUIViewController { App(sharedImage = image) }

fun MainViewControllerWithText(text: String) = ComposeUIViewController { App(sharedText = text) }

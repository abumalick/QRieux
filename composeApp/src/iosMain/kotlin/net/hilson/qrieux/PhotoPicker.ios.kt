package net.hilson.qrieux

import androidx.compose.runtime.*
import kotlinx.cinterop.ExperimentalForeignApi
import platform.PhotosUI.*
import platform.UIKit.*
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

private fun getRootViewController(): UIViewController? {
    val scenes = UIApplication.sharedApplication.connectedScenes
    val windowScene = scenes.firstOrNull {
        (it as? UIWindowScene)?.activationState == UISceneActivationStateForegroundActive
    } as? UIWindowScene
    return windowScene?.windows?.firstOrNull { (it as? UIWindow)?.isKeyWindow() == true }
        ?.let { (it as? UIWindow)?.rootViewController }
}

@OptIn(ExperimentalForeignApi::class)
@Composable
fun PhotoPickerLauncher(
    onImageSelected: (UIImage?) -> Unit,
    onDismiss: () -> Unit
) {
    val currentOnImageSelected by rememberUpdatedState(onImageSelected)
    val currentOnDismiss by rememberUpdatedState(onDismiss)

    val delegate = remember {
        PhotoPickerDelegate(
            onResult = { image ->
                dispatch_async(dispatch_get_main_queue()) {
                    if (image != null) {
                        currentOnImageSelected(image)
                    } else {
                        currentOnDismiss()
                    }
                }
            }
        )
    }

    DisposableEffect(Unit) {
        val configuration = PHPickerConfiguration()
        configuration.filter = PHPickerFilter.imagesFilter
        configuration.selectionLimit = 1

        val picker = PHPickerViewController(configuration = configuration)
        picker.delegate = delegate

        getRootViewController()?.presentViewController(picker, animated = true, completion = null)

        onDispose {}
    }
}

@OptIn(ExperimentalForeignApi::class)
private class PhotoPickerDelegate(
    private val onResult: (UIImage?) -> Unit
) : NSObject(), PHPickerViewControllerDelegateProtocol {

    override fun picker(picker: PHPickerViewController, didFinishPicking: List<*>) {
        picker.dismissViewControllerAnimated(true, null)

        val result = didFinishPicking.firstOrNull() as? PHPickerResult
        if (result == null) {
            onResult(null)
            return
        }

        val itemProvider = result.itemProvider
        val typeIdentifier = "public.image"

        if (itemProvider.hasItemConformingToTypeIdentifier(typeIdentifier)) {
            itemProvider.loadDataRepresentationForTypeIdentifier(typeIdentifier) { data, error ->
                if (error == null && data != null) {
                    val image = UIImage(data = data)
                    onResult(image)
                } else {
                    onResult(null)
                }
            }
        } else {
            onResult(null)
        }
    }
}

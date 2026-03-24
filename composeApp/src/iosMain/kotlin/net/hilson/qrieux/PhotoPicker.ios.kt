package net.hilson.qrieux

import androidx.compose.runtime.*
import kotlinx.cinterop.ExperimentalForeignApi
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
        ImagePickerDelegate(
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
        val rootVC = getRootViewController()
        if (rootVC != null) {
            val picker = UIImagePickerController()
            picker.sourceType = UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypePhotoLibrary
            picker.delegate = delegate
            rootVC.presentViewController(picker, animated = true, completion = null)
        } else {
            dispatch_async(dispatch_get_main_queue()) { currentOnDismiss() }
        }

        onDispose {}
    }
}

@OptIn(ExperimentalForeignApi::class)
private class ImagePickerDelegate(
    private val onResult: (UIImage?) -> Unit
) : NSObject(), UIImagePickerControllerDelegateProtocol, UINavigationControllerDelegateProtocol {

    override fun imagePickerController(
        picker: UIImagePickerController,
        didFinishPickingMediaWithInfo: Map<Any?, *>
    ) {
        picker.dismissViewControllerAnimated(true, null)
        val image = didFinishPickingMediaWithInfo[UIImagePickerControllerOriginalImage] as? UIImage
        onResult(image)
    }

    override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
        picker.dismissViewControllerAnimated(true, null)
        onResult(null)
    }
}

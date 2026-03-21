import UIKit
import SwiftUI
import ComposeApp

struct ComposeView: UIViewControllerRepresentable {
    var sharedImage: UIImage?
    var sharedText: String?

    func makeUIViewController(context: Context) -> UIViewController {
        if let image = sharedImage {
            return MainViewControllerKt.MainViewControllerWithImage(image: image)
        }
        if let text = sharedText {
            return MainViewControllerKt.MainViewControllerWithText(text: text)
        }
        return MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    @Binding var sharedImage: UIImage?
    @Binding var sharedText: String?

    var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()
            ComposeView(sharedImage: sharedImage, sharedText: sharedText)
                .ignoresSafeArea()
                .id(viewId)
        }
    }

    private var viewId: String {
        if let image = sharedImage {
            return "shared-image-\(ObjectIdentifier(image))"
        }
        if let text = sharedText {
            return "shared-text-\(text.hashValue)"
        }
        return "default"
    }
}

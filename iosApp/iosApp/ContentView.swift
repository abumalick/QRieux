import UIKit
import SwiftUI
import ComposeApp

struct ComposeView: UIViewControllerRepresentable {
    var sharedImage: UIImage?

    func makeUIViewController(context: Context) -> UIViewController {
        if let image = sharedImage {
            return MainViewControllerKt.MainViewControllerWithImage(image: image)
        }
        return MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    @Binding var sharedImage: UIImage?

    var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()
            ComposeView(sharedImage: sharedImage)
                .ignoresSafeArea()
                .id(sharedImage == nil ? "default" : "shared-\(ObjectIdentifier(sharedImage!))")
        }
    }
}

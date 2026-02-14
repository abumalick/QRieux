import SwiftUI
import UIKit

@main
struct iOSApp: App {
    // Keep in sync with ShareViewController
    private let appGroupID = "group.net.hilson.qrieux"
    private let sharedImageName = "shared-image.jpg"

    @Environment(\.scenePhase) private var scenePhase
    @State private var sharedImage: UIImage?

    init() {
        UIView.appearance().backgroundColor = .clear
    }

    var body: some Scene {
        WindowGroup {
            ContentView(sharedImage: $sharedImage)
                .preferredColorScheme(.dark)
                .onChange(of: scenePhase) { phase in
                    if phase == .active {
                        loadSharedImage()
                    }
                }
        }
    }

    private func loadSharedImage() {
        guard let containerURL = FileManager.default.containerURL(
            forSecurityApplicationGroupIdentifier: appGroupID
        ) else { return }

        let fileURL = containerURL.appendingPathComponent(sharedImageName)
        guard let image = UIImage(contentsOfFile: fileURL.path) else { return }

        sharedImage = image
        try? FileManager.default.removeItem(at: fileURL)
    }
}

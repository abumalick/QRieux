import SwiftUI
import UIKit

@main
struct iOSApp: App {
    // Keep in sync with ShareViewController
    private let appGroupID = "group.net.hilson.qrieux"
    private let sharedImageName = "shared-image.jpg"

    @Environment(\.scenePhase) private var scenePhase
    @State private var sharedImage: UIImage?
    @State private var sharedText: String?

    init() {
        UIView.appearance().backgroundColor = .clear
    }

    var body: some Scene {
        WindowGroup {
            ContentView(sharedImage: $sharedImage, sharedText: $sharedText)
                .preferredColorScheme(.dark)
                .onChange(of: scenePhase) { phase in
                    if phase == .active {
                        loadSharedImage()
                    }
                }
                .onOpenURL { url in
                    handleURL(url)
                }
        }
    }

    private func handleURL(_ url: URL) {
        if url.host == "shared-image" {
            loadSharedImage()
        } else if url.host == "create",
                  let components = URLComponents(url: url, resolvingAgainstBaseURL: false),
                  let text = components.queryItems?.first(where: { $0.name == "text" })?.value {
            sharedText = text
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

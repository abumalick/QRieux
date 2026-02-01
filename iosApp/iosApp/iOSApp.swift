import SwiftUI
import UIKit

@main
struct iOSApp: App {
    init() {
        // Set default window background to black for edge-to-edge camera
        UIView.appearance().backgroundColor = .clear
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .preferredColorScheme(.dark)
        }
    }
}

import UIKit
import SwiftUI
import ComposeApp

// MARK: - Tab-specific Compose hosts

private struct ScanHost: UIViewControllerRepresentable {
    var sharedImage: UIImage?

    func makeUIViewController(context: Context) -> UIViewController {
        if let image = sharedImage {
            return MainViewControllerKt.makeScanViewControllerWithImage(image: image)
        }
        return MainViewControllerKt.makeScanViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

private struct CreateHost: UIViewControllerRepresentable {
    var sharedText: String?

    func makeUIViewController(context: Context) -> UIViewController {
        if let text = sharedText {
            return MainViewControllerKt.makeCreateViewControllerWithText(text: text)
        }
        return MainViewControllerKt.makeCreateViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

private struct HistoryHost: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        return MainViewControllerKt.makeHistoryViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

private struct HelpHost: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        return MainViewControllerKt.makeHelpViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

// MARK: - Main TabView

enum AppTab: Hashable {
    case scan, create, history, help
}

struct ContentView: View {
    @Binding var sharedImage: UIImage?
    @Binding var sharedText: String?
    @Binding var selectedTab: AppTab
    @State private var historyViewId = UUID()

    var body: some View {
        TabView(selection: $selectedTab) {
            ScanHost(sharedImage: sharedImage)
                .ignoresSafeArea()
                .tabItem {
                    Label(NSLocalizedString("tab_scan", comment: ""), systemImage: "qrcode.viewfinder")
                }
                .tag(AppTab.scan)
                .id(scanViewId)

            CreateHost(sharedText: sharedText)
                .ignoresSafeArea()
                .tabItem {
                    Label(NSLocalizedString("tab_create", comment: ""), systemImage: "plus.circle")
                }
                .tag(AppTab.create)
                .id(createViewId)

            HistoryHost()
                .ignoresSafeArea()
                .tabItem {
                    Label(NSLocalizedString("tab_history", comment: ""), systemImage: "clock.arrow.circlepath")
                }
                .tag(AppTab.history)
                .id(historyViewId)

            HelpHost()
                .ignoresSafeArea()
                .tabItem {
                    Label(NSLocalizedString("tab_help", comment: ""), systemImage: "questionmark.circle")
                }
                .tag(AppTab.help)
        }
        .onChange(of: sharedImage) { image in
            if image != nil { selectedTab = .scan }
        }
        .onChange(of: sharedText) { text in
            if text != nil { selectedTab = .create }
        }
        .onChange(of: selectedTab) { tab in
            if tab == .history { historyViewId = UUID() }
        }
    }

    // Recreate Compose views when shared content changes
    private var scanViewId: String {
        if let image = sharedImage {
            return "scan-\(ObjectIdentifier(image))"
        }
        return "scan-default"
    }

    private var createViewId: String {
        if let text = sharedText {
            return "create-\(text.hashValue)"
        }
        return "create-default"
    }
}

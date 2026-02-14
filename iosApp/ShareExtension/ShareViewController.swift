import UIKit
import UniformTypeIdentifiers

class ShareViewController: UIViewController {

    // Keep in sync with iOSApp
    private let appGroupID = "group.net.hilson.qrieux"
    private let sharedImageName = "shared-image.jpg"

    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = .clear
        view.isOpaque = false
        processSharedImage()
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        clearBackgrounds()
    }

    private func clearBackgrounds() {
        var current: UIView? = view
        while let v = current {
            v.backgroundColor = .clear
            v.isOpaque = false
            current = v.superview
        }
        view.window?.backgroundColor = .clear
        view.window?.rootViewController?.view.backgroundColor = .clear
    }

    private func processSharedImage() {
        guard let item = extensionContext?.inputItems.first as? NSExtensionItem,
              let provider = item.attachments?.first(where: {
                  $0.hasItemConformingToTypeIdentifier(UTType.image.identifier)
              }) else {
            showAlert(
                title: NSLocalizedString("share_error", comment: ""),
                message: NSLocalizedString("share_error_read", comment: "")
            )
            return
        }

        provider.loadItem(forTypeIdentifier: UTType.image.identifier) { [weak self] data, _ in
            DispatchQueue.main.async {
                self?.handleLoadedItem(data)
            }
        }
    }

    private func handleLoadedItem(_ data: NSSecureCoding?) {
        var image: UIImage?
        if let url = data as? URL {
            image = UIImage(contentsOfFile: url.path)
        } else if let uiImage = data as? UIImage {
            image = uiImage
        } else if let imageData = data as? Data {
            image = UIImage(data: imageData)
        }

        guard let image,
              let jpegData = image.jpegData(compressionQuality: 0.9),
              let containerURL = FileManager.default.containerURL(
                  forSecurityApplicationGroupIdentifier: appGroupID
              ) else {
            showAlert(
                title: NSLocalizedString("share_error", comment: ""),
                message: NSLocalizedString("share_error_process", comment: "")
            )
            return
        }

        let fileURL = containerURL.appendingPathComponent(sharedImageName)
        do {
            try jpegData.write(to: fileURL)
            showAlert(
                title: NSLocalizedString("share_image_shared", comment: ""),
                message: NSLocalizedString("share_open_app", comment: "")
            )
        } catch {
            showAlert(
                title: NSLocalizedString("share_error", comment: ""),
                message: NSLocalizedString("share_error_save", comment: "")
            )
        }
    }

    private func showAlert(title: String, message: String) {
        let alert = UIAlertController(title: title, message: message, preferredStyle: .alert)
        alert.addAction(UIAlertAction(title: NSLocalizedString("share_ok", comment: ""), style: .default) { [weak self] _ in
            self?.extensionContext?.completeRequest(returningItems: nil)
        })
        present(alert, animated: true)
    }
}

import UIKit
import UniformTypeIdentifiers

class ShareViewController: UIViewController {

    // Keep in sync with iOSApp
    private let appGroupID = "group.net.hilson.qrieux"
    private let sharedImageName = "shared-image.jpg"
    private let sharedTextName = "shared-text.txt"

    private let vcardIdentifiers = [
        UTType.vCard.identifier,
        "public.vcard",
        "public.contact"
    ]

    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = .clear
        view.isOpaque = false
        processSharedContent()
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

    private func processSharedContent() {
        guard let item = extensionContext?.inputItems.first as? NSExtensionItem,
              let attachments = item.attachments, !attachments.isEmpty else {
            showAlert(
                title: NSLocalizedString("share_error", comment: ""),
                message: NSLocalizedString("share_error_read", comment: "")
            )
            return
        }

        if let imageProvider = attachments.first(where: { $0.hasItemConformingToTypeIdentifier(UTType.image.identifier) }) {
            imageProvider.loadItem(forTypeIdentifier: UTType.image.identifier) { [weak self] data, _ in
                DispatchQueue.main.async { self?.handleLoadedImage(data) }
            }
        } else if let (vcardProvider, vcardTypeId) = findVCardProvider(in: attachments) {
            vcardProvider.loadItem(forTypeIdentifier: vcardTypeId) { [weak self] data, error in
                DispatchQueue.main.async { self?.handleLoadedVCard(data) }
            }
        } else if let urlProvider = attachments.first(where: { $0.hasItemConformingToTypeIdentifier(UTType.url.identifier) }) {
            urlProvider.loadItem(forTypeIdentifier: UTType.url.identifier) { [weak self] data, _ in
                DispatchQueue.main.async {
                    if let url = data as? URL {
                        self?.openAppWithText(url.absoluteString)
                    } else {
                        self?.showError()
                    }
                }
            }
        } else if let textProvider = attachments.first(where: { $0.hasItemConformingToTypeIdentifier(UTType.plainText.identifier) }) {
            textProvider.loadItem(forTypeIdentifier: UTType.plainText.identifier) { [weak self] data, _ in
                DispatchQueue.main.async {
                    if let text = data as? String {
                        self?.openAppWithText(text)
                    } else {
                        self?.showError()
                    }
                }
            }
        } else {
            // No recognized type — try loading first attachment as data
            let provider = attachments[0]
            let typeId = provider.registeredTypeIdentifiers.first ?? UTType.data.identifier
            provider.loadItem(forTypeIdentifier: typeId) { [weak self] data, _ in
                DispatchQueue.main.async {
                    // Try to interpret as vCard text
                    if let vcardString = self?.extractString(from: data),
                       vcardString.contains("BEGIN:VCARD") {
                        self?.handleVCardString(vcardString)
                    } else {
                        self?.showError()
                    }
                }
            }
        }
    }

    private func findVCardProvider(in attachments: [NSItemProvider]) -> (NSItemProvider, String)? {
        for att in attachments {
            for typeId in vcardIdentifiers {
                if att.hasItemConformingToTypeIdentifier(typeId) {
                    return (att, typeId)
                }
            }
        }
        return nil
    }

    // MARK: - Image handling

    private func handleLoadedImage(_ data: NSSecureCoding?) {
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
            showError()
            return
        }

        let fileURL = containerURL.appendingPathComponent(sharedImageName)
        do {
            try jpegData.write(to: fileURL)
            openApp(urlString: "qrieux://shared-image")
        } catch {
            showAlert(
                title: NSLocalizedString("share_error", comment: ""),
                message: NSLocalizedString("share_error_save", comment: "")
            )
        }
    }

    // MARK: - vCard handling

    private func handleLoadedVCard(_ data: NSSecureCoding?) {
        guard let vcardString = extractString(from: data), !vcardString.isEmpty else {
            showError()
            return
        }
        handleVCardString(vcardString)
    }

    private func handleVCardString(_ vcard: String) {
        let stripped = stripVCard(vcard)
        if stripped.isEmpty {
            showError()
            return
        }
        saveTextAndOpenApp(stripped)
    }

    private func extractString(from data: NSSecureCoding?) -> String? {
        if let data = data as? Data {
            return String(data: data, encoding: .utf8) ?? String(data: data, encoding: .ascii)
        } else if let url = data as? URL {
            if let rawData = try? Data(contentsOf: url) {
                return String(data: rawData, encoding: .utf8) ?? String(data: rawData, encoding: .ascii)
            }
        } else if let string = data as? String {
            return string
        }
        return nil
    }

    /// Keep only QR-friendly fields: name, phone, email, org, title, address.
    /// Strip photos, notes, and other large binary fields that exceed QR capacity.
    private func stripVCard(_ vcard: String) -> String {
        let allowedPrefixes = [
            "BEGIN:", "END:", "VERSION:", "N:", "N;", "FN:", "FN;",
            "TEL:", "TEL;", "EMAIL:", "EMAIL;", "ORG:", "ORG;",
            "TITLE:", "TITLE;", "ADR:", "ADR;", "URL:", "URL;",
            "BDAY:", "NOTE:"
        ]

        let lines = vcard.components(separatedBy: .newlines)
        var result: [String] = []
        var skipContinuation = false

        for line in lines {
            let trimmed = line.trimmingCharacters(in: .whitespaces)
            if trimmed.isEmpty { continue }

            if line.hasPrefix(" ") || line.hasPrefix("\t") {
                if !skipContinuation {
                    result.append(line)
                }
                continue
            }

            let upperLine = trimmed.uppercased()
            let allowed = allowedPrefixes.contains { upperLine.hasPrefix($0) }
            skipContinuation = !allowed
            if allowed {
                result.append(trimmed)
            }
        }

        return result.joined(separator: "\r\n")
    }

    // MARK: - Text handling

    private func openAppWithText(_ text: String) {
        var components = URLComponents()
        components.scheme = "qrieux"
        components.host = "create"
        components.queryItems = [URLQueryItem(name: "text", value: text)]
        guard let urlString = components.string else {
            showError()
            return
        }
        openApp(urlString: urlString)
    }

    private func saveTextAndOpenApp(_ text: String) {
        guard let containerURL = FileManager.default.containerURL(
            forSecurityApplicationGroupIdentifier: appGroupID
        ) else {
            showError()
            return
        }

        let fileURL = containerURL.appendingPathComponent(sharedTextName)
        do {
            try text.write(to: fileURL, atomically: true, encoding: .utf8)
            openApp(urlString: "qrieux://shared-text")
        } catch {
            showAlert(
                title: NSLocalizedString("share_error", comment: ""),
                message: NSLocalizedString("share_error_save", comment: "")
            )
        }
    }

    // MARK: - App opening

    private func openApp(urlString: String) {
        guard let url = URL(string: urlString) else {
            extensionContext?.completeRequest(returningItems: nil)
            return
        }

        // Get UIApplication via NSClassFromString (not directly available in extensions)
        let openSelector = NSSelectorFromString("openURL:")
        guard let appClass = NSClassFromString("UIApplication") as? NSObject.Type,
              let application = appClass.perform(NSSelectorFromString("sharedApplication"))?.takeUnretainedValue() as? NSObject else {
            showAlert(
                title: NSLocalizedString("share_image_shared", comment: ""),
                message: NSLocalizedString("share_open_app", comment: "")
            )
            return
        }

        _ = application.perform(openSelector, with: url)
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) { [weak self] in
            self?.extensionContext?.completeRequest(returningItems: nil)
        }
    }

    // MARK: - Alerts

    private func showError() {
        showAlert(
            title: NSLocalizedString("share_error", comment: ""),
            message: NSLocalizedString("share_error_process", comment: "")
        )
    }

    private func showAlert(title: String, message: String) {
        let alert = UIAlertController(title: title, message: message, preferredStyle: .alert)
        alert.addAction(UIAlertAction(title: NSLocalizedString("share_ok", comment: ""), style: .default) { [weak self] _ in
            self?.extensionContext?.completeRequest(returningItems: nil)
        })
        present(alert, animated: true)
    }
}

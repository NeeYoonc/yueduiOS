import AVFoundation
import SwiftUI
import UIKit

struct QRImportView: View {
    @EnvironmentObject private var app: AppState
    @State private var scannedText: String = ""
    @State private var hasHandledScan = false

    var body: some View {
        VStack(spacing: 16) {
            QRScannerContainer { code in
                handleScannedCode(code)
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .clipShape(RoundedRectangle(cornerRadius: 16))
            .padding()

            VStack(alignment: .leading, spacing: 8) {
                Text(scannedText.isEmpty ? "Scan a Legado import QR code" : scannedText)
                    .font(.footnote.monospaced())
                    .textSelection(.enabled)
                    .lineLimit(4)
                Text("URL QR codes are fetched and imported. JSON QR codes are imported directly through Universal Import.")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.horizontal)
            .padding(.bottom)
        }
        .navigationTitle("QR Import")
        .navigationBarTitleDisplayMode(.inline)
    }

    private func handleScannedCode(_ code: String) {
        guard !hasHandledScan else {
            return
        }
        hasHandledScan = true
        scannedText = code
        if URL(string: code)?.scheme?.hasPrefix("http") == true {
            app.smartImportUrl = code
            Task {
                await app.importSmartConfigFromUrl()
            }
        } else {
            app.smartImportJson = code
            app.importSmartConfig()
        }
    }
}

private struct QRScannerContainer: UIViewControllerRepresentable {
    let onCode: (String) -> Void

    func makeUIViewController(context: Context) -> QRScannerController {
        QRScannerController(onCode: onCode)
    }

    func updateUIViewController(_ uiViewController: QRScannerController, context: Context) {
        uiViewController.onCode = onCode
    }
}

private final class QRScannerController: UIViewController, AVCaptureMetadataOutputObjectsDelegate {
    var onCode: (String) -> Void

    private let session = AVCaptureSession()
    private var previewLayer: AVCaptureVideoPreviewLayer?

    init(onCode: @escaping (String) -> Void) {
        self.onCode = onCode
        super.init(nibName: nil, bundle: nil)
    }

    required init?(coder: NSCoder) {
        return nil
    }

    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = .black
        configureSession()
    }

    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        previewLayer?.frame = view.bounds
    }

    deinit {
        session.stopRunning()
    }

    private func configureSession() {
        guard let device = AVCaptureDevice.default(for: .video),
              let input = try? AVCaptureDeviceInput(device: device),
              session.canAddInput(input) else {
            showUnavailableMessage()
            return
        }
        session.addInput(input)

        let output = AVCaptureMetadataOutput()
        guard session.canAddOutput(output) else {
            showUnavailableMessage()
            return
        }
        session.addOutput(output)
        output.setMetadataObjectsDelegate(self, queue: .main)
        output.metadataObjectTypes = [.qr]

        let preview = AVCaptureVideoPreviewLayer(session: session)
        preview.videoGravity = .resizeAspectFill
        view.layer.addSublayer(preview)
        previewLayer = preview
        session.startRunning()
    }

    private func showUnavailableMessage() {
        let label = UILabel()
        label.text = "Camera unavailable"
        label.textColor = .white
        label.textAlignment = .center
        label.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(label)
        NSLayoutConstraint.activate([
            label.centerXAnchor.constraint(equalTo: view.centerXAnchor),
            label.centerYAnchor.constraint(equalTo: view.centerYAnchor)
        ])
    }

    func metadataOutput(
        _ output: AVCaptureMetadataOutput,
        didOutput metadataObjects: [AVMetadataObject],
        from connection: AVCaptureConnection
    ) {
        guard let code = metadataObjects
            .compactMap({ $0 as? AVMetadataMachineReadableCodeObject })
            .first(where: { $0.type == .qr })?
            .stringValue else {
            return
        }
        session.stopRunning()
        onCode(code)
    }
}

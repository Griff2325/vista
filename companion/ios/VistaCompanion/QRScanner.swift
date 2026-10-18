import SwiftUI
import AVFoundation

struct QRScanner: UIViewControllerRepresentable {
    let onCode: (String) -> Void
    let onError: (String) -> Void
    let onCancel: () -> Void
    func makeUIViewController(context: Context) -> ScannerController { ScannerController(onCode: onCode, onError: onError, onCancel: onCancel) }
    func updateUIViewController(_ uiViewController: ScannerController, context: Context) {}
    static func dismantleUIViewController(_ uiViewController: ScannerController, coordinator: ()) { uiViewController.stop() }
}

final class ScannerController: UIViewController, AVCaptureMetadataOutputObjectsDelegate {
    private let capture = AVCaptureSession()
    private let queue = DispatchQueue(label: "vista.qr-camera")
    private var preview: AVCaptureVideoPreviewLayer?
    private let onCode: (String) -> Void
    private let onError: (String) -> Void
    private let onCancel: () -> Void
    private var completed = false
    private var disposed = false
    init(onCode: @escaping (String) -> Void, onError: @escaping (String) -> Void, onCancel: @escaping () -> Void) { self.onCode = onCode; self.onError = onError; self.onCancel = onCancel; super.init(nibName: nil, bundle: nil) }
    required init?(coder: NSCoder) { fatalError("init(coder:) has not been implemented") }
    override func viewDidLoad() {
        super.viewDidLoad(); view.backgroundColor = .black
        let help = UILabel(); help.text = "Point at Vista’s QR on the TV"; help.textColor = .white; help.textAlignment = .center; help.translatesAutoresizingMaskIntoConstraints = false
        let cancel = UIButton(type: .system); cancel.setTitle("Cancel", for: .normal); cancel.addTarget(self, action: #selector(cancelScan), for: .touchUpInside); cancel.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(help); view.addSubview(cancel)
        NSLayoutConstraint.activate([help.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 20), help.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -20), help.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor, constant: 24), cancel.centerXAnchor.constraint(equalTo: view.centerXAnchor), cancel.bottomAnchor.constraint(equalTo: view.safeAreaLayoutGuide.bottomAnchor, constant: -24), cancel.heightAnchor.constraint(equalToConstant: 50)])
        Task { let granted = await AVCaptureDevice.requestAccess(for: .video); guard !disposed else { return }; if granted { configure() } else { onError("Camera access is off. You can type the TV address manually, or enable Camera in iPhone Settings.") } }
    }
    private func configure() {
        guard let device = AVCaptureDevice.default(for: .video), let input = try? AVCaptureDeviceInput(device: device), capture.canAddInput(input) else { onError("Camera is unavailable. Type the TV address manually."); return }
        capture.addInput(input)
        let output = AVCaptureMetadataOutput()
        guard capture.canAddOutput(output) else { onError("QR scanning is unavailable. Type the TV address manually."); return }
        capture.addOutput(output); output.setMetadataObjectsDelegate(self, queue: .main); output.metadataObjectTypes = [.qr]
        let layer = AVCaptureVideoPreviewLayer(session: capture); layer.videoGravity = .resizeAspectFill; layer.frame = view.bounds
        view.layer.insertSublayer(layer, at: 0); preview = layer
        queue.async { [capture] in capture.startRunning() }
    }
    override func viewDidLayoutSubviews() { super.viewDidLayoutSubviews(); preview?.frame = view.bounds }
    func metadataOutput(_ output: AVCaptureMetadataOutput, didOutput metadataObjects: [AVMetadataObject], from connection: AVCaptureConnection) {
        guard !completed, let value = (metadataObjects.first as? AVMetadataMachineReadableCodeObject)?.stringValue else { return }
        completed = true; stop(); onCode(value)
    }
    @objc private func cancelScan() { completed = true; stop(); onCancel() }
    func stop() { disposed = true; queue.async { [capture] in if capture.isRunning { capture.stopRunning() } } }
}

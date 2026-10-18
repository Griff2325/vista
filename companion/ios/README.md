# Vista for iPhone

A native SwiftUI client for Vista's TV companion service; there is no embedded browser or WebView. Requires iOS 17+ and Xcode 16+ with an iOS SDK. The TV hosts the paired local HTTP transport; no laptop or cloud server is needed at runtime.

Open `VistaCompanion.xcodeproj`, select the **VistaCompanion** scheme, and run on an iPhone simulator. For your own iPhone, connect/trust it in Xcode, enable Developer Mode if iOS requests it, select your personal/development signing team under Signing & Capabilities, choose that iPhone, and Run. A personal team may require periodic re-signing. Signing identities and profiles are machine-local and excluded from this repository. This project is not an App Store/TestFlight release and has not been installed on the owner's physical iPhone.

On the TV enable **Settings → Phone companion**. Scan its QR inside the iPhone app, or enter the displayed address manually, then type the six-digit code. Allow Local Network access when iOS asks. Camera access is optional and requested only for QR scanning; manual entry works without it. The simulator uses manual entry because it has no real phone camera.

Native Remote, Find and Scene controls use the same fixed, paired protocol as the optional browser fallback. App and remote commands require a visible Vista Activity. Use the physical TV Home button to return from a streaming app or the system screensaver; Fire OS blocks ordinary background Activity launches. Spotify buttons report unavailable controls honestly. Explore still requires a separately configured TMDB catalog credential for results. See [transport, security and TV limitations](../../docs/companion.md).

The app validates private RFC1918 IPv4 endpoints on port 8765, rejects credentials/paths/query strings/nonlocal hosts, and refuses HTTP redirects. The QR contains only the address; pairing requires the TV code. Session credentials stay in memory, never UserDefaults/keychain or URLs. Only the last TV address is saved on the phone. The network session is ephemeral and has no cookies/cache; no camera images, search or listening history are stored. HTTP is not encrypted: use trusted home Wi-Fi. The TV service expires after inactivity and does not start at boot. iOS ATS permits local networking through `NSAllowsLocalNetworking`; no global arbitrary-load exception is enabled.

## Build and policy tests

From the repository root on a Mac with Xcode:

```sh
xcodebuild -project companion/ios/VistaCompanion.xcodeproj \
  -scheme VistaCompanion -sdk iphonesimulator \
  -destination 'generic/platform=iOS Simulator' \
  -derivedDataPath build/ios CODE_SIGNING_ALLOWED=NO build
xcrun swiftc -target "$(uname -m)-apple-macos14.0" \
  companion/ios/VistaCompanion/CompanionModel.swift \
  companion/ios/Tests/CompanionModelTest.swift -o build/companion-model-tests
build/companion-model-tests
```

Tests cover endpoint restrictions, Origin/auth headers, literal-plus/ampersand form encoding, session revocation and invalid QR content. They use a URLProtocol fixture, not streaming accounts or real credentials. Camera permission/scanning, physical-device LAN permission and long background/standby behavior still require hardware validation. The source icon can be regenerated with `scripts/make-ios-icon.swift`; it uses native vector shapes and no external artwork.

import SwiftUI

private enum Palette {
    static let background = Color(red: 0.063, green: 0.094, blue: 0.098)
    static let panel = Color(red: 0.106, green: 0.153, blue: 0.157)
    static let accent = Color(red: 0.82, green: 0.74, blue: 0.58)
    static let focus = Color(red: 0.24, green: 0.32, blue: 0.28)
}

struct ContentView: View {
    @EnvironmentObject private var model: CompanionModel
    @State private var scanner = false
    @State private var tab = 0
    @State private var search = ""
    @State private var season = "AUTO"
    @FocusState private var typing: Bool
    var body: some View {
        ZStack {
            Palette.background.ignoresSafeArea()
            ScrollView {
                VStack(alignment: .leading, spacing: 24) {
                    HStack { Text("VISTA").font(.title3.weight(.semibold)).tracking(2); Spacer(); Text("Companion").font(.subheadline).foregroundStyle(.secondary) }
                    if model.paired { controls } else { pairing }
                    if !model.message.isEmpty {
                        Text(model.message).font(.subheadline).frame(maxWidth: .infinity, alignment: .leading).padding(16)
                            .background(Palette.focus.opacity(0.65), in: RoundedRectangle(cornerRadius: 16)).accessibilityIdentifier("feedback")
                    }
                }.padding(24).frame(maxWidth: 520).frame(maxWidth: .infinity)
            }.scrollDismissesKeyboard(.interactively)
        }.tint(Palette.accent)
            .sheet(isPresented: $scanner) { QRScanner(onCode: { value in _ = model.scanned(value); scanner = false }, onError: { message in model.report(message); scanner = false }, onCancel: { scanner = false }) }
    }
    private var pairing: some View {
        VStack(alignment: .leading, spacing: 20) {
            Text("Your window.\nWithin reach.").font(.largeTitle.weight(.semibold)).tracking(-1)
            Text("On your TV, open Settings → Phone companion. Keep your iPhone on the same home Wi-Fi.").foregroundStyle(.secondary)
            Button { scanner = true } label: { Label("Scan the TV’s QR", systemImage: "qrcode.viewfinder").frame(maxWidth: .infinity).padding(10) }.buttonStyle(.borderedProminent).foregroundStyle(Palette.background)
            VStack(alignment: .leading, spacing: 8) {
                Text("TV address").font(.subheadline).foregroundStyle(.secondary)
                TextField("http://TV-address:8765", text: $model.address).textInputAutocapitalization(.never).autocorrectionDisabled().keyboardType(.URL).textContentType(.URL).focused($typing).textFieldStyle(.roundedBorder).accessibilityIdentifier("address")
                Text("Or type the address displayed beside its QR.").font(.caption).foregroundStyle(.secondary)
            }
            VStack(alignment: .leading, spacing: 8) {
                Text("Six-digit TV code").font(.subheadline).foregroundStyle(.secondary)
                TextField("000000", text: $model.code).keyboardType(.numberPad).textContentType(.oneTimeCode).font(.title.monospacedDigit()).focused($typing).textFieldStyle(.roundedBorder).accessibilityIdentifier("pairCode")
                    .onChange(of: model.code) { _, value in model.code = String(value.filter { $0.isASCII && $0.isNumber }.prefix(6)) }
            }
            Button { typing = false; Task { await model.pair() } } label: { HStack { if model.busy { ProgressView() }; Text(model.busy ? "Connecting…" : "Connect to Vista") }.frame(maxWidth: .infinity).padding(10) }.buttonStyle(.borderedProminent).disabled(model.busy).accessibilityIdentifier("connect")
            Text("Local to your home. No cloud account. The TV connection uses unencrypted HTTP; use trusted Wi-Fi. QR scanning needs camera access. The app saves only the last TV address; pairing credentials stay in memory.").font(.footnote).foregroundStyle(.secondary)
        }
    }
    private var controls: some View {
        VStack(alignment: .leading, spacing: 22) {
            Text(model.tvState).font(.subheadline).foregroundStyle(.secondary).accessibilityIdentifier("tvState")
            Picker("Section", selection: $tab) { Text("Remote").tag(0); Text("Find").tag(1); Text("Scene").tag(2) }.pickerStyle(.segmented)
            Group { if tab == 0 { remote } else if tab == 1 { find } else { scene } }.disabled(model.busy)
            HStack { Text("Local network only").font(.footnote).foregroundStyle(.secondary); Spacer(); Button("Unpair") { Task { await model.send("disconnect") } }.disabled(model.busy) }
            if model.busy { ProgressView().frame(maxWidth: .infinity) }
        }
    }
    private var remote: some View {
        VStack(alignment: .leading, spacing: 22) {
            heading("Settle in.")
            Grid(horizontalSpacing: 10, verticalSpacing: 10) {
                GridRow { Color.clear.frame(width: 78, height: 72); arrow("up", "arrow.up", "Up"); Color.clear.frame(width: 78, height: 72) }
                GridRow { arrow("left", "arrow.left", "Left"); command("OK", "key", "ok").frame(width: 78, height: 72); arrow("right", "arrow.right", "Right") }
                GridRow { Color.clear.frame(width: 78, height: 72); arrow("down", "arrow.down", "Down"); Color.clear.frame(width: 78, height: 72) }
            }.frame(maxWidth: .infinity)
            HStack { command("Back", "key", "back"); command("Vista Home", "home") }
            note("Controls work while Vista Home or Explore is visible. Use the physical Home button to return from streaming apps or the system screensaver. TV dialogs, volume and power use the remote.")
            Divider(); Text("Spotify on this TV").font(.headline)
            HStack { command("Previous", "spotify", "previous"); command("Play / pause", "spotify", "toggle"); command("Next", "spotify", "next") }
            note("Start music in Spotify first. Controls depend on an active session and the TV’s existing Spotify access setting.")
        }
    }
    private var find: some View {
        VStack(alignment: .leading, spacing: 18) {
            heading("What sounds good?")
            TextField("Movie or TV show", text: $search).textFieldStyle(.roundedBorder).submitLabel(.search).focused($typing).accessibilityIdentifier("search")
                .onChange(of: search) { _, value in search = String(value.prefix(120)) }
                .onSubmit { submitSearch() }
            Button("Search on TV") { submitSearch() }.buttonStyle(.borderedProminent).disabled(search.trimmingCharacters(in: .whitespaces).isEmpty)
            note("Sends a title to Explore. Results require a TMDB catalog connection in its TV settings; service sign-ins and private viewing histories stay in their apps.")
            command("Open Explore", "panel", "explore")
            Divider(); Text("App shortcuts").font(.headline)
            LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 12) {
                ForEach([("YouTube", "youtube"), ("Netflix", "netflix"), ("Hulu", "hulu"), ("Disney+", "disney"), ("Spotify", "spotify"), ("Apple TV", "apple")], id: \.1) { name, id in command(name, "app", id) }
            }
        }
    }
    private var scene: some View {
        VStack(alignment: .leading, spacing: 18) {
            heading("Outside your window.")
            Text("A passing flock. A curious fox. A distant winter rider. Quiet moments between visitors.").foregroundStyle(.secondary)
            Picker("Season", selection: $season) { Text("Automatic · Cincinnati").tag("AUTO"); Text("Preview spring").tag("SPRING"); Text("Preview summer").tag("SUMMER"); Text("Preview autumn").tag("AUTUMN"); Text("Preview winter").tag("WINTER") }.pickerStyle(.menu)
            command("Apply season", "season", season)
            note("Previews stay selected until Automatic is restored. The Cincinnati clock and weather remain live.")
            HStack { command("Wildlife on", "wildlife", "on"); command("Wildlife off", "wildlife", "off") }
            command("Open ambient scene", "panel", "ambient")
            note("Use the TV remote to return from the system screensaver. Changes made while it is open appear within a minute.")
        }
    }
    private func heading(_ title: String) -> some View { Text(title).font(.largeTitle.weight(.semibold)).tracking(-1) }
    private func note(_ text: String) -> some View { Text(text).font(.footnote).foregroundStyle(.secondary) }
    private func command(_ label: String, _ action: String, _ value: String = "") -> some View {
        Button { typing = false; Task { await model.send(action, value) } } label: { Text(label).font(.subheadline.weight(.medium)).frame(maxWidth: .infinity, minHeight: 32).padding(10).background(Palette.panel, in: RoundedRectangle(cornerRadius: 14)) }.buttonStyle(.plain).accessibilityIdentifier("\(action)-\(value)")
    }
    private func arrow(_ direction: String, _ symbol: String, _ label: String) -> some View {
        Button { Task { await model.send("key", direction) } } label: { Image(systemName: symbol).font(.title2).frame(width: 78, height: 72).background(Palette.panel, in: RoundedRectangle(cornerRadius: 16)) }.buttonStyle(.plain).accessibilityLabel(label)
    }
    private func submitSearch() { typing = false; Task { await model.send("search", search.trimmingCharacters(in: .whitespacesAndNewlines)) } }
}

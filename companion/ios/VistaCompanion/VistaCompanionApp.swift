import SwiftUI

@main
struct VistaCompanionApp: App {
    @StateObject private var model = CompanionModel()
    @Environment(\.scenePhase) private var phase
    var body: some Scene {
        WindowGroup {
            ContentView().environmentObject(model).preferredColorScheme(.dark)
                .onChange(of: phase) { _, phase in if phase == .active { Task { await model.refresh() } } }
        }
    }
}

import SwiftUI

struct ContentView: View {
    @StateObject private var appState = AppState()

    var body: some View {
        MainTabView()
            .environmentObject(appState)
    }
}

#Preview {
    ContentView()
}

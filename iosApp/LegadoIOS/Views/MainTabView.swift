import SwiftUI

struct MainTabView: View {
    var body: some View {
        TabView {
            BookshelfView()
                .tabItem {
                    Label("Bookshelf", systemImage: "books.vertical")
                }

            ExploreView()
                .tabItem {
                    Label("Explore", systemImage: "safari")
                }

            RssHomeView()
                .tabItem {
                    Label("RSS", systemImage: "dot.radiowaves.left.and.right")
                }

            SettingsHomeView()
                .tabItem {
                    Label("Settings", systemImage: "gearshape")
                }
        }
    }
}

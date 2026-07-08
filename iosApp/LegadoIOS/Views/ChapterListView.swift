import SwiftUI

struct ChapterListView: View {
    @EnvironmentObject private var app: AppState

    var body: some View {
        List {
            if app.chapters.isEmpty {
                Section {
                    EmptyStateView(title: "No chapters", systemImage: "list.bullet")
                }
            } else {
                Section {
                    ForEach(app.chapters.indices, id: \.self) { index in
                        NavigationLink {
                            ReaderView(initialChapterIndex: index)
                        } label: {
                            ChapterRow(title: app.chapters[index].title, index: index)
                        }
                    }
                }
            }
        }
        .navigationTitle("Catalog")
    }
}

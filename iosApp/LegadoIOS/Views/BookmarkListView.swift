import LegadoShared
import SwiftUI

struct BookmarkListView: View {
    @EnvironmentObject private var app: AppState

    var body: some View {
        List {
            if app.bookmarks.isEmpty {
                Section {
                    EmptyStateView(title: "No bookmarks", systemImage: "bookmark")
                }
            } else {
                Section {
                    ForEach(app.bookmarks.indices, id: \.self) { index in
                        let bookmark = app.bookmarks[index]
                        VStack(alignment: .leading, spacing: 5) {
                            Text(bookmark.chapterName)
                                .font(.headline)
                            Text(bookmark.bookName)
                                .font(.footnote)
                                .foregroundStyle(.secondary)
                            if !bookmark.bookText.isEmpty {
                                Text(bookmark.bookText)
                                    .font(.callout)
                                    .lineLimit(3)
                            }
                        }
                        .padding(.vertical, 4)
                    }
                    .onDelete { offsets in
                        offsets
                            .map { app.bookmarks[$0] }
                            .forEach(app.deleteBookmark)
                    }
                }
            }
        }
        .navigationTitle("Bookmarks")
    }
}

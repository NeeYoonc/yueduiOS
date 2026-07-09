import LegadoShared
import SwiftUI

struct SearchBookCacheView: View {
    @EnvironmentObject private var app: AppState

    var body: some View {
        List {
            if app.searchBooks.isEmpty {
                Section {
                    EmptyStateView(title: "No cached search books", systemImage: "books.vertical")
                }
            } else {
                Section {
                    ForEach(app.searchBooks.indices, id: \.self) { index in
                        let book = app.searchBooks[index]
                        SearchBookCacheRow(book: book)
                            .swipeActions(edge: .trailing, allowsFullSwipe: true) {
                                Button(role: .destructive) {
                                    app.deleteSearchBook(book)
                                } label: {
                                    Label("Delete", systemImage: "trash")
                                }
                            }
                    }
                    .onDelete { offsets in
                        offsets
                            .map { app.searchBooks[$0] }
                            .forEach(app.deleteSearchBook)
                    }
                }

                Section("Cleanup") {
                    Button {
                        app.clearExpiredSearchBooks(days: 30)
                    } label: {
                        Label("Clear older than 30 days", systemImage: "calendar.badge.minus")
                    }

                    Button(role: .destructive) {
                        app.clearSearchBooks()
                    } label: {
                        Label("Clear all cached search books", systemImage: "trash")
                    }
                }
            }
        }
        .navigationTitle("Search Books")
        .toolbar {
            Button {
                app.refreshLibrary()
            } label: {
                Image(systemName: "arrow.clockwise")
            }
        }
    }
}

private struct SearchBookCacheRow: View {
    let book: SharedSearchBook

    var body: some View {
        VStack(alignment: .leading, spacing: 5) {
            Text(book.name.isEmpty ? book.bookUrl : book.name)
                .font(.headline)
            if !book.author.isEmpty {
                Text(book.author)
                    .foregroundStyle(.secondary)
            }
            HStack(spacing: 6) {
                if !book.originName.isEmpty {
                    Text(book.originName)
                } else if !book.origin.isEmpty {
                    Text(book.origin)
                }
                if let latest = book.latestChapterTitle, !latest.isEmpty {
                    Text("•")
                    Text(latest)
                }
            }
            .font(.footnote)
            .foregroundStyle(.secondary)
            .lineLimit(2)
            if !book.bookUrl.isEmpty {
                Text(book.bookUrl)
                    .font(.caption.monospaced())
                    .foregroundStyle(.secondary)
                    .lineLimit(2)
            }
        }
        .padding(.vertical, 3)
    }
}

import LegadoShared
import SwiftUI

struct BookshelfView: View {
    @EnvironmentObject private var app: AppState

    var body: some View {
        NavigationStack {
            List {
                Section {
                    if app.books.isEmpty {
                        EmptyStateView(title: "No books", systemImage: "books.vertical")
                    } else {
                        ForEach(app.books.indices, id: \.self) { index in
                            let book = app.books[index]
                            NavigationLink {
                                BookDetailView()
                                    .task {
                                        await app.openBook(book)
                                    }
                            } label: {
                                BookRow(book: book)
                            }
                        }
                    }
                }
            }
            .navigationTitle("Legado")
            .toolbar {
                Button {
                    app.refreshLibrary()
                } label: {
                    Image(systemName: "arrow.clockwise")
                }
            }
        }
    }
}

private struct BookRow: View {
    let book: SharedBook

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(book.name)
                .font(.headline)
            Text(book.author.isEmpty ? book.originName : book.author)
                .foregroundStyle(.secondary)
            if let chapter = book.durChapterTitle, !chapter.isEmpty {
                Label(chapter, systemImage: "bookmark")
                    .font(.footnote)
                    .foregroundStyle(.secondary)
            }
        }
        .padding(.vertical, 4)
    }
}

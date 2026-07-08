import SwiftUI

struct BookDetailView: View {
    @EnvironmentObject private var app: AppState

    var body: some View {
        List {
            if app.isLoading {
                Section {
                    HStack {
                        ProgressView()
                        Text("Loading")
                            .foregroundStyle(.secondary)
                    }
                }
            }

            if let book = app.selectedBook {
                Section {
                    VStack(alignment: .leading, spacing: 8) {
                        Text(book.name)
                            .font(.title3.weight(.semibold))
                        if !book.author.isEmpty {
                            Text(book.author)
                                .foregroundStyle(.secondary)
                        }
                        if let intro = book.intro, !intro.isEmpty {
                            Text(intro)
                                .font(.callout)
                                .foregroundStyle(.secondary)
                        }
                    }
                    .padding(.vertical, 4)
                }

                Section("Catalog") {
                    if app.chapters.isEmpty {
                        EmptyStateView(title: "No chapters", systemImage: "list.bullet")
                    } else {
                        NavigationLink {
                            ChapterListView()
                        } label: {
                            LabeledContent("Chapters", value: "\(app.chapters.count)")
                        }

                        ForEach(app.chapters.prefix(5).indices, id: \.self) { index in
                            NavigationLink {
                                ReaderView(initialChapterIndex: index)
                            } label: {
                                ChapterRow(title: app.chapters[index].title, index: index)
                            }
                        }
                    }
                }
            } else {
                Section {
                    EmptyStateView(title: "No book", systemImage: "book")
                }
            }
        }
        .navigationTitle(app.selectedBook?.name ?? "Detail")
        .navigationBarTitleDisplayMode(.inline)
    }
}

struct ChapterRow: View {
    let title: String
    let index: Int

    var body: some View {
        HStack(spacing: 12) {
            Text("\(index + 1)")
                .font(.footnote.monospacedDigit())
                .foregroundStyle(.secondary)
                .frame(width: 34, alignment: .trailing)
            Text(title.isEmpty ? "Untitled" : title)
                .lineLimit(2)
        }
    }
}

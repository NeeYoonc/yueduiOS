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

                    NavigationLink {
                        BookEditView(book: book)
                    } label: {
                        Label("Edit metadata", systemImage: "pencil")
                    }

                    if let documentUrl = firstDocumentURL {
                        NavigationLink {
                            DocumentPreviewView(url: documentUrl)
                                .navigationTitle(book.name)
                                .navigationBarTitleDisplayMode(.inline)
                        } label: {
                            Label("Open local document", systemImage: "doc.viewfinder")
                        }
                    }
                }

                Section("Groups") {
                    if app.selectableBookGroups.isEmpty {
                        EmptyStateView(title: "No custom groups", systemImage: "folder")
                    } else {
                        ForEach(app.selectableBookGroups.indices, id: \.self) { index in
                            let group = app.selectableBookGroups[index]
                            Toggle(isOn: Binding(
                                get: { ((app.selectedBook?.group ?? 0) & group.groupId) > 0 },
                                set: { app.setSelectedBookGroup(group, enabled: $0) }
                            )) {
                                Text(group.groupName)
                            }
                        }
                    }
                }

                Section("Change Source") {
                    Button {
                        app.loadChangeSourceCandidates()
                    } label: {
                        Label("Load candidates", systemImage: "arrow.triangle.2.circlepath")
                    }

                    if app.changeSourceCandidates.isEmpty {
                        EmptyStateView(title: "No candidates", systemImage: "arrow.left.arrow.right")
                    } else {
                        ForEach(app.changeSourceCandidates.indices, id: \.self) { index in
                            let candidate = app.changeSourceCandidates[index]
                            Button {
                                Task {
                                    await app.changeSource(to: candidate)
                                }
                            } label: {
                                VStack(alignment: .leading, spacing: 4) {
                                    Text(candidate.originName.isEmpty ? candidate.origin : candidate.originName)
                                    Text(candidate.latestChapterTitle ?? candidate.bookUrl)
                                        .font(.footnote)
                                        .foregroundStyle(.secondary)
                                        .lineLimit(2)
                                }
                            }
                        }
                    }
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

    private var firstDocumentURL: URL? {
        guard let resource = app.chapters.first?.resourceUrl else {
            return nil
        }
        return LocalDocumentURL.make(resource)
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

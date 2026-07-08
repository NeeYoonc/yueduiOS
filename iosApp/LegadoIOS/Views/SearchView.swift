import LegadoShared
import SwiftUI

struct SearchView: View {
    @EnvironmentObject private var app: AppState

    var body: some View {
        List {
            Section {
                TextField("Keyword", text: $app.keyword)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()

                if !app.sources.isEmpty {
                    Picker("Source", selection: $app.selectedSourceIndex) {
                        Text("All enabled").tag(-1)
                        ForEach(app.sources.indices, id: \.self) { index in
                            Text(app.sources[index].bookSourceName).tag(index)
                        }
                    }
                }

                Button {
                    Task {
                        await app.search()
                    }
                } label: {
                    Label("Search", systemImage: "magnifyingglass")
                }
                .disabled(app.isLoading || app.sources.isEmpty)
            }

            if app.isLoading {
                Section {
                    HStack {
                        ProgressView()
                        Text("Loading")
                            .foregroundStyle(.secondary)
                    }
                }
            }

            if !app.searchKeywords.isEmpty {
                Section("History") {
                    ForEach(app.searchKeywords.indices, id: \.self) { index in
                        let item = app.searchKeywords[index]
                        HStack {
                            Button {
                                Task {
                                    await app.search(keyword: item)
                                }
                            } label: {
                                VStack(alignment: .leading, spacing: 3) {
                                    Text(item.word)
                                    Text("\(item.usage) use\(item.usage == 1 ? "" : "s")")
                                        .font(.caption)
                                        .foregroundStyle(.secondary)
                                }
                            }
                            .foregroundStyle(.primary)

                            Spacer()

                            Button(role: .destructive) {
                                app.deleteSearchKeyword(item)
                            } label: {
                                Image(systemName: "trash")
                            }
                        }
                        .buttonStyle(.borderless)
                    }

                    Button(role: .destructive) {
                        app.clearSearchKeywords()
                    } label: {
                        Label("Clear history", systemImage: "trash")
                    }
                }
            }

            if !app.searchResults.isEmpty {
                Section("Results") {
                    ForEach(app.searchResults.indices, id: \.self) { index in
                        let book = app.searchResults[index]
                        NavigationLink {
                            BookDetailView()
                                .task {
                                    await app.openSearchResult(book)
                                }
                        } label: {
                            SearchBookRow(book: book)
                        }
                    }
                }
            }

            if !app.searchErrors.isEmpty {
                Section("Errors") {
                    ForEach(app.searchErrors.indices, id: \.self) { index in
                        let error = app.searchErrors[index]
                        VStack(alignment: .leading, spacing: 4) {
                            Text(error.source.bookSourceName)
                            Text(error.message)
                                .font(.footnote)
                                .foregroundStyle(.secondary)
                        }
                    }
                }
            }
        }
    }
}

private struct SearchBookRow: View {
    let book: SharedSearchBook

    var body: some View {
        VStack(alignment: .leading, spacing: 5) {
            Text(book.name.isEmpty ? book.bookUrl : book.name)
                .font(.headline)
            if !book.author.isEmpty {
                Text(book.author)
                    .foregroundStyle(.secondary)
            }
            if let latest = book.latestChapterTitle, !latest.isEmpty {
                Text(latest)
                    .font(.footnote)
                    .foregroundStyle(.secondary)
            }
        }
        .padding(.vertical, 3)
    }
}

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

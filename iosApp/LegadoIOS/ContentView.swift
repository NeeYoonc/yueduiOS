import SwiftUI

struct ContentView: View {
    @StateObject private var viewModel = LegadoViewModel()

    var body: some View {
        TabView {
            bookshelf
                .tabItem {
                    Label("Bookshelf", systemImage: "books.vertical")
                }

            search
                .tabItem {
                    Label("Search", systemImage: "magnifyingglass")
                }

            sources
                .tabItem {
                    Label("Sources", systemImage: "tray.full")
                }

            settings
                .tabItem {
                    Label("Settings", systemImage: "gearshape")
                }
        }
    }

    private var bookshelf: some View {
        NavigationStack {
            List {
                if viewModel.books.isEmpty {
                    Section {
                        emptyState(title: "No books", systemImage: "books.vertical")
                    }
                } else {
                    Section {
                        ForEach(viewModel.books.indices, id: \.self) { index in
                            let book = viewModel.books[index]
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
                }
            }
            .navigationTitle("Legado")
            .toolbar {
                Button {
                    viewModel.refreshLibrary()
                } label: {
                    Image(systemName: "arrow.clockwise")
                }
            }
        }
    }

    private var search: some View {
        NavigationStack {
            List {
                Section {
                    TextField("Keyword", text: $viewModel.keyword)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()

                    if !viewModel.sources.isEmpty {
                        Picker("Source", selection: $viewModel.selectedSourceIndex) {
                            Text("All enabled").tag(-1)
                            ForEach(viewModel.sources.indices, id: \.self) { index in
                                Text(viewModel.sources[index].bookSourceName).tag(index)
                            }
                        }
                    }

                    HStack {
                        Button {
                            Task {
                                await viewModel.search()
                            }
                        } label: {
                            Label("Search", systemImage: "magnifyingglass")
                        }
                        .disabled(viewModel.isLoading || viewModel.sources.isEmpty)

                        Button {
                            Task {
                                await viewModel.openFirstResult()
                            }
                        } label: {
                            Label("Open", systemImage: "book")
                        }
                        .disabled(viewModel.isLoading || viewModel.sources.isEmpty)
                    }
                }

                if viewModel.isLoading {
                    Section {
                        HStack {
                            ProgressView()
                            Text("Loading")
                                .foregroundStyle(.secondary)
                        }
                    }
                }

                if !viewModel.searchResults.isEmpty {
                    Section("Results") {
                        ForEach(viewModel.searchResults.indices, id: \.self) { index in
                            let book = viewModel.searchResults[index]
                            VStack(alignment: .leading, spacing: 5) {
                                Text(book.name)
                                    .font(.headline)
                                Text(book.author)
                                    .foregroundStyle(.secondary)
                                if let latest = book.latestChapterTitle, !latest.isEmpty {
                                    Text(latest)
                                        .font(.footnote)
                                        .foregroundStyle(.secondary)
                                }
                            }
                            .padding(.vertical, 3)
                        }
                    }
                }

                if !viewModel.chapterContent.isEmpty {
                    Section(viewModel.chapterTitle ?? "Reader") {
                        Text(viewModel.chapterContent)
                            .textSelection(.enabled)
                    }
                }
            }
            .navigationTitle("Search")
        }
    }

    private var sources: some View {
        NavigationStack {
            List {
                Section {
                    Button {
                        viewModel.importBundledDefaultData()
                    } label: {
                        Label("Import bundled data", systemImage: "square.and.arrow.down")
                    }

                    TextEditor(text: $viewModel.sourceJson)
                        .font(.system(.footnote, design: .monospaced))
                        .frame(minHeight: 120)

                    Button {
                        viewModel.importSources()
                    } label: {
                        Label("Import JSON", systemImage: "tray.and.arrow.down")
                    }
                }

                Section("Active") {
                    if viewModel.sources.isEmpty {
                        emptyState(title: "No sources", systemImage: "tray")
                    } else {
                        ForEach(viewModel.sources.indices, id: \.self) { index in
                            let source = viewModel.sources[index]
                            VStack(alignment: .leading, spacing: 4) {
                                Text(source.bookSourceName)
                                Text(source.bookSourceUrl)
                                    .font(.footnote)
                                    .foregroundStyle(.secondary)
                            }
                        }
                    }
                }
            }
            .navigationTitle("Sources")
        }
    }

    private var settings: some View {
        NavigationStack {
            List {
                Section("Library") {
                    LabeledContent("Sources", value: "\(viewModel.sources.count)")
                    LabeledContent("Books", value: "\(viewModel.books.count)")
                    LabeledContent("Results", value: "\(viewModel.searchResults.count)")
                }

                if let message = viewModel.message {
                    Section("Status") {
                        Text(message)
                    }
                }
            }
            .navigationTitle("Settings")
        }
    }

    private func emptyState(title: String, systemImage: String) -> some View {
        HStack(spacing: 12) {
            Image(systemName: systemImage)
                .foregroundStyle(.secondary)
            Text(title)
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity, alignment: .center)
        .padding(.vertical, 24)
    }
}

#Preview {
    ContentView()
}

import SwiftUI

struct ContentView: View {
    @StateObject private var viewModel = LegadoViewModel()

    var body: some View {
        NavigationStack {
            List {
                Section("Source") {
                    TextEditor(text: $viewModel.sourceJson)
                        .font(.system(.footnote, design: .monospaced))
                        .frame(minHeight: 120)

                    Button {
                        viewModel.importSources()
                    } label: {
                        Label("Import", systemImage: "tray.and.arrow.down")
                    }

                    if !viewModel.sources.isEmpty {
                        Picker("Active", selection: $viewModel.selectedSourceIndex) {
                            ForEach(viewModel.sources.indices, id: \.self) { index in
                                Text(viewModel.sources[index].bookSourceName).tag(index)
                            }
                        }
                    }
                }

                Section("Search") {
                    TextField("Keyword", text: $viewModel.keyword)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()

                    Button {
                        Task {
                            await viewModel.openFirstResult()
                        }
                    } label: {
                        if viewModel.isLoading {
                            ProgressView()
                        } else {
                            Label("Open First Result", systemImage: "magnifyingglass")
                        }
                    }
                    .disabled(viewModel.isLoading || viewModel.sources.isEmpty)
                }

                if let message = viewModel.message {
                    Section("Status") {
                        Text(message)
                    }
                }

                if let title = viewModel.bookTitle {
                    Section("Book") {
                        Text(title)
                            .font(.headline)
                        if let chapterTitle = viewModel.chapterTitle {
                            Text(chapterTitle)
                        }
                    }
                }

                if !viewModel.chapterContent.isEmpty {
                    Section("Content") {
                        Text(viewModel.chapterContent)
                            .font(.body)
                            .textSelection(.enabled)
                    }
                }
            }
            .navigationTitle("Legado")
        }
    }
}

#Preview {
    ContentView()
}

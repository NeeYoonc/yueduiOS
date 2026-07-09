import SwiftUI

struct ReaderView: View {
    @EnvironmentObject private var app: AppState
    let initialChapterIndex: Int
    @State private var hasLoaded = false
    @State private var showSearch = false
    @State private var readerSearchQuery = ""
    @State private var readSessionStart: Date?
    @StateObject private var speech = SpeechController()

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 18) {
                if app.isLoading && app.currentContent.isEmpty {
                    HStack {
                        ProgressView()
                        Text("Loading")
                            .foregroundStyle(.secondary)
                    }
                    .frame(maxWidth: .infinity, alignment: .center)
                    .padding(.top, 80)
                } else {
                    Text(app.currentChapter?.title ?? app.chapters[safe: initialChapterIndex]?.title ?? "Reader")
                        .font(.title2.weight(.semibold))
                    Text(app.currentContent)
                        .font(.system(.body, design: .serif))
                        .lineSpacing(8)
                        .textSelection(.enabled)

                    if showSearch {
                        VStack(alignment: .leading, spacing: 10) {
                            HStack {
                                TextField("Search in chapter", text: $readerSearchQuery)
                                    .textInputAutocapitalization(.never)
                                    .autocorrectionDisabled()
                                    .textFieldStyle(.roundedBorder)
                                    .onSubmit {
                                        app.searchCurrentContent(query: readerSearchQuery)
                                    }

                                Button("Search") {
                                    app.searchCurrentContent(query: readerSearchQuery)
                                }
                                .disabled(app.currentContent.isEmpty)
                            }

                            if app.readerSearchResults.isEmpty {
                                Text("No matches")
                                    .font(.footnote)
                                    .foregroundStyle(.secondary)
                            } else {
                                ForEach(app.readerSearchResults.indices, id: \.self) { index in
                                    let result = app.readerSearchResults[index]
                                    VStack(alignment: .leading, spacing: 4) {
                                        Text("#\(index + 1) \(result.startIndex)-\(result.endIndex)")
                                            .font(.caption.monospacedDigit())
                                            .foregroundStyle(.secondary)
                                        Text(result.snippet)
                                            .font(.callout)
                                            .textSelection(.enabled)
                                    }
                                    .padding(.vertical, 4)
                                }
                            }
                        }
                        .padding()
                        .background(.thinMaterial, in: RoundedRectangle(cornerRadius: 12))
                    }
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(20)
        }
        .navigationTitle(app.currentChapter?.title ?? "Reader")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                HStack {
                    Button {
                        showSearch.toggle()
                        if showSearch {
                            app.searchCurrentContent(query: readerSearchQuery)
                        }
                    } label: {
                        Image(systemName: "magnifyingglass")
                    }
                    .disabled(app.currentContent.isEmpty)

                    Button {
                        app.addCurrentBookmark()
                    } label: {
                        Image(systemName: "bookmark")
                    }
                    .disabled(app.currentContent.isEmpty)

                    Button {
                        if speech.isSpeaking {
                            speech.stop()
                        } else {
                            speech.speak(app.currentContent)
                        }
                    } label: {
                        Image(systemName: speech.isSpeaking ? "stop.fill" : "speaker.wave.2")
                    }
                    .disabled(app.currentContent.isEmpty)
                }
            }

            ToolbarItemGroup(placement: .bottomBar) {
                Button {
                    Task {
                        await loadChapter(app.currentChapterIndex - 1)
                    }
                } label: {
                    Label("Previous", systemImage: "chevron.left")
                }
                .disabled(app.currentChapterIndex <= 0 || app.isLoading)

                Spacer()

                Text("\(app.currentChapterIndex + 1)/\(max(app.chapters.count, 1))")
                    .font(.footnote.monospacedDigit())
                    .foregroundStyle(.secondary)

                Spacer()

                Button {
                    Task {
                        await loadChapter(app.currentChapterIndex + 1)
                    }
                } label: {
                    Label("Next", systemImage: "chevron.right")
                }
                .disabled(app.currentChapterIndex >= app.chapters.count - 1 || app.isLoading)
            }
        }
        .task {
            guard !hasLoaded else {
                return
            }
            hasLoaded = true
            readSessionStart = Date()
            await loadChapter(initialChapterIndex)
        }
        .onDisappear {
            recordReadSession()
            speech.stop()
        }
    }

    private func loadChapter(_ index: Int) async {
        guard app.chapters.indices.contains(index) else {
            return
        }
        await app.loadChapter(at: index)
    }

    private func recordReadSession() {
        guard let start = readSessionStart, !app.currentContent.isEmpty else {
            return
        }
        let duration = Int64(Date().timeIntervalSince(start) * 1000)
        app.recordCurrentReadingTime(durationMillis: duration)
        readSessionStart = nil
    }
}

private extension Array {
    subscript(safe index: Int) -> Element? {
        indices.contains(index) ? self[index] : nil
    }
}

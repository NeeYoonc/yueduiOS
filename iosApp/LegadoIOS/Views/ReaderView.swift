import SwiftUI

struct ReaderView: View {
    @EnvironmentObject private var app: AppState
    let initialChapterIndex: Int
    @State private var hasLoaded = false
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
            await loadChapter(initialChapterIndex)
        }
        .onDisappear {
            speech.stop()
        }
    }

    private func loadChapter(_ index: Int) async {
        guard app.chapters.indices.contains(index) else {
            return
        }
        await app.loadChapter(at: index)
    }
}

private extension Array {
    subscript(safe index: Int) -> Element? {
        indices.contains(index) ? self[index] : nil
    }
}

import LegadoShared
import SwiftUI

struct HttpTtsListView: View {
    @EnvironmentObject private var app: AppState

    var body: some View {
        List {
            if app.httpTts.isEmpty {
                Section {
                    EmptyStateView(title: "No HTTP TTS engines", systemImage: "speaker.wave.2")
                }
            } else {
                Section {
                    ForEach(app.httpTts.indices, id: \.self) { index in
                        let engine = app.httpTts[index]
                        VStack(alignment: .leading, spacing: 5) {
                            Text(engine.name)
                                .font(.headline)
                            Text(engine.url)
                                .font(.footnote)
                                .foregroundStyle(.secondary)
                                .lineLimit(2)
                            if let contentType = engine.contentType, !contentType.isEmpty {
                                Text(contentType)
                                    .font(.caption)
                                    .foregroundStyle(.secondary)
                            }
                        }
                        .padding(.vertical, 4)
                    }
                    .onDelete { offsets in
                        offsets
                            .map { app.httpTts[$0] }
                            .forEach(app.deleteHttpTts)
                    }
                }
            }
        }
        .navigationTitle("HTTP TTS")
        .toolbar {
            NavigationLink {
                HttpTtsEditorView()
            } label: {
                Image(systemName: "doc.text")
            }
        }
    }
}

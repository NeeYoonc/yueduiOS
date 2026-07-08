import LegadoShared
import SwiftUI

struct RssSourceListView: View {
    @EnvironmentObject private var app: AppState

    var body: some View {
        List {
            if app.rssSources.isEmpty {
                Section {
                    EmptyStateView(title: "No RSS sources", systemImage: "dot.radiowaves.left.and.right")
                }
            } else {
                Section {
                    ForEach(app.rssSources.indices, id: \.self) { index in
                        let source = app.rssSources[index]
                        RssSourceRow(source: source)
                            .swipeActions(edge: .trailing, allowsFullSwipe: false) {
                                Button(role: .destructive) {
                                    app.deleteRssSource(source)
                                } label: {
                                    Label("Delete", systemImage: "trash")
                                }
                            }
                    }
                } footer: {
                    Text("RSS sources keep Legado's source URL, rule fields, JS flags, grouping, and ordering so imported Android RSS source JSON can be managed on iOS.")
                }
            }
        }
        .navigationTitle("RSS Sources")
        .toolbar {
            NavigationLink {
                RssSourceEditorView()
            } label: {
                Image(systemName: "doc.text")
            }
        }
    }
}

private struct RssSourceRow: View {
    @EnvironmentObject private var app: AppState
    let source: SharedRssSource

    var body: some View {
        Toggle(isOn: Binding(
            get: { source.enabled },
            set: { app.setRssSourceEnabled(source, enabled: $0) }
        )) {
            VStack(alignment: .leading, spacing: 6) {
                HStack(spacing: 8) {
                    Text(source.sourceName.isEmpty ? source.sourceUrl : source.sourceName)
                        .font(.headline)

                    if let group = source.sourceGroup, !group.isEmpty {
                        Text(group)
                            .font(.caption)
                            .padding(.horizontal, 6)
                            .padding(.vertical, 2)
                            .background(.secondary.opacity(0.12), in: Capsule())
                    }
                }

                Text(source.sourceUrl)
                    .font(.footnote)
                    .foregroundStyle(.secondary)
                    .lineLimit(2)
                    .textSelection(.enabled)

                if let comment = source.sourceComment, !comment.isEmpty {
                    Text(comment)
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                        .lineLimit(3)
                }

                HStack(spacing: 12) {
                    Text("order \(source.customOrder)")
                    if source.type != 0 {
                        Text("type \(source.type)")
                    }
                    if source.articleStyle != 0 {
                        Text("style \(source.articleStyle)")
                    }
                    if source.enableJs {
                        Text("JS")
                    }
                    if source.cacheFirst {
                        Text("cache first")
                    }
                }
                .font(.caption.monospacedDigit())
                .foregroundStyle(.secondary)
            }
            .padding(.vertical, 4)
        }
    }
}

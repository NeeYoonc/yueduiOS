import LegadoShared
import SwiftUI

struct SourceListView: View {
    @EnvironmentObject private var app: AppState

    var body: some View {
        List {
            if app.sources.isEmpty {
                Section {
                    EmptyStateView(title: "No sources", systemImage: "tray")
                }
            } else {
                Section {
                    ForEach(app.sources.indices, id: \.self) { index in
                        let source = app.sources[index]
                        NavigationLink {
                            BookSourceFormView(source: source)
                        } label: {
                            SourceRow(source: source)
                        }
                        .swipeActions(edge: .trailing, allowsFullSwipe: false) {
                            Button(role: .destructive) {
                                app.deleteSource(source)
                            } label: {
                                Label("Delete", systemImage: "trash")
                            }
                        }
                        .swipeActions(edge: .leading, allowsFullSwipe: true) {
                            Button {
                                app.setSourceEnabled(source, enabled: !source.enabled)
                            } label: {
                                Label(source.enabled ? "Disable" : "Enable", systemImage: source.enabled ? "pause.circle" : "play.circle")
                            }
                            .tint(source.enabled ? .orange : .green)
                        }
                    }
                    .onMove { offsets, destination in
                        app.moveSource(from: offsets, to: destination)
                    }
                }
            }
        }
        .navigationTitle("Sources")
        .toolbar {
            ToolbarItem(placement: .navigationBarLeading) {
                EditButton()
            }

            ToolbarItemGroup(placement: .navigationBarTrailing) {
                NavigationLink {
                    SourceEditorView()
                } label: {
                    Image(systemName: "doc.text")
                }

                NavigationLink {
                    BookSourceFormView()
                } label: {
                    Image(systemName: "plus")
                }

                Button {
                    app.refreshLibrary()
                } label: {
                    Image(systemName: "arrow.clockwise")
                }
            }
        }
    }
}

private struct SourceRow: View {
    let source: SharedBookSource

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: source.enabled ? "checkmark.circle.fill" : "pause.circle")
                .foregroundStyle(source.enabled ? .green : .secondary)
            VStack(alignment: .leading, spacing: 4) {
                Text(source.bookSourceName.isEmpty ? source.bookSourceUrl : source.bookSourceName)
                Text(source.bookSourceUrl)
                    .font(.footnote)
                    .foregroundStyle(.secondary)
                    .lineLimit(2)
            }
        }
        .padding(.vertical, 3)
    }
}

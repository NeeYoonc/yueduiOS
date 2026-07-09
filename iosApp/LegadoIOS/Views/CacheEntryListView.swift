import LegadoShared
import SwiftUI

struct CacheEntryListView: View {
    @EnvironmentObject private var app: AppState

    var body: some View {
        List {
            Section("Import from URL") {
                TextField("https://example.com/cacheEntries.json", text: $app.cacheImportUrl)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                    .keyboardType(.URL)

                Button {
                    Task {
                        await app.importCacheEntriesFromUrl()
                    }
                } label: {
                    Label("Fetch and import", systemImage: "link.badge.plus")
                }
                .disabled(app.isLoading || app.cacheImportUrl.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)

                Button(role: .destructive) {
                    Task {
                        await app.importCacheEntriesFromUrl(replace: true)
                    }
                } label: {
                    Label("Fetch and replace all", systemImage: "arrow.triangle.2.circlepath")
                }
                .disabled(app.isLoading || app.cacheImportUrl.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
            }

            Section {
                TextEditor(text: $app.cacheJson)
                    .font(.system(.body, design: .monospaced))
                    .frame(minHeight: 180)
            } header: {
                Text("Cache JSON")
            } footer: {
                Text("Import Android cache entries such as [{\"key\":\"chapter\",\"value\":\"...\",\"deadline\":0}].")
            }

            Section {
                Button {
                    app.importCacheEntries()
                } label: {
                    Label("Import", systemImage: "square.and.arrow.down")
                }

                Button {
                    app.importCacheEntries(replace: true)
                } label: {
                    Label("Replace all", systemImage: "arrow.triangle.2.circlepath")
                }

                Button {
                    app.exportCacheEntriesToEditor()
                } label: {
                    Label("Export", systemImage: "square.and.arrow.up")
                }

                Button {
                    app.clearExpiredCacheEntries()
                } label: {
                    Label("Clear expired", systemImage: "timer")
                }
                .disabled(app.cacheEntries.isEmpty)

                Button(role: .destructive) {
                    app.clearCacheEntries()
                } label: {
                    Label("Clear all cache", systemImage: "trash")
                }
                .disabled(app.cacheEntries.isEmpty)
            }

            if app.cacheEntries.isEmpty {
                Section {
                    EmptyStateView(title: "No cache entries", systemImage: "archivebox")
                }
            } else {
                Section {
                    ForEach(app.cacheEntries.indices, id: \.self) { index in
                        let entry = app.cacheEntries[index]
                        CacheEntryRow(entry: entry)
                    }
                    .onDelete { offsets in
                        offsets
                            .map { app.cacheEntries[$0] }
                            .forEach(app.deleteCacheEntry)
                    }
                } footer: {
                    Text("Cache entries preserve Android's shared key/value cache snapshot, including expiration deadlines.")
                }
            }
        }
        .navigationTitle("Cache")
    }
}

private struct CacheEntryRow: View {
    let entry: SharedCacheEntry

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(entry.key)
                .font(.headline.monospaced())
                .textSelection(.enabled)

            Text(entry.value ?? "")
                .font(.footnote.monospaced())
                .foregroundStyle(.secondary)
                .lineLimit(3)
                .textSelection(.enabled)

            Text(deadlineText)
                .font(.caption.monospacedDigit())
                .foregroundStyle(.secondary)
        }
        .padding(.vertical, 4)
    }

    private var deadlineText: String {
        entry.deadline == 0 ? "never expires" : "deadline \(entry.deadline)"
    }
}

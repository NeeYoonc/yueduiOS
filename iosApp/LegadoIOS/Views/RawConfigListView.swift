import LegadoShared
import SwiftUI

struct RawConfigListView: View {
    @EnvironmentObject private var app: AppState

    var body: some View {
        List {
            if app.rawConfigs.isEmpty {
                Section {
                    EmptyStateView(title: "No raw configs", systemImage: "slider.horizontal.3")
                }
            } else {
                Section {
                    ForEach(app.rawConfigs.indices, id: \.self) { index in
                        let entry = app.rawConfigs[index]
                        RawConfigRow(entry: entry)
                    }
                    .onDelete { offsets in
                        offsets
                            .map { app.rawConfigs[$0] }
                            .forEach(app.deleteRawConfig)
                    }
                } footer: {
                    Text("Raw configs preserve Android readConfig, themeConfig, coverRule, directLinkUpload, and compatible future config JSON.")
                }
            }
        }
        .navigationTitle("Raw Configs")
        .toolbar {
            NavigationLink {
                RawConfigEditorView()
            } label: {
                Image(systemName: "doc.text")
            }
        }
    }
}

private struct RawConfigRow: View {
    let entry: SharedRawConfigEntry

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(entry.key)
                .font(.headline.monospaced())
            Text(entry.value)
                .font(.footnote.monospaced())
                .foregroundStyle(.secondary)
                .lineLimit(4)
                .textSelection(.enabled)
        }
        .padding(.vertical, 4)
    }
}

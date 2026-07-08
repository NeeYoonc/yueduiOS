import SwiftUI

struct SettingsHomeView: View {
    @EnvironmentObject private var app: AppState

    var body: some View {
        NavigationStack {
            List {
                Section("Library") {
                    LabeledContent("Sources", value: "\(app.sources.count)")
                    LabeledContent("Books", value: "\(app.books.count)")
                    LabeledContent("RSS", value: "\(app.rssSources.count)")
                    LabeledContent("Replacements", value: "\(app.replaceRules.count)")
                    LabeledContent("Results", value: "\(app.searchResults.count)")
                }

                Section("Sources") {
                    NavigationLink {
                        SourceListView()
                    } label: {
                        Label("Manage sources", systemImage: "tray.full")
                    }

                    NavigationLink {
                        SourceEditorView()
                    } label: {
                        Label("Import and export", systemImage: "doc.text")
                    }
                }

                Section("Reading") {
                    NavigationLink {
                        ReplaceRuleListView()
                    } label: {
                        Label("Replacement rules", systemImage: "wand.and.stars")
                    }
                }

                Section("Data") {
                    NavigationLink {
                        BackupView()
                    } label: {
                        Label("Backup and restore", systemImage: "externaldrive")
                    }
                }

                Section("Active Sources") {
                    if app.sources.isEmpty {
                        EmptyStateView(title: "No sources", systemImage: "tray")
                    } else {
                        ForEach(app.sources.indices, id: \.self) { index in
                            let source = app.sources[index]
                            VStack(alignment: .leading, spacing: 4) {
                                Text(source.bookSourceName)
                                Text(source.bookSourceUrl)
                                    .font(.footnote)
                                    .foregroundStyle(.secondary)
                            }
                            .padding(.vertical, 3)
                        }
                    }
                }

                if let message = app.message {
                    Section("Status") {
                        Text(message)
                        Button {
                            app.clearMessage()
                        } label: {
                            Label("Clear", systemImage: "xmark.circle")
                        }
                    }
                }
            }
            .navigationTitle("Settings")
        }
    }
}

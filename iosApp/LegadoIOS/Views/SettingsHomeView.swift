import SwiftUI

struct SettingsHomeView: View {
    @EnvironmentObject private var app: AppState

    var body: some View {
        NavigationStack {
            List {
                Section("Library") {
                    LabeledContent("Sources", value: "\(app.sources.count)")
                    LabeledContent("Books", value: "\(app.books.count)")
                    LabeledContent("Book Groups", value: "\(app.bookGroups.count)")
                    LabeledContent("Bookmarks", value: "\(app.bookmarks.count)")
                    LabeledContent("RSS", value: "\(app.rssSources.count)")
                    LabeledContent("Replacements", value: "\(app.replaceRules.count)")
                    LabeledContent("Dictionaries", value: "\(app.dictRules.count)")
                    LabeledContent("HTTP TTS", value: "\(app.httpTts.count)")
                    LabeledContent("Search History", value: "\(app.searchKeywords.count)")
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
                        BookGroupListView()
                    } label: {
                        Label("Book groups", systemImage: "folder")
                    }

                    NavigationLink {
                        ReplaceRuleListView()
                    } label: {
                        Label("Replacement rules", systemImage: "wand.and.stars")
                    }

                    NavigationLink {
                        BookmarkListView()
                    } label: {
                        Label("Bookmarks", systemImage: "bookmark")
                    }
                }

                Section("Tools") {
                    NavigationLink {
                        DictRuleListView()
                    } label: {
                        Label("Dictionaries", systemImage: "character.book.closed")
                    }

                    NavigationLink {
                        HttpTtsListView()
                    } label: {
                        Label("HTTP TTS", systemImage: "speaker.wave.2")
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

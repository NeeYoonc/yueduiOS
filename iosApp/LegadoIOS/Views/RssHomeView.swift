import SwiftUI

struct RssHomeView: View {
    @EnvironmentObject private var app: AppState

    var body: some View {
        NavigationStack {
            List {
                Section {
                    if app.rssSources.isEmpty {
                        EmptyStateView(title: "No RSS sources", systemImage: "dot.radiowaves.left.and.right")
                    } else {
                        ForEach(app.rssSources.indices, id: \.self) { index in
                            let source = app.rssSources[index]
                            NavigationLink {
                                RssArticleListView(source: source)
                                    .task {
                                        await app.refreshRss(source)
                                    }
                            } label: {
                                VStack(alignment: .leading, spacing: 4) {
                                    Text(source.sourceName.isEmpty ? source.sourceUrl : source.sourceName)
                                    if !source.sourceUrl.isEmpty {
                                        Text(source.sourceUrl)
                                            .font(.footnote)
                                            .foregroundStyle(.secondary)
                                    }
                                }
                                .padding(.vertical, 3)
                            }
                        }
                    }
                }
            }
            .navigationTitle("RSS")
            .toolbar {
                NavigationLink {
                    RssSourceListView()
                } label: {
                    Image(systemName: "tray.full")
                }

                NavigationLink {
                    RssStarListView()
                } label: {
                    Image(systemName: "star")
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

import LegadoShared
import SwiftUI

struct ExploreView: View {
    @EnvironmentObject private var app: AppState

    var body: some View {
        NavigationStack {
            List {
                Section("Sources") {
                    if app.exploreSources.isEmpty {
                        EmptyStateView(title: "No explore sources", systemImage: "safari")
                    } else {
                        ForEach(app.exploreSources.indices, id: \.self) { index in
                            let source = app.exploreSources[index]
                            Button {
                                app.openExploreSource(source)
                            } label: {
                                HStack {
                                    VStack(alignment: .leading, spacing: 4) {
                                        Text(source.bookSourceName.isEmpty ? source.bookSourceUrl : source.bookSourceName)
                                        if let group = source.bookSourceGroup, !group.isEmpty {
                                            Text(group)
                                                .font(.footnote)
                                                .foregroundStyle(.secondary)
                                        }
                                    }
                                    Spacer()
                                    if app.selectedExploreSource?.bookSourceUrl == source.bookSourceUrl {
                                        Image(systemName: "checkmark")
                                            .foregroundStyle(.tint)
                                    }
                                }
                            }
                            .foregroundStyle(.primary)
                        }
                    }
                }

                if !app.exploreKinds.isEmpty {
                    Section("Categories") {
                        ForEach(app.exploreKinds.indices, id: \.self) { index in
                            let kind = app.exploreKinds[index]
                            Button {
                                Task {
                                    await app.openExploreKind(kind)
                                }
                            } label: {
                                Label(kind.title, systemImage: kind.type == "error" ? "exclamationmark.triangle" : "tag")
                            }
                            .disabled(kind.url?.isEmpty ?? true)
                        }
                    }
                }

                if app.isLoading {
                    Section {
                        HStack {
                            ProgressView()
                            Text("Loading")
                                .foregroundStyle(.secondary)
                        }
                    }
                }

                if !app.exploreResults.isEmpty {
                    Section("Results") {
                        ForEach(app.exploreResults.indices, id: \.self) { index in
                            let book = app.exploreResults[index]
                            NavigationLink {
                                BookDetailView()
                                    .task {
                                        await app.openSearchResult(book)
                                    }
                            } label: {
                                ExploreBookRow(book: book)
                            }
                        }
                    }
                }
            }
                .navigationTitle("Explore")
                .toolbar {
                    NavigationLink {
                        SearchView()
                    } label: {
                        Image(systemName: "magnifyingglass")
                    }
                }
        }
    }
}

private struct ExploreBookRow: View {
    let book: SharedSearchBook

    var body: some View {
        VStack(alignment: .leading, spacing: 5) {
            Text(book.name.isEmpty ? book.bookUrl : book.name)
                .font(.headline)
            if !book.author.isEmpty {
                Text(book.author)
                    .foregroundStyle(.secondary)
            }
            if let latest = book.latestChapterTitle, !latest.isEmpty {
                Text(latest)
                    .font(.footnote)
                    .foregroundStyle(.secondary)
            }
        }
        .padding(.vertical, 3)
    }
}

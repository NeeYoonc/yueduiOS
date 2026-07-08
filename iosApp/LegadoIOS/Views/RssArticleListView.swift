import LegadoShared
import SwiftUI

struct RssArticleListView: View {
    @EnvironmentObject private var app: AppState
    let source: SharedRssSource

    var body: some View {
        List {
            if app.isLoading && app.rssArticles.isEmpty {
                Section {
                    HStack {
                        ProgressView()
                        Text("Loading")
                            .foregroundStyle(.secondary)
                    }
                }
            }

            if app.rssArticles.isEmpty {
                Section {
                    EmptyStateView(title: "No articles", systemImage: "newspaper")
                }
            } else {
                Section {
                    ForEach(app.rssArticles.indices, id: \.self) { index in
                        let article = app.rssArticles[index]
                        NavigationLink {
                            RssReaderView(article: article)
                                .task {
                                    await app.openRssArticle(article)
                                }
                        } label: {
                            RssArticleRow(article: article)
                        }
                    }
                }
            }
        }
        .navigationTitle(source.sourceName.isEmpty ? "RSS" : source.sourceName)
        .toolbar {
            Button {
                Task {
                    await app.refreshRss(source)
                }
            } label: {
                Image(systemName: "arrow.clockwise")
            }
        }
    }
}

private struct RssArticleRow: View {
    let article: SharedRssArticle

    var body: some View {
        VStack(alignment: .leading, spacing: 5) {
            Text(article.title.isEmpty ? article.link : article.title)
                .font(.headline)
            if let date = article.pubDate, !date.isEmpty {
                Text(date)
                    .font(.footnote)
                    .foregroundStyle(.secondary)
            }
            if !article.summary.isEmpty {
                Text(article.summary)
                    .font(.footnote)
                    .foregroundStyle(.secondary)
                    .lineLimit(3)
            }
        }
        .padding(.vertical, 3)
    }
}

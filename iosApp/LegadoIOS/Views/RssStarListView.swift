import LegadoShared
import SwiftUI

struct RssStarListView: View {
    @EnvironmentObject private var app: AppState

    var body: some View {
        List {
            if app.rssStarredArticles.isEmpty {
                Section {
                    EmptyStateView(title: "No starred RSS articles", systemImage: "star")
                }
            } else {
                Section {
                    ForEach(app.rssStarredArticles.indices, id: \.self) { index in
                        let article = app.rssStarredArticles[index]
                        NavigationLink {
                            RssReaderView(article: article)
                                .task {
                                    await app.openRssArticle(article)
                                }
                        } label: {
                            RssStarRow(article: article)
                        }
                        .swipeActions(edge: .trailing, allowsFullSwipe: true) {
                            Button(role: .destructive) {
                                app.setRssArticleStarred(article, starred: false)
                            } label: {
                                Label("Unstar", systemImage: "star.slash")
                            }
                        }
                    }
                } footer: {
                    Text("Starred RSS articles are stored in the shared library snapshot and can be opened from their original source when available.")
                }
            }
        }
        .navigationTitle("RSS Stars")
    }
}

private struct RssStarRow: View {
    let article: SharedRssArticle

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack(spacing: 8) {
                Image(systemName: "star.fill")
                    .foregroundStyle(.yellow)
                Text(article.title.isEmpty ? article.link : article.title)
                    .font(.headline)
                    .foregroundStyle(article.read ? .secondary : .primary)
            }

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

            Text(article.origin)
                .font(.caption.monospaced())
                .foregroundStyle(.secondary)
                .lineLimit(1)
        }
        .padding(.vertical, 4)
    }
}

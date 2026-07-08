import LegadoShared
import SwiftUI

struct RssReaderView: View {
    @EnvironmentObject private var app: AppState
    let article: SharedRssArticle

    private var displayedArticle: SharedRssArticle {
        app.selectedRssArticle ?? article
    }

    private var displayedContent: String {
        if !app.rssContent.isEmpty {
            return app.rssContent
        }
        return article.readableContent
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                if app.isLoading && app.rssContent.isEmpty {
                    HStack {
                        ProgressView()
                        Text("Loading")
                            .foregroundStyle(.secondary)
                    }
                    .frame(maxWidth: .infinity, alignment: .center)
                    .padding(.top, 80)
                } else {
                    Text(displayedArticle.title.isEmpty ? displayedArticle.link : displayedArticle.title)
                        .font(.title2.weight(.semibold))

                    if let date = displayedArticle.pubDate, !date.isEmpty {
                        Text(date)
                            .font(.footnote)
                            .foregroundStyle(.secondary)
                    }

                    Text(displayedContent)
                        .font(.body)
                        .lineSpacing(7)
                        .textSelection(.enabled)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(20)
        }
        .navigationTitle(article.title.isEmpty ? "RSS" : article.title)
        .navigationBarTitleDisplayMode(.inline)
    }
}

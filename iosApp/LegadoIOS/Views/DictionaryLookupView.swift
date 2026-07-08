import LegadoShared
import SwiftUI

struct DictionaryLookupView: View {
    @EnvironmentObject private var app: AppState
    @FocusState private var keywordFocused: Bool

    var body: some View {
        List {
            Section("Lookup") {
                TextField("Word or phrase", text: $app.dictionaryKeyword)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                    .focused($keywordFocused)
                    .submitLabel(.search)
                    .onSubmit {
                        runLookup()
                    }

                Button {
                    runLookup()
                } label: {
                    Label("Search dictionaries", systemImage: "magnifyingglass")
                }
                .disabled(app.dictionaryKeyword.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || app.isLoading)
            }

            Section("Rules") {
                LabeledContent("Enabled", value: "\(app.dictRules.filter { $0.enabled }.count)")
                LabeledContent("Total", value: "\(app.dictRules.count)")
                NavigationLink {
                    DictRuleListView()
                } label: {
                    Label("Manage dictionary rules", systemImage: "character.book.closed")
                }
            }

            Section("Results") {
                if app.isLoading {
                    HStack {
                        ProgressView()
                        Text("Searching...")
                            .foregroundStyle(.secondary)
                    }
                } else if app.dictionaryLookupResults.isEmpty {
                    EmptyStateView(title: "No dictionary results", systemImage: "text.magnifyingglass")
                } else {
                    ForEach(app.dictionaryLookupResults.indices, id: \.self) { index in
                        DictionaryLookupResultRow(result: app.dictionaryLookupResults[index])
                    }
                }
            }
        }
        .navigationTitle("Dictionary")
        .toolbar {
            Button {
                runLookup()
            } label: {
                Image(systemName: "magnifyingglass")
            }
            .disabled(app.dictionaryKeyword.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || app.isLoading)
        }
    }

    private func runLookup() {
        keywordFocused = false
        Task {
            await app.lookupDictionary()
        }
    }
}

private struct DictionaryLookupResultRow: View {
    let result: SharedDictionaryLookupResult

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text(result.ruleName)
                    .font(.headline)
                Spacer()
                if result.errorMessage == nil {
                    Text("\(result.statusCode)")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                } else {
                    Image(systemName: "exclamationmark.triangle")
                        .foregroundStyle(.orange)
                }
            }

            if let error = result.errorMessage {
                Text(error)
                    .font(.footnote)
                    .foregroundStyle(.red)
                    .textSelection(.enabled)
            } else {
                Text(displayContent(result.content))
                    .font(.body)
                    .textSelection(.enabled)
            }

            if !result.url.isEmpty {
                Text(result.url)
                    .font(.caption)
                    .foregroundStyle(.secondary)
                    .lineLimit(2)
                    .textSelection(.enabled)
            }
        }
        .padding(.vertical, 4)
    }

    private func displayContent(_ content: String) -> String {
        content
            .replacingOccurrences(of: #"(?i)<br\s*/?>"#, with: "\n", options: .regularExpression)
            .replacingOccurrences(of: #"(?i)</(p|div|h[1-6]|li|tr)>"#, with: "\n", options: .regularExpression)
            .replacingOccurrences(of: #"<[^>]+>"#, with: " ", options: .regularExpression)
            .replacingOccurrences(of: "&nbsp;", with: " ")
            .replacingOccurrences(of: "&amp;", with: "&")
            .replacingOccurrences(of: "&lt;", with: "<")
            .replacingOccurrences(of: "&gt;", with: ">")
            .split(separator: "\n", omittingEmptySubsequences: false)
            .map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }
            .filter { !$0.isEmpty }
            .joined(separator: "\n")
    }
}

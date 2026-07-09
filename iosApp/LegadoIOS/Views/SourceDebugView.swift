import LegadoShared
import SwiftUI

struct SourceDebugView: View {
    @EnvironmentObject private var app: AppState
    let source: SharedBookSource

    var body: some View {
        List {
            Section {
                VStack(alignment: .leading, spacing: 4) {
                    Text(source.bookSourceName.isEmpty ? source.bookSourceUrl : source.bookSourceName)
                        .font(.headline)
                    Text(source.bookSourceUrl)
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                }
                TextField("Keyword", text: $app.keyword)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                Button {
                    Task {
                        await app.debugSource(source)
                    }
                } label: {
                    Label("Run debug", systemImage: "stethoscope")
                }
                .disabled(app.isLoading)
            }

            if let loginUrl = source.loginUrl, !loginUrl.isEmpty {
                Section("Account") {
                    NavigationLink {
                        SourceLoginView(source: source)
                    } label: {
                        Label("Login / Cookie", systemImage: "key")
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

            if !app.debugSteps.isEmpty {
                Section("Steps") {
                    ForEach(app.debugSteps.indices, id: \.self) { index in
                        let step = app.debugSteps[index]
                        VStack(alignment: .leading, spacing: 4) {
                            Text(step.stage)
                                .font(.headline)
                            Text(step.message)
                                .foregroundStyle(.secondary)
                            if let url = step.url, !url.isEmpty {
                                Text(url)
                                    .font(.footnote)
                                    .foregroundStyle(.secondary)
                            }
                        }
                        .padding(.vertical, 3)
                    }
                }
            }

            if !app.debugContent.isEmpty {
                Section("Content") {
                    Text(app.debugContent)
                        .textSelection(.enabled)
                }
            }
        }
        .navigationTitle("Debug")
        .toolbar {
            if let loginUrl = source.loginUrl, !loginUrl.isEmpty {
                NavigationLink {
                    SourceLoginView(source: source)
                } label: {
                    Image(systemName: "key")
                }
            }
        }
    }
}

import LegadoShared
import SwiftUI

struct BookEditView: View {
    @EnvironmentObject private var app: AppState
    @Environment(\.dismiss) private var dismiss

    @State private var name: String
    @State private var author: String
    @State private var customIntro: String
    @State private var customCoverUrl: String
    @State private var customTag: String

    init(book: SharedBook) {
        _name = State(initialValue: book.name)
        _author = State(initialValue: book.author)
        _customIntro = State(initialValue: book.customIntro ?? book.intro ?? "")
        _customCoverUrl = State(initialValue: book.customCoverUrl ?? book.coverUrl ?? "")
        _customTag = State(initialValue: book.customTag ?? "")
    }

    var body: some View {
        Form {
            Section("Identity") {
                TextField("Name", text: $name)
                TextField("Author", text: $author)
                TextField("Tag", text: $customTag)
            }

            Section("Cover") {
                TextField("Cover URL", text: $customCoverUrl)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
            }

            Section("Intro") {
                TextEditor(text: $customIntro)
                    .frame(minHeight: 180)
            }

            Section {
                Button {
                    app.updateSelectedBookMetadata(
                        name: name,
                        author: author,
                        customIntro: customIntro,
                        customCoverUrl: customCoverUrl,
                        customTag: customTag
                    )
                    dismiss()
                } label: {
                    Label("Save", systemImage: "checkmark")
                }
                .disabled(name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
            }
        }
        .navigationTitle("Edit Book")
        .navigationBarTitleDisplayMode(.inline)
    }
}

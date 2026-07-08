import LegadoShared
import SwiftUI

struct BookGroupListView: View {
    @EnvironmentObject private var app: AppState
    @State private var newGroupName = ""

    var body: some View {
        List {
            Section("New Group") {
                HStack {
                    TextField("Name", text: $newGroupName)
                        .textInputAutocapitalization(.words)
                        .submitLabel(.done)
                        .onSubmit(createGroup)

                    Button(action: createGroup) {
                        Image(systemName: "plus.circle.fill")
                    }
                    .disabled(newGroupName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                }
            }

            Section("Groups") {
                ForEach(app.bookGroups.indices, id: \.self) { index in
                    let group = app.bookGroups[index]
                    VStack(alignment: .leading, spacing: 8) {
                        HStack {
                            Label(group.groupName, systemImage: group.groupId > 0 ? "folder" : "books.vertical")
                            Spacer()
                            Text(group.groupId > 0 ? "Custom" : "System")
                                .font(.caption)
                                .foregroundStyle(.secondary)
                        }

                        Toggle(isOn: Binding(
                            get: { group.show },
                            set: { app.setBookGroupVisible(group, show: $0) }
                        )) {
                            Text("Show on bookshelf")
                        }

                        if group.groupId > 0 {
                            Button(role: .destructive) {
                                app.deleteBookGroup(group)
                            } label: {
                                Label("Delete", systemImage: "trash")
                            }
                        }
                    }
                    .padding(.vertical, 4)
                }
            }
        }
        .navigationTitle("Book Groups")
        .navigationBarTitleDisplayMode(.inline)
    }

    private func createGroup() {
        app.createBookGroup(name: newGroupName)
        newGroupName = ""
    }
}

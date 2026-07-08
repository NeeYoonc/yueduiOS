import LegadoShared
import SwiftUI

struct ReadRecordListView: View {
    @EnvironmentObject private var app: AppState

    var body: some View {
        List {
            if app.readRecords.isEmpty {
                Section {
                    EmptyStateView(title: "No read records", systemImage: "clock")
                }
            } else {
                Section {
                    ForEach(app.readRecords.indices, id: \.self) { index in
                        ReadRecordRow(record: app.readRecords[index])
                    }
                    .onDelete { offsets in
                        offsets
                            .map { app.readRecords[$0] }
                            .forEach(app.deleteReadRecord)
                    }
                } footer: {
                    Text("Read records mirror Android's per-device cumulative reading time records.")
                }
            }
        }
        .navigationTitle("Read Records")
        .toolbar {
            Button(role: .destructive) {
                app.clearReadRecords()
            } label: {
                Image(systemName: "trash")
            }
            .disabled(app.readRecords.isEmpty)
        }
    }
}

private struct ReadRecordRow: View {
    let record: SharedReadRecord

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(record.bookName)
                .font(.headline)

            HStack(spacing: 12) {
                Label(readTimeText, systemImage: "timer")
                if record.lastRead > 0 {
                    Label(lastReadText, systemImage: "clock")
                }
            }
            .font(.caption)
            .foregroundStyle(.secondary)

            Text(record.deviceId)
                .font(.caption.monospaced())
                .foregroundStyle(.secondary)
        }
        .padding(.vertical, 4)
    }

    private var readTimeText: String {
        let totalSeconds = max(Int(record.readTime / 1000), 0)
        let hours = totalSeconds / 3600
        let minutes = (totalSeconds % 3600) / 60
        if hours > 0 {
            return "\(hours)h \(minutes)m"
        }
        return "\(minutes)m"
    }

    private var lastReadText: String {
        Date(timeIntervalSince1970: TimeInterval(record.lastRead) / 1000)
            .formatted(date: .abbreviated, time: .shortened)
    }
}

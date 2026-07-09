import AVKit
import SwiftUI

struct MediaPlayerView: View {
    let title: String
    @State private var player: AVPlayer

    init(url: URL, title: String) {
        self.title = title
        _player = State(initialValue: AVPlayer(url: url))
    }

    var body: some View {
        VideoPlayer(player: player)
            .navigationTitle(title.isEmpty ? "Media" : title)
            .navigationBarTitleDisplayMode(.inline)
            .onAppear {
                player.play()
            }
            .onDisappear {
                player.pause()
            }
    }
}

enum LegadoMediaURL {
    static func make(from raw: String) -> URL? {
        let trimmed = raw.trimmingCharacters(in: .whitespacesAndNewlines)
        guard let url = URL(string: trimmed), isSupported(url) else {
            return nil
        }
        return url
    }

    private static func isSupported(_ url: URL) -> Bool {
        let path = url.path.lowercased()
        return audioExtensions.contains(where: { path.hasSuffix(".\($0)") }) ||
            videoExtensions.contains(where: { path.hasSuffix(".\($0)") })
    }

    private static let audioExtensions = ["mp3", "m4a", "aac", "wav", "flac", "ogg", "opus"]
    private static let videoExtensions = ["mp4", "m4v", "mov", "mkv", "webm", "avi"]
}

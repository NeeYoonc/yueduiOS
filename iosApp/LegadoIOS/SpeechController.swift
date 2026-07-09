import AVFoundation
import Foundation
import LegadoShared

final class SpeechController: NSObject, ObservableObject, AVSpeechSynthesizerDelegate {
    @Published private(set) var isSpeaking = false

    private let synthesizer = AVSpeechSynthesizer()
    private var player: AVPlayer?
    private var audioTask: URLSessionDataTask?
    private var playbackEndObserver: NSObjectProtocol?

    override init() {
        super.init()
        synthesizer.delegate = self
    }

    func speak(_ text: String) {
        let content = text.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !content.isEmpty else {
            return
        }
        stop()
        let utterance = AVSpeechUtterance(string: content)
        utterance.rate = AVSpeechUtteranceDefaultSpeechRate
        synthesizer.speak(utterance)
        isSpeaking = true
    }

    func speakHttp(_ request: SharedHttpRequest, fallbackText: String) {
        let content = fallbackText.trimmingCharacters(in: .whitespacesAndNewlines)
        guard let url = URL(string: request.url) else {
            speak(content)
            return
        }
        stop()
        var urlRequest = URLRequest(url: url)
        urlRequest.httpMethod = httpMethod(from: request)
        headers(from: request).forEach { name, value in
            urlRequest.setValue(value, forHTTPHeaderField: name)
        }
        if let body = request.body, !body.isEmpty {
            urlRequest.httpBody = body.data(using: .utf8)
        }
        isSpeaking = true
        audioTask = URLSession.shared.dataTask(with: urlRequest) { [weak self] data, _, error in
            DispatchQueue.main.async {
                guard let self else { return }
                guard error == nil, let data, !data.isEmpty else {
                    self.speak(content)
                    return
                }
                do {
                    let fileURL = FileManager.default.temporaryDirectory
                        .appendingPathComponent("legado-http-tts-\(UUID().uuidString).mp3")
                    try data.write(to: fileURL, options: [.atomic])
                    self.playAudioFile(fileURL)
                } catch {
                    self.speak(content)
                }
            }
        }
        audioTask?.resume()
    }

    func stop() {
        audioTask?.cancel()
        audioTask = nil
        if let playbackEndObserver {
            NotificationCenter.default.removeObserver(playbackEndObserver)
            self.playbackEndObserver = nil
        }
        player?.pause()
        player = nil
        if synthesizer.isSpeaking {
            synthesizer.stopSpeaking(at: .immediate)
        }
        isSpeaking = false
    }

    func speechSynthesizer(_ synthesizer: AVSpeechSynthesizer, didFinish utterance: AVSpeechUtterance) {
        isSpeaking = false
    }

    func speechSynthesizer(_ synthesizer: AVSpeechSynthesizer, didCancel utterance: AVSpeechUtterance) {
        isSpeaking = false
    }

    private func playAudioFile(_ fileURL: URL) {
        if let playbackEndObserver {
            NotificationCenter.default.removeObserver(playbackEndObserver)
            self.playbackEndObserver = nil
        }
        let item = AVPlayerItem(url: fileURL)
        playbackEndObserver = NotificationCenter.default.addObserver(
            forName: .AVPlayerItemDidPlayToEndTime,
            object: item,
            queue: .main
        ) { [weak self] _ in
            self?.isSpeaking = false
        }
        player = AVPlayer(playerItem: item)
        player?.play()
        isSpeaking = true
    }

    private func headers(from request: SharedHttpRequest) -> [String: String] {
        if let headers = request.headers as? [String: String] {
            return headers
        }
        if let headers = request.headers as? NSDictionary {
            var result: [String: String] = [:]
            headers.forEach { key, value in
                result["\(key)"] = "\(value)"
            }
            return result
        }
        return [:]
    }

    private func httpMethod(from request: SharedHttpRequest) -> String {
        let raw = String(describing: request.method).uppercased()
        if raw.contains("POST") {
            return "POST"
        }
        if raw.contains("PUT") {
            return "PUT"
        }
        if raw.contains("DELETE") {
            return "DELETE"
        }
        if raw.contains("HEAD") {
            return "HEAD"
        }
        return "GET"
    }
}

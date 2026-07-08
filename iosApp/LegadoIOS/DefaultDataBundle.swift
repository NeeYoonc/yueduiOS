import Foundation
import LegadoShared

enum DefaultDataBundle {
    static func payload(bundle: Bundle = .main) -> DefaultDataPayload {
        DefaultDataPayload(
            bookSourcesJson: read("bookSources", bundle: bundle),
            rssSourcesJson: read("rssSources", bundle: bundle),
            httpTtsJson: read("httpTTS", bundle: bundle),
            dictRulesJson: read("dictRules", bundle: bundle),
            txtTocRulesJson: read("txtTocRule", bundle: bundle),
            keyboardAssistsJson: read("keyboardAssists", bundle: bundle),
            readConfigJson: read("readConfig", bundle: bundle),
            themeConfigJson: read("themeConfig", bundle: bundle),
            coverRuleJson: read("coverRule", bundle: bundle),
            directLinkUploadJson: read("directLinkUpload", bundle: bundle)
        )
    }

    private static func read(_ name: String, bundle: Bundle) -> String? {
        guard let url = bundle.url(forResource: name, withExtension: "json", subdirectory: "DefaultData") else {
            return nil
        }
        return try? String(contentsOf: url, encoding: .utf8)
    }
}

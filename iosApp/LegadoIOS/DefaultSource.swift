enum DefaultSource {
    static let json = """
    {
      "bookSourceUrl": "https://api.example.test",
      "bookSourceName": "JSON API Template",
      "searchUrl": "https://api.example.test/search?q={{key}}&page={{page}}",
      "ruleSearch": {
        "bookList": "$.content.content",
        "name": "$.title",
        "author": "$.author",
        "bookUrl": "$.url",
        "lastChapter": "$.newestChapterTitle",
        "intro": "$.desc",
        "coverUrl": "$.cover"
      },
      "ruleBookInfo": {
        "name": "$.content.title",
        "author": "$.content.authorName",
        "kind": "$.content.category",
        "lastChapter": "$.content.newestChapterTitle",
        "intro": "$.content.desc",
        "coverUrl": "$.content.cover",
        "tocUrl": "$.content.catalogUrl"
      },
      "ruleToc": {
        "chapterList": "$.content.content",
        "chapterName": "$.chapterTitle",
        "chapterUrl": "$.url",
        "isVip": "$.vip",
        "isPay": "$.pay"
      },
      "ruleContent": {
        "title": "$.content.title",
        "content": "$.content.text",
        "subContent": "$.content.note",
        "nextContentUrl": "$.content.next"
      }
    }
    """
}

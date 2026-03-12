package dev.gregross.challenges.urls

import java.util.concurrent.ConcurrentHashMap

class UrlStore {

    private val codeToUrl = ConcurrentHashMap<String, String>()
    private val urlToCode = ConcurrentHashMap<String, String>()

    fun create(code: String, url: String): Boolean {
        val existing = codeToUrl.putIfAbsent(code, url)
        if (existing != null) return false
        urlToCode[url] = code
        return true
    }

    fun getUrl(code: String): String? = codeToUrl[code]

    fun getCode(url: String): String? = urlToCode[url]

    fun delete(code: String): Boolean {
        val url = codeToUrl.remove(code) ?: return false
        urlToCode.remove(url)
        return true
    }

    fun size(): Int = codeToUrl.size
}

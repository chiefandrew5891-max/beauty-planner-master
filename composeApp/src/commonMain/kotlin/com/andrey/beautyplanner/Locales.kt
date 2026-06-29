package com.andrey.beautyplanner

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.compose.resources.ExperimentalResourceApi
import com.andrey.beautyplanner.generated.resources.Res

object Locales {

    var currentLanguage by mutableStateOf(
        AppSettings.languageCodes[AppSettings.selectedLanguage] ?: "ru"
    )

    private val json = Json { ignoreUnknownKeys = true }

    // Кэш загруженных языков: "ru" -> mapOf("key" to "value")
    private val strings = mutableMapOf<String, Map<String, String>>()

    // вызывать один раз на старте
    @OptIn(ExperimentalResourceApi::class)
    suspend fun init() {
        ensureLoaded("en")
        ensureLoaded(currentLanguage)
    }

    // если язык меняется из UI
    @OptIn(ExperimentalResourceApi::class)
    suspend fun onLanguageChanged(langCode: String) {
        currentLanguage = langCode
        ensureLoaded(langCode)
        ensureLoaded("en")
    }

    // старый API
    fun t(key: String): String {
        val cur = strings[currentLanguage]
        val en = strings["en"]
        return cur?.get(key) ?: en?.get(key) ?: key
    }

    // старый API сохранен
    fun daysCount(n: Int): String = tPlural("duration_days", n)
    fun hoursCount(n: Int): String = tPlural("duration_hours", n)
    fun minutesCount(n: Int): String = tPlural("duration_minutes", n)

    // новый plural API (внутренний)
    fun tPlural(key: String, count: Int): String {
        val template = t(key)
        return formatPluralTemplate(template, count)
    }

    // ---------------- loading ----------------

    @OptIn(ExperimentalResourceApi::class)
    private suspend fun ensureLoaded(lang: String) {
        if (strings.containsKey(lang)) return
        strings[lang] = loadLang(lang)
    }

    @OptIn(ExperimentalResourceApi::class)
    private suspend fun loadLang(lang: String): Map<String, String> {
        val path = "files/locales/$lang.json"
        return try {
            val bytes = Res.readBytes(path)
            val text = bytes.decodeToString()
            val root = json.parseToJsonElement(text).jsonObject
            buildMap(root.size) {
                for ((k, v) in root) put(k, v.jsonPrimitive.content)
            }
        } catch (_: Throwable) {
            emptyMap()
        }
    }

    // ---------------- plural parser ----------------

    private fun formatPluralTemplate(template: String, count: Int): String {
        // ожидаем ICU-подобный шаблон:
        // {count, plural, one {# day} other {# days}}
        val marker = "plural,"
        val p = template.indexOf(marker)
        if (p == -1) return template.replace("{count}", count.toString())

        val bodyRaw = template.substring(p + marker.length).trim()
        val body = if (bodyRaw.endsWith("}")) bodyRaw.dropLast(1).trim() else bodyRaw

        val forms = parsePluralForms(body)
        val category = pluralCategoryFor(currentLanguage, count)
        val chosen = forms[category] ?: forms["other"] ?: template
        return chosen.replace("#", count.toString())
    }

    private fun parsePluralForms(body: String): Map<String, String> {
        val keys = listOf("zero", "one", "two", "few", "many", "other")
        val out = mutableMapOf<String, String>()
        var i = 0

        while (i < body.length) {
            while (i < body.length && body[i].isWhitespace()) i++

            var found: String? = null
            for (k in keys) {
                if (body.startsWith(k, i)) {
                    found = k
                    break
                }
            }
            if (found == null) {
                i++
                continue
            }

            i += found.length
            while (i < body.length && body[i].isWhitespace()) i++
            if (i >= body.length || body[i] != '{') continue

            val (txt, next) = readBracedText(body, i)
            out[found] = txt
            i = next
        }
        return out
    }

    private fun readBracedText(s: String, openIdx: Int): Pair<String, Int> {
        var depth = 0
        var i = openIdx
        val sb = StringBuilder()

        while (i < s.length) {
            val c = s[i]
            if (c == '{') {
                depth++
                if (depth > 1) sb.append(c)
            } else if (c == '}') {
                depth--
                if (depth == 0) return Pair(sb.toString(), i + 1)
                sb.append(c)
            } else {
                sb.append(c)
            }
            i++
        }
        return Pair(sb.toString(), s.length)
    }

    private fun pluralCategoryFor(lang: String, n: Int): String {
        val x = kotlin.math.abs(n)
        return when (lang) {
            "ru", "uk" -> {
                val m10 = x % 10
                val m100 = x % 100
                when {
                    m10 == 1 && m100 != 11 -> "one"
                    m10 in 2..4 && m100 !in 12..14 -> "few"
                    m10 == 0 || m10 in 5..9 || m100 in 11..14 -> "many"
                    else -> "other"
                }
            }
            "pl" -> {
                val m10 = x % 10
                val m100 = x % 100
                when {
                    x == 1 -> "one"
                    m10 in 2..4 && m100 !in 12..14 -> "few"
                    else -> "many"
                }
            }
            "cs", "sk" -> when {
                x == 1 -> "one"
                x in 2..4 -> "few"
                else -> "other"
            }
            "sl" -> when (x % 100) {
                1 -> "one"
                2 -> "two"
                3, 4 -> "few"
                else -> "other"
            }
            "ar" -> when {
                x == 0 -> "zero"
                x == 1 -> "one"
                x == 2 -> "two"
                x % 100 in 3..10 -> "few"
                x % 100 in 11..99 -> "many"
                else -> "other"
            }
            "ja", "zh", "ko", "tr", "id" -> "other"
            else -> if (x == 1) "one" else "other"
        }
    }
}
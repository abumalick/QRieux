package net.hilson.qrieux.history

import net.hilson.qrieux.PlatformContext
import net.hilson.qrieux.loadHistoryJson
import net.hilson.qrieux.saveHistoryJson

private const val MAX_ENTRIES = 200

fun addHistoryEntry(context: PlatformContext, entry: HistoryEntry) {
    val entries = loadHistory(context).toMutableList()
    entries.add(0, entry)
    if (entries.size > MAX_ENTRIES) {
        entries.subList(MAX_ENTRIES, entries.size).clear()
    }
    saveHistory(context, entries)
}

fun loadHistory(context: PlatformContext): List<HistoryEntry> {
    val json = loadHistoryJson(context)
    if (json.isBlank()) return emptyList()
    return historyFromJson(json)
}

fun deleteHistoryEntry(context: PlatformContext, id: String) {
    val entries = loadHistory(context).filter { it.id != id }
    saveHistory(context, entries)
}

fun clearHistory(context: PlatformContext) {
    saveHistoryJson(context, "[]")
}

private fun saveHistory(context: PlatformContext, entries: List<HistoryEntry>) {
    saveHistoryJson(context, historyToJson(entries))
}

// --- JSON serialization ---

private fun escapeJson(s: String): String = buildString {
    for (c in s) {
        when (c) {
            '"' -> append("\\\"")
            '\\' -> append("\\\\")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> append(c)
        }
    }
}

private fun unescapeJson(s: String): String = buildString {
    var i = 0
    while (i < s.length) {
        if (s[i] == '\\' && i + 1 < s.length) {
            when (s[i + 1]) {
                '"' -> append('"')
                '\\' -> append('\\')
                'n' -> append('\n')
                'r' -> append('\r')
                't' -> append('\t')
                else -> { append(s[i]); append(s[i + 1]) }
            }
            i += 2
        } else {
            append(s[i])
            i++
        }
    }
}

fun HistoryEntry.toJson(): String = buildString {
    append('{')
    append("\"id\":\""); append(escapeJson(id)); append("\",")
    append("\"ts\":"); append(timestamp); append(',')
    append("\"type\":\""); append(type.name); append("\",")
    append("\"raw\":\""); append(escapeJson(rawValue)); append('"')
    if (generatorType != null) {
        append(",\"gen\":\""); append(escapeJson(generatorType)); append('"')
    }
    append('}')
}

private fun extractJsonString(json: String, key: String): String? {
    val searchKey = "\"$key\":\""
    val start = json.indexOf(searchKey)
    if (start < 0) return null
    val valueStart = start + searchKey.length
    val sb = StringBuilder()
    var i = valueStart
    while (i < json.length) {
        if (json[i] == '\\' && i + 1 < json.length) {
            sb.append(json[i])
            sb.append(json[i + 1])
            i += 2
        } else if (json[i] == '"') {
            break
        } else {
            sb.append(json[i])
            i++
        }
    }
    return unescapeJson(sb.toString())
}

private fun extractJsonLong(json: String, key: String): Long? {
    val searchKey = "\"$key\":"
    val start = json.indexOf(searchKey)
    if (start < 0) return null
    val valueStart = start + searchKey.length
    val sb = StringBuilder()
    var i = valueStart
    while (i < json.length && (json[i].isDigit() || json[i] == '-')) {
        sb.append(json[i])
        i++
    }
    return sb.toString().toLongOrNull()
}

fun historyEntryFromJson(json: String): HistoryEntry? {
    val id = extractJsonString(json, "id") ?: return null
    val ts = extractJsonLong(json, "ts") ?: return null
    val typeName = extractJsonString(json, "type") ?: return null
    val raw = extractJsonString(json, "raw") ?: return null
    val gen = extractJsonString(json, "gen")
    val type = try { HistoryEntryType.valueOf(typeName) } catch (_: Exception) { return null }
    return HistoryEntry(id, ts, type, raw, gen)
}

fun historyToJson(entries: List<HistoryEntry>): String = buildString {
    append('[')
    entries.forEachIndexed { i, entry ->
        if (i > 0) append(',')
        append(entry.toJson())
    }
    append(']')
}

fun historyFromJson(json: String): List<HistoryEntry> {
    if (json.isBlank() || json == "[]") return emptyList()
    val result = mutableListOf<HistoryEntry>()
    var depth = 0
    var objStart = -1
    var inString = false
    var escaped = false
    for (i in json.indices) {
        val c = json[i]
        if (escaped) { escaped = false; continue }
        if (c == '\\' && inString) { escaped = true; continue }
        if (c == '"') { inString = !inString; continue }
        if (inString) continue
        when (c) {
            '{' -> { if (depth == 0) objStart = i; depth++ }
            '}' -> {
                depth--
                if (depth == 0 && objStart >= 0) {
                    historyEntryFromJson(json.substring(objStart, i + 1))?.let { result.add(it) }
                    objStart = -1
                }
            }
        }
    }
    return result
}

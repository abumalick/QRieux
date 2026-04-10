package net.hilson.qrieux.history

data class HistoryEntry(
    val id: String,
    val timestamp: Long,
    val type: HistoryEntryType,
    val rawValue: String,
    val generatorType: String? = null
)

enum class HistoryEntryType { SCAN, GENERATE }

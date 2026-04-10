package net.hilson.qrieux.history

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HistoryRepositoryTest {

    @Test
    fun roundTripScanEntry() {
        val entry = HistoryEntry("id-1", 1000L, HistoryEntryType.SCAN, "https://example.com")
        val json = entry.toJson()
        val parsed = historyEntryFromJson(json)
        assertEquals(entry, parsed)
    }

    @Test
    fun roundTripGenerateEntry() {
        val entry = HistoryEntry("id-2", 2000L, HistoryEntryType.GENERATE, "mailto:test@example.com", "Email")
        val json = entry.toJson()
        val parsed = historyEntryFromJson(json)
        assertEquals(entry, parsed)
    }

    @Test
    fun roundTripWithSpecialChars() {
        val raw = "BEGIN:VCARD\nFN:John \"Doe\"\nTEL:+1234\nEND:VCARD"
        val entry = HistoryEntry("id-3", 3000L, HistoryEntryType.SCAN, raw)
        val json = entry.toJson()
        val parsed = historyEntryFromJson(json)
        assertEquals(entry, parsed)
    }

    @Test
    fun roundTripWithBackslash() {
        val entry = HistoryEntry("id-4", 4000L, HistoryEntryType.GENERATE, "WIFI:T:WPA;S:Net\\;work;P:pass\\word;;", "Wifi")
        val json = entry.toJson()
        val parsed = historyEntryFromJson(json)
        assertEquals(entry, parsed)
    }

    @Test
    fun roundTripList() {
        val entries = listOf(
            HistoryEntry("a", 100L, HistoryEntryType.SCAN, "text1"),
            HistoryEntry("b", 200L, HistoryEntryType.GENERATE, "tel:+123", "Phone"),
        )
        val json = historyToJson(entries)
        val parsed = historyFromJson(json)
        assertEquals(entries, parsed)
    }

    @Test
    fun emptyList() {
        assertEquals(emptyList<HistoryEntry>(), historyFromJson(""))
        assertEquals(emptyList<HistoryEntry>(), historyFromJson("[]"))
    }

    @Test
    fun invalidJsonReturnsNull() {
        assertNull(historyEntryFromJson("not json"))
    }

    @Test
    fun roundTripWithBraces() {
        val entry = HistoryEntry("id-6", 6000L, HistoryEntryType.SCAN, """{"key":"value","nested":{"a":1}}""")
        val json = entry.toJson()
        val parsed = historyEntryFromJson(json)
        assertEquals(entry, parsed)
    }

    @Test
    fun listWithBracesInContent() {
        val entries = listOf(
            HistoryEntry("a", 100L, HistoryEntryType.SCAN, """{"url":"https://x.com/{id}"}"""),
            HistoryEntry("b", 200L, HistoryEntryType.GENERATE, "simple text", "Text"),
        )
        val json = historyToJson(entries)
        val parsed = historyFromJson(json)
        assertEquals(entries, parsed)
    }

    @Test
    fun roundTripWithTabs() {
        val entry = HistoryEntry("id-5", 5000L, HistoryEntryType.SCAN, "line1\ttab\nline2")
        val json = entry.toJson()
        val parsed = historyEntryFromJson(json)
        assertEquals(entry, parsed)
    }
}

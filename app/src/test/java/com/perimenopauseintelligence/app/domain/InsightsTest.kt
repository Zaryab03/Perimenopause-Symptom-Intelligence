package com.perimenopauseintelligence.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class InsightsTest {
    private val start = LocalDate.of(2026, 10, 1)
    private val end = LocalDate.of(2026, 10, 10)

    private fun day(d: Int, vararg obs: ObservationRecord, status: EntryStatus = EntryStatus.LOGGED) =
        DayRecord(start.plusDays(d - 1L), status, observations = obs.toList())

    private fun present(id: String, sev: Int? = null) = ObservationRecord(id, ObsState.PRESENT, sev)
    private fun absent(id: String) = ObservationRecord(id, ObsState.ABSENT, null)

    @Test
    fun countsPresentAbsentAndUnmentionedSeparately() {
        val records = listOf(
            day(1, present("headache", 2)),
            day(2, present("headache", 3)),
            day(3, absent("headache")),
            day(4, present("fatigue", 1)),
            day(5, present("fatigue", 1)),
            day(6, status = EntryStatus.SKIPPED),
        )
        val s = Insights.summarize(start, end, records)
        assertEquals(10, s.totalDays)
        assertEquals(5, s.loggedDays)
        assertEquals(1, s.skippedDays)
        assertEquals(4, s.noEntryDays)
        val h = s.stat("headache")
        assertEquals(2, h.presentDays)
        assertEquals(1, h.absentDays)
        assertEquals(2, h.unmentionedDays)
        assertEquals(2.5, h.averageSeverity!!, 1e-9)
        assertTrue(s.sufficient)
    }

    @Test
    fun missingDaysAreNotTreatedAsSymptomFree() {
        val records = listOf(day(1, present("headache", 1)))
        val s = Insights.summarize(start, end, records)
        assertEquals(1, s.stat("headache").presentDays)
        assertEquals(0, s.stat("headache").absentDays)
        assertEquals(9, s.noEntryDays)
        assertFalse(s.sufficient)
    }

    @Test
    fun unrecordedSymptomHasZeroEverythingAndNoSeverity() {
        val s = Insights.summarize(start, end, listOf(day(1, present("fatigue", 2))))
        val h = s.stat("headache")
        assertEquals(0, h.presentDays)
        assertNull(h.averageSeverity)
    }

    @Test
    fun recordsOutsideRangeAreIgnored() {
        val outside = DayRecord(start.minusDays(1), EntryStatus.LOGGED, observations = listOf(present("headache", 3)))
        val s = Insights.summarize(start, end, listOf(outside))
        assertEquals(0, s.loggedDays)
        assertEquals(0, s.stats.size)
    }

    @Test
    fun comparisonRefusesWhenDataIsInsufficient() {
        val few = Insights.summarize(start, end, listOf(day(1, present("headache", 2))))
        val many = Insights.summarize(start, end, (1..6).map { day(it, present("headache", 2)) })
        assertNull(Insights.compare(few, many, "headache").frequency)
        assertNull(Insights.compare(many, few, "headache").frequency)
    }

    @Test
    fun comparisonDetectsMoreLessAndSimilar() {
        val high = Insights.summarize(start, end, (1..6).map { day(it, present("headache", 3)) })        // rate 1.0
        val low = Insights.summarize(start, end, (1..6).map { day(it, if (it <= 2) present("headache", 1) else absent("headache")) }) // 2/6
        val same = Insights.summarize(start, end, (1..6).map { day(it, present("headache", 3)) })
        assertEquals(Direction.MORE, Insights.compare(high, low, "headache").frequency)
        assertEquals(Direction.LESS, Insights.compare(low, high, "headache").frequency)
        assertEquals(Direction.SIMILAR, Insights.compare(high, same, "headache").frequency)
        // severity needs >= 3 severity observations in each period; low has only 2
        assertNull(Insights.compare(high, low, "headache").severity)
        assertEquals(Direction.SIMILAR, Insights.compare(high, same, "headache").severity)
    }

    @Test
    fun wordingNeverImpliesCause() {
        val s = Insights.summarize(start, end, (1..6).map { day(it, present("headache", 2)) })
        val text = InsightText.frequency("Headache", s.stat("headache"))
        assertTrue(text.contains("recorded on 6 days out of 6 days with entries"))
        listOf("because", "caused", "due to", "diagnos").forEach { assertFalse(text.lowercase().contains(it)) }
    }
}

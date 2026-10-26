package com.perimenopauseintelligence.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ExportersTest {
    private val syms = mapOf("headache" to SymptomInfo("headache", "Headache", "Body"))
    private val d = LocalDate.of(2026, 10, 3)

    @Test
    fun csvEscapesQuotesCommasAndNewlines() {
        assertEquals("\"a,b\"", Exporters.csvCell("a,b"))
        assertEquals("\"say \"\"hi\"\"\"", Exporters.csvCell("say \"hi\""))
        assertEquals("\"line1\nline2\"", Exporters.csvCell("line1\nline2"))
    }

    @Test
    fun csvNeutralisesFormulaInjection() {
        assertEquals("'=1+1", Exporters.csvCell("=1+1"))
        assertEquals("'@SUM(A1)", Exporters.csvCell("@SUM(A1)"))
        assertEquals("'-5", Exporters.csvCell("-5"))
    }

    @Test
    fun csvKeepsDaysWithoutObservationsAndCanOmitNotes() {
        val rec = listOf(
            DayRecord(d, EntryStatus.LOGGED, notes = "secret note", observations = listOf(ObservationRecord("headache", ObsState.PRESENT, 2))),
            DayRecord(d.plusDays(1), EntryStatus.SKIPPED),
        )
        val withNotes = Exporters.csv(rec, syms, includeNotes = true).trim().split("\r\n")
        assertEquals(3, withNotes.size)
        assertTrue(withNotes[1].startsWith("2026-10-03,logged,Headache,present,2"))
        assertTrue(withNotes[1].contains("secret note"))
        assertTrue(withNotes[2].startsWith("2026-10-04,skipped,,,"))
        assertFalse(Exporters.csv(rec, syms, includeNotes = false).contains("secret note"))
    }

    @Test
    fun jsonEscapesAndOmitsNotesWhenAsked() {
        val rec = listOf(DayRecord(d, EntryStatus.LOGGED, notes = "he said \"no\"\nok"))
        val json = Exporters.json(rec, syms, d, d, d, includeNotes = true)
        assertTrue(json.contains("he said \\\"no\\\"\\nok"))
        assertFalse(Exporters.json(rec, syms, d, d, d, includeNotes = false).contains("he said"))
    }

    @Test
    fun reportContainsOnlyRequestedRangeAndDistinguishesMissing() {
        val inRange = (1..6).map { DayRecord(d.plusDays(it - 1L), EntryStatus.LOGGED, observations = listOf(ObservationRecord("headache", ObsState.PRESENT, 1))) }
        val outside = DayRecord(d.minusDays(30), EntryStatus.LOGGED, notes = "OUTSIDE")
        val spec = ReportSpec("T", d, d.plusDays(9), includeNotes = true)
        val text = ReportBuilder.build(spec, syms, inRange + outside, d.plusDays(10)).joinToString("\n") { it.toString() }
        assertFalse(text.contains("OUTSIDE"))
        assertTrue(text.contains("Days with no entry: 4"))
        assertTrue(text.contains("not symptom-free"))
    }

    @Test
    fun reportWithTooFewDaysShowsNoFrequencySummary() {
        val one = listOf(DayRecord(d, EntryStatus.LOGGED, observations = listOf(ObservationRecord("headache", ObsState.PRESENT, 1))))
        val blocks = ReportBuilder.build(ReportSpec("T", d, d.plusDays(9)), syms, one, d)
        assertTrue(blocks.none { it is ReportBlock.Heading && it.text == "Symptom summary" })
    }
}

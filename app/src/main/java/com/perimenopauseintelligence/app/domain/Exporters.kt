package com.perimenopauseintelligence.app.domain

import java.time.LocalDate

object Exporters {
    private val header = listOf("date", "status", "symptom", "state", "severity", "sleep", "mood", "energy", "menstrual", "notes")

    /** One row per observation. Days with no observations still get one row so they are not lost. */
    fun csv(records: List<DayRecord>, symptoms: Map<String, SymptomInfo>, includeNotes: Boolean): String {
        val sb = StringBuilder()
        sb.append(header.joinToString(",")).append("\r\n")
        for (d in records.sortedBy { it.date }) {
            val base = listOf(
                d.date.toString(),
                d.status.name.lowercase(),
            )
            val tail = listOf(
                d.sleep?.toString().orEmpty(),
                d.mood?.toString().orEmpty(),
                d.energy?.toString().orEmpty(),
                d.cycle?.name?.lowercase().orEmpty(),
                if (includeNotes) d.notes else "",
            )
            val rows = if (d.observations.isEmpty()) {
                listOf(base + listOf("", "", "") + tail)
            } else {
                d.observations.map { o ->
                    base + listOf(
                        symptoms[o.symptomId]?.label ?: o.symptomId,
                        o.state.name.lowercase(),
                        o.severity?.toString().orEmpty(),
                    ) + tail
                }
            }
            for (r in rows) sb.append(r.joinToString(",") { csvCell(it) }).append("\r\n")
        }
        return sb.toString()
    }

    /** Quotes cells and neutralises spreadsheet formula injection (cells starting with = + - @). */
    fun csvCell(value: String): String {
        var v = value
        if (v.isNotEmpty() && v[0] in "=+-@\t\r") v = "'$v"
        return if (v.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"" + v.replace("\"", "\"\"") + "\""
        } else {
            v
        }
    }

    fun json(
        records: List<DayRecord>,
        symptoms: Map<String, SymptomInfo>,
        start: LocalDate?,
        end: LocalDate?,
        generatedOn: LocalDate,
        includeNotes: Boolean,
    ): String {
        val sb = StringBuilder()
        sb.append("{\n")
        sb.append("  \"app\": \"Perimenopause Symptom Intelligence\",\n")
        sb.append("  \"formatVersion\": 1,\n")
        sb.append("  \"generatedOn\": ${q(generatedOn.toString())},\n")
        sb.append("  \"rangeStart\": ${start?.let { q(it.toString()) } ?: "null"},\n")
        sb.append("  \"rangeEnd\": ${end?.let { q(it.toString()) } ?: "null"},\n")
        sb.append("  \"note\": \"Self-recorded data. Days with no entry are missing, not symptom-free.\",\n")
        sb.append("  \"days\": [")
        val sorted = records.sortedBy { it.date }
        sorted.forEachIndexed { i, d ->
            sb.append(if (i == 0) "\n" else ",\n")
            sb.append("    {\"date\": ${q(d.date.toString())}, \"status\": ${q(d.status.name.lowercase())}")
            sb.append(", \"sleep\": ${d.sleep ?: "null"}, \"mood\": ${d.mood ?: "null"}, \"energy\": ${d.energy ?: "null"}")
            sb.append(", \"menstrual\": ${d.cycle?.let { q(it.name.lowercase()) } ?: "null"}")
            sb.append(", \"notes\": ${if (includeNotes) q(d.notes) else "null"}")
            sb.append(", \"symptoms\": [")
            d.observations.forEachIndexed { j, o ->
                if (j > 0) sb.append(", ")
                sb.append("{\"name\": ${q(symptoms[o.symptomId]?.label ?: o.symptomId)}")
                sb.append(", \"state\": ${q(o.state.name.lowercase())}, \"severity\": ${o.severity ?: "null"}}")
            }
            sb.append("]}")
        }
        sb.append(if (sorted.isEmpty()) "]\n" else "\n  ]\n")
        sb.append("}\n")
        return sb.toString()
    }

    fun q(s: String): String {
        val sb = StringBuilder("\"")
        for (c in s) {
            when {
                c == '"' -> sb.append("\\\"")
                c == '\\' -> sb.append("\\\\")
                c == '\n' -> sb.append("\\n")
                c == '\r' -> sb.append("\\r")
                c == '\t' -> sb.append("\\t")
                c < ' ' -> sb.append("\\u%04x".format(c.code))
                else -> sb.append(c)
            }
        }
        return sb.append('"').toString()
    }
}

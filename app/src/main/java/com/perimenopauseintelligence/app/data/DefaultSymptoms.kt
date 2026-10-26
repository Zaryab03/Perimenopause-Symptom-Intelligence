package com.perimenopauseintelligence.app.data

/** Seed catalogue. Rows live in the database, so the list can grow without touching UI code. */
object DefaultSymptoms {
    val categories = listOf(
        "Hot flushes & sweats", "Sleep", "Mood", "Thinking & energy", "Body", "Intimate & urinary", "Bleeding", "Other",
    )

    private val items = listOf(
        Triple("hot_flushes", "Hot flushes", "Hot flushes & sweats"),
        Triple("night_sweats", "Night sweats", "Hot flushes & sweats"),
        Triple("trouble_falling_asleep", "Trouble falling asleep", "Sleep"),
        Triple("night_waking", "Waking during the night", "Sleep"),
        Triple("low_mood", "Low mood", "Mood"),
        Triple("anxiety", "Anxiety or worry", "Mood"),
        Triple("irritability", "Irritability", "Mood"),
        Triple("mood_swings", "Mood swings", "Mood"),
        Triple("concentration", "Trouble concentrating", "Thinking & energy"),
        Triple("brain_fog", "Brain fog or forgetfulness", "Thinking & energy"),
        Triple("fatigue", "Fatigue", "Thinking & energy"),
        Triple("headache", "Headache", "Body"),
        Triple("joint_aches", "Joint or muscle aches", "Body"),
        Triple("palpitations", "Heart palpitations", "Body"),
        Triple("breast_tenderness", "Breast tenderness", "Body"),
        Triple("bloating", "Bloating", "Body"),
        Triple("skin_hair", "Dry skin or hair changes", "Body"),
        Triple("vaginal_dryness", "Vaginal dryness", "Intimate & urinary"),
        Triple("low_libido", "Lower sex drive", "Intimate & urinary"),
        Triple("bladder", "Bladder urgency or leaks", "Intimate & urinary"),
        Triple("heavy_bleeding", "Heavy bleeding", "Bleeding"),
    )

    /** Pre-selected during onboarding; everything else stays available under "Add or hide symptoms". */
    val starterIds = setOf(
        "hot_flushes", "night_sweats", "trouble_falling_asleep", "night_waking",
        "low_mood", "anxiety", "fatigue", "brain_fog",
    )

    fun emojiFor(category: String): String = when (category) {
        "Hot flushes & sweats" -> "\uD83D\uDD25"
        "Sleep" -> "\uD83C\uDF19"
        "Mood" -> "\uD83D\uDCAD"
        "Thinking & energy" -> "\u26A1"
        "Body" -> "\uD83C\uDF3F"
        "Intimate & urinary" -> "\uD83D\uDCA7"
        "Bleeding" -> "\uD83E\uDE78"
        else -> "\u2728"
    }

    val all: List<SymptomEntity> = items.mapIndexed { i, (id, label, cat) ->
        SymptomEntity(id = id, label = label, category = cat, isCustom = false, sortOrder = i)
    }
}

package com.perimenopauseintelligence.app.content

data class Article(val id: String, val title: String, val summary: String, val paragraphs: List<String>)

/**
 * IMPORTANT: this text is a plain-English DRAFT written for testing. It has NOT been reviewed by a
 * qualified clinician. It must be reviewed and sourced before any public release.
 */
object Articles {
    const val STATUS = "Draft for testing — not yet reviewed by a clinician. Written October 2026."

    val sourcesToVerify = listOf(
        "NHS: Menopause and perimenopause pages",
        "NICE guideline NG23: Menopause: identification and management",
        "The Menopause Society (formerly NAMS) patient resources",
        "American College of Obstetricians and Gynecologists (ACOG) patient FAQs",
    )

    val all = listOf(
        Article(
            "what-is", "What is perimenopause?", "The transition before periods stop.",
            listOf(
                "Perimenopause is the transition leading up to menopause, the point when a person has not had a period for 12 months. During this time, hormone levels change and can fluctuate, and many people notice changes in their bodies and routines.",
                "It commonly begins in the 40s, but timing varies a lot from person to person, and some people notice changes earlier or later. How long it lasts also varies; it is often several years.",
                "Not everyone has the same experience. Some people have few noticeable changes, others have many. Having a symptom does not by itself mean you are in perimenopause, because many conditions can cause similar symptoms.",
            ),
        ),
        Article(
            "symptoms", "Commonly reported symptoms", "What people often notice.",
            listOf(
                "People commonly report changes in their periods, hot flushes and night sweats, difficulty sleeping, changes in mood, trouble concentrating, tiredness, vaginal dryness, changes in sex drive, bladder changes, headaches, and aches in joints and muscles.",
                "These can come and go, and they can vary in strength from week to week. Many of these symptoms can also have other causes, such as stress, sleep problems, thyroid conditions, medicines, or other health conditions.",
                "Recording what you notice over time can help you and a healthcare professional see what is happening for you, without relying on memory.",
            ),
        ),
        Article(
            "periods", "Changes in periods", "Cycles can change in many ways.",
            listOf(
                "During perimenopause, periods may become shorter or longer, lighter or heavier, or less predictable. You may skip some periods and then have them return.",
                "Not everyone has regular cycles to begin with, and some people do not have periods for other reasons. This app lets you record bleeding as \"Doesn't apply to me\" if that is the case for you.",
                "Some bleeding patterns should be checked by a healthcare professional promptly. See \"When to seek medical attention\".",
            ),
        ),
        Article(
            "sleep-mood", "Sleep, mood, concentration and energy", "Everyday changes worth noticing.",
            listOf(
                "Sleep can be disturbed by night sweats, worry, or changes in routine. Poor sleep can in turn affect mood, concentration and energy, which makes it hard to tell what is causing what. That is one reason this app describes what you recorded and does not claim to explain causes.",
                "Noting how you slept, how your mood was, and how your energy felt on a simple 1–5 scale can show patterns over weeks that are hard to notice day by day.",
                "If low mood, anxiety or other feelings are affecting your daily life, please talk to a healthcare professional. You do not have to wait for an appointment about perimenopause to do that.",
            ),
        ),
        Article(
            "tracking", "Tracking symptoms over time", "Getting the most from your records.",
            listOf(
                "A few minutes of recording when it suits you is enough. You do not need to log every day.",
                "Choose \"Not today\" when you want to note that a symptom was not present. If you leave a symptom blank, the app treats it as not recorded, because no entry does not mean no symptom.",
                "The more days you have recorded, the more meaningful a summary becomes. With only a few entries, the app will tell you there is not enough information yet.",
            ),
        ),
        Article(
            "appointment", "Preparing for an appointment", "Make the visit count.",
            listOf(
                "Choose a recent period such as the last 30 or 90 days and look at which symptoms you recorded most often and how much they affected you.",
                "Write down your questions beforehand. Common topics include what could be causing your symptoms, which tests or treatments are options, what lifestyle changes may help, and when to follow up.",
                "Use Appointment preparation in the More tab to create a PDF you can show or send to your clinician. You decide whether to share it.",
            ),
        ),
        Article(
            "talking", "Talking with your clinician", "Describing what you notice.",
            listOf(
                "Start with what bothers you most. Describe how often it happens, how strong it is, and how it affects your day.",
                "Mention any medicines or supplements you take, your family history, and any changes in your periods.",
                "It is fine to ask for things to be explained again, to ask about the evidence for a recommendation, and to ask what to do if things do not improve.",
            ),
        ),
        Article(
            "urgent", "When to seek medical attention", "Do not wait for a routine appointment.",
            listOf(
                "This is general information and not a complete list. If you are worried about any symptom, contact a healthcare professional.",
                "Seek prompt medical advice for: very heavy bleeding (for example soaking through a pad or tampon every hour for several hours), bleeding between periods or after sex, bleeding after you have gone 12 months without a period, periods that last much longer than usual, or new pelvic pain.",
                "Seek urgent or emergency help for: chest pain, severe shortness of breath, sudden severe headache, signs of a stroke, or thoughts of harming yourself. If you are in immediate danger, call your local emergency number.",
            ),
        ),
    )

    fun byId(id: String): Article? = all.firstOrNull { it.id == id }
}

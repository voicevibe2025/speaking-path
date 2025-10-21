package com.example.voicevibe.presentation.screens.storytime

data class Story(
    val slug: String,
    val title: String,
    val moral: String,
    val summary: String,
    val audioFile: String
)

object StoryCatalog {
    val stories: List<Story> = listOf(
        Story(
            slug = "malin_kundang",
            title = "Malin Kundang",
            moral = "Respect and honor your parents.",
            summary = "A poor boy becomes a rich merchant but denies his mother and is cursed into stone.",
            audioFile = "malin_kundang.ogg"
        ),
        Story(
            slug = "sangkuriang",
            title = "Sangkuriang",
            moral = "Obedience and awareness of one’s past are important.",
            summary = "Sangkuriang unknowingly loves his mother; an impossible task leads to Mount Tangkuban Perahu.",
            audioFile = "sangkuriang.ogg"
        ),
        Story(
            slug = "timun_mas",
            title = "Timun Mas",
            moral = "Bravery and cleverness can defeat evil.",
            summary = "Timun Mas escapes a giant using magical items that become obstacles.",
            audioFile = "timun_mas.ogg"
        ),
        Story(
            slug = "roro_jonggrang",
            title = "Roro Jonggrang",
            moral = "Honesty and sincerity are essential in promises.",
            summary = "A trick prevents 1,000 temples from finishing; the princess is cursed into stone.",
            audioFile = "roro_jonggrang.ogg"
        ),
        Story(
            slug = "legend_of_lake_toba",
            title = "Legend of Lake Toba",
            moral = "Keep promises and be grateful.",
            summary = "A broken promise leads to a great flood forming Lake Toba and Samosir Island.",
            audioFile = "legend_of_lake_toba.ogg"
        )
    )

    fun bySlug(slug: String): Story? = stories.find { it.slug == slug }
}

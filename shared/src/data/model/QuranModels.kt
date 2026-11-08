package com.jetbrains.kmpapp.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.math.max
import kotlin.math.min

object QuranConstants {
    const val PAGES_FIRST = 1
    const val FIRST_SURA = 1
    const val LAST_SURA = 114
    const val NUMBER_OF_SURAS = 114
    const val JUZ2_COUNT = 30
    const val MIN_AYAH = 1
    const val MAX_AYAH = 286
    const val NO_PAGE = -1
    const val MAX_RECENT_PAGES = 5
    const val TRANSLATION_HOST = "https://quran.app/"
    const val ANDROID_QURAN_DATA_HOST = "https://android.quran.com/data"
}

@Serializable
data class SuraAyah(
    val sura: Int,
    val ayah: Int
) : Comparable<SuraAyah> {
    val key: String get() = "$sura:$ayah"

    override fun compareTo(other: SuraAyah): Int {
        return if (this == other) 0
        else if (sura != other.sura) sura.compareTo(other.sura)
        else ayah.compareTo(other.ayah)
    }

    override fun toString(): String = "($sura:$ayah)"
}

@Serializable
data class VerseRange(
    val startSura: Int,
    val startAyah: Int,
    val endingSura: Int,
    val endingAyah: Int,
    val versesInRange: Int = -1
) {
    val start: SuraAyah get() = SuraAyah(startSura, startAyah)
    val end: SuraAyah get() = SuraAyah(endingSura, endingAyah)

    fun contains(sura: Int, ayah: Int): Boolean {
        val target = SuraAyah(sura, ayah)
        return target >= start && target <= end
    }
}

@Serializable
data class QuranText(
    val sura: Int,
    val ayah: Int,
    val text: String,
    val extraData: String? = null,
    val translationId: Int? = null,
    val translatorName: String? = null
)

@Serializable
enum class MushafType(
    val id: String,
    val titleKey: String,
    val defaultTitle: String,
    val descriptionKey: String,
    val defaultDescription: String
) {
    MADANI(
        id = "madani",
        titleKey = "madani_title",
        defaultTitle = "Madani (1405H)",
        descriptionKey = "madani_description",
        defaultDescription = "Classic King Fahd Complex Madani Mushaf (604 pages)"
    ),
    TAJWEED(
        id = "tajweed",
        titleKey = "tajweed_title",
        defaultTitle = "Madani Tajweed",
        descriptionKey = "tajweed_description",
        defaultDescription = "Color-coded Tajweed rules on the 604-page Madani Mushaf"
    ),
    WARSH(
        id = "warsh",
        titleKey = "warsh_title",
        defaultTitle = "Warsh (Nafiʻ)",
        descriptionKey = "warsh_description",
        defaultDescription = "Riwayat Warsh ʻan Nafiʻ Mushaf edition (560 pages)"
    ),
    QALOON(
        id = "qaloon",
        titleKey = "qaloon_title",
        defaultTitle = "Qaloon (Nafiʻ)",
        descriptionKey = "qaloon_description",
        defaultDescription = "Riwayat Qaloon ʻan Nafiʻ Mushaf edition (604 pages)"
    ),
    NASKH(
        id = "naskh",
        titleKey = "naskh_title",
        defaultTitle = "IndoPak / Naskh (15-line)",
        descriptionKey = "naskh_description",
        defaultDescription = "15-line Naskh script Mushaf (610 pages)"
    );

    val displayName: String get() = defaultTitle

    companion object {
        fun fromId(id: String): MushafType = entries.find { it.id == id } ?: MADANI
    }
}

@Serializable
enum class PageAppearance(val id: String, val label: String) {
    LIGHT_CREAM("cream", "Classic Cream"),
    WHITE("white", "Pure White"),
    SEPIA("sepia", "Warm Sepia"),
    GREEN("green", "Soft Mint"),
    NIGHT("night", "Night Mode");

    val displayName: String get() = label

    companion object {
        fun fromId(id: String): PageAppearance = entries.find { it.id == id } ?: LIGHT_CREAM
    }
}

@Serializable
enum class ReaderViewMode {
    MUSHAF_PAGE,
    TRANSLATION,
    SPLIT_MUSHAF_AND_TRANSLATION
}

@Serializable
data class RectF(
    var left: Float = 0f,
    var top: Float = 0f,
    var right: Float = 0f,
    var bottom: Float = 0f
) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
    val isEmpty: Boolean get() = left >= right || top >= bottom

    fun contains(x: Float, y: Float): Boolean {
        return !isEmpty && x >= left && x <= right && y >= top && y <= bottom
    }

    fun contains(other: RectF): Boolean {
        return !isEmpty && !other.isEmpty &&
            left <= other.left && top <= other.top &&
            right >= other.right && bottom >= other.bottom
    }

    fun union(other: RectF): RectF {
        if (other.isEmpty) return this.copy()
        if (this.isEmpty) return other.copy()
        return RectF(
            left = min(left, other.left),
            top = min(top, other.top),
            right = max(right, other.right),
            bottom = max(bottom, other.bottom)
        )
    }

    fun engulf(other: RectF) {
        if (other.isEmpty) return
        if (this.isEmpty) {
            left = other.left
            top = other.top
            right = other.right
            bottom = other.bottom
        } else {
            left = min(left, other.left)
            top = min(top, other.top)
            right = max(right, other.right)
            bottom = max(bottom, other.bottom)
        }
    }
}

@Serializable
data class AyahBounds(
    val line: Int,
    val position: Int,
    var bounds: RectF
) {
    constructor(line: Int, position: Int, minX: Int, minY: Int, maxX: Int, maxY: Int) : this(
        line = line,
        position = position,
        bounds = RectF(minX.toFloat(), minY.toFloat(), maxX.toFloat(), maxY.toFloat())
    )

    fun engulf(other: AyahBounds) {
        bounds.engulf(other.bounds)
    }

    fun withBounds(newBounds: RectF): AyahBounds = copy(bounds = newBounds.copy())
}

@Serializable
data class SuraHeaderLocation(
    val sura: Int,
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int
)

@Serializable
data class AyahMarkerLocation(
    val sura: Int,
    val ayah: Int,
    val x: Int,
    val y: Int
)

@Serializable
data class PageCoordinates(
    val page: Int,
    val pageBounds: RectF,
    val suraHeaders: List<SuraHeaderLocation> = emptyList(),
    val ayahMarkers: List<AyahMarkerLocation> = emptyList()
)

@Serializable
data class AyahCoordinates(
    val page: Int,
    val ayahCoordinates: Map<String, List<AyahBounds>>,
    val ayahLineCoordinates: Map<String, List<AyahBounds>> = emptyMap(),
    val underlineRaiseRatio: Float = 0f
) {
    val glyphCoordinates: Map<String, List<AyahBounds>> get() = ayahCoordinates

    fun findAyahAt(imageX: Float, imageY: Float): SuraAyah? {
        for ((key, boundsList) in ayahCoordinates) {
            for (b in boundsList) {
                if (b.bounds.contains(imageX, imageY)) {
                    val parts = key.split(":")
                    if (parts.size == 2) {
                        val s = parts[0].toIntOrNull()
                        val a = parts[1].toIntOrNull()
                        if (s != null && a != null) return SuraAyah(s, a)
                    }
                }
            }
        }
        // Fallback: closest bound within 24px
        var bestKey: String? = null
        var bestDist = 24f
        for ((key, boundsList) in ayahCoordinates) {
            for (b in boundsList) {
                val r = b.bounds
                val dx = when {
                    imageX < r.left -> r.left - imageX
                    imageX > r.right -> imageX - r.right
                    else -> 0f
                }
                val dy = when {
                    imageY < r.top -> r.top - imageY
                    imageY > r.bottom -> imageY - r.bottom
                    else -> 0f
                }
                val dist = dx + dy
                if (dist < bestDist) {
                    bestDist = dist
                    bestKey = key
                }
            }
        }
        return bestKey?.split(":")?.let { parts ->
            if (parts.size == 2) SuraAyah(parts[0].toInt(), parts[1].toInt()) else null
        }
    }
}

@Serializable
data class Bookmark(
    val id: Long,
    val sura: Int? = null,
    val ayah: Int? = null,
    val page: Int,
    val timestamp: Long = 0L,
    val tags: List<Long> = emptyList(),
    val ayahText: String? = null
) {
    val isPageBookmark: Boolean get() = sura == null || ayah == null
    val suraAyah: SuraAyah? get() = if (sura != null && ayah != null) SuraAyah(sura, ayah) else null
}

@Serializable
data class Tag(
    val id: Long,
    val name: String,
    val timestamp: Long = 0L
)

@Serializable
data class RecentPage(
    val page: Int,
    val timestamp: Long
)

@Serializable
data class Qari(
    val id: Int,
    val nameKey: String,
    val defaultName: String,
    val url: String,
    val path: String,
    val hasGaplessAlternative: Boolean = false,
    val db: String? = null
) {
    val name: String get() = defaultName
    val isGapless: Boolean get() = !db.isNullOrEmpty()
    val databaseName: String? get() = db?.let { if (it.endsWith(".db")) it else "$it.db" }
}

@Serializable
data class TranslationList(
    @SerialName("data") val translations: List<Translation> = emptyList()
)

@Serializable
data class Translation(
    val id: Int,
    @SerialName("minimum_version") val minimumVersion: Int = 1,
    @SerialName("current_version") val currentVersion: Int = 1,
    val displayName: String = "",
    val downloadType: String = "translation",
    val fileName: String = "",
    val fileUrl: String = "",
    val saveTo: String = "databases",
    val languageCode: String? = "en",
    val translator: String? = null,
    @SerialName("translator_foreign") val translatorForeign: String? = null
) {
    val displayTranslator: String
        get() = translatorForeign?.takeIf { it.isNotBlank() }
            ?: translator?.takeIf { it.isNotBlank() }
            ?: displayName
}

@Serializable
data class TranslationItem(
    val translation: Translation,
    val localVersion: Int = -1,
    val displayOrder: Int = -1,
    val isActive: Boolean = false
) {
    val exists: Boolean get() = localVersion > 0
    val isDownloaded: Boolean get() = localVersion > 0
    val needsUpgrade: Boolean get() = exists && translation.currentVersion > localVersion
}

@Serializable
data class SearchResultItem(
    val sura: Int,
    val ayah: Int,
    val page: Int,
    val text: String,
    val sourceTitle: String,
    val isArabic: Boolean
) {
    val sourceName: String get() = sourceTitle
    val matchedText: String get() = text
}

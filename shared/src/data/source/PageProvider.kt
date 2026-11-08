package com.jetbrains.kmpapp.data.source

import com.jetbrains.kmpapp.data.model.MushafType
import com.jetbrains.kmpapp.data.model.Qari

interface PageProvider {
    val mushafType: MushafType
    val dataSource: QuranDataSource
    val widthParam: String
    val imageVersion: Int
    val imagesBaseUrl: String
    val imagesZipBaseUrl: String
    val ayahInfoBaseUrl: String
    val databasesBaseUrl: String
    val audioDatabasesBaseUrl: String
    val arabicDatabaseName: String
    val arabicDatabaseZipUrl: String
    val directoryPrefix: String
    val qaris: List<Qari>
    val defaultQariId: Int

    val zipFileUrl: String
        get() = "${imagesZipBaseUrl}images$widthParam.zip"

    val ayahInfoFileName: String
        get() = "ayahinfo$widthParam.db"

    val ayahInfoZipUrl: String
        get() = "${ayahInfoBaseUrl}ayahinfo$widthParam.zip"

    fun pageImageUrl(page: Int): String {
        val pageStr = page.toString().padStart(3, '0')
        return "${imagesBaseUrl}width$widthParam/page$pageStr.png"
    }

    fun getPreviewTitle(): String = mushafType.defaultTitle
    fun getPreviewDescription(): String = mushafType.defaultDescription
    fun getImageWidthParam(): String = widthParam.removePrefix("_")
}

object DefaultQariCatalog {
    val qaris: List<Qari> get() = hafsQaris
    val hafsQaris: List<Qari> = listOf(
        Qari(
            id = 0,
            nameKey = "qari_minshawi_murattal_gapless",
            defaultName = "Minshawi Murattal",
            url = "https://download.quranicaudio.com/quran/muhammad_siddeeq_al-minshaawee/",
            path = "minshawi_murattal",
            hasGaplessAlternative = false,
            db = "minshawi_murattal"
        ),
        Qari(
            id = 1,
            nameKey = "qari_husary_gapless",
            defaultName = "Mahmoud Khalil Al-Husary",
            url = "https://download.quranicaudio.com/quran/mahmood_khaleel_al-husaree/",
            path = "husary",
            hasGaplessAlternative = false,
            db = "husary"
        ),
        Qari(
            id = 2,
            nameKey = "qari_afasy_gapless",
            defaultName = "Mishary Rashid Al-Afasy",
            url = "https://download.quranicaudio.com/quran/mishaari_raashid_al_3afaasee/",
            path = "mishari_alafasy",
            hasGaplessAlternative = false,
            db = "mishari_alafasy"
        ),
        Qari(
            id = 3,
            nameKey = "qari_sudais_gapless",
            defaultName = "Abdurrahman As-Sudais",
            url = "https://download.quranicaudio.com/quran/abdurrahmaan_as-sudays/",
            path = "abdurrahmaan_as-sudays",
            hasGaplessAlternative = false,
            db = "abdurrahmaan_as-sudays"
        ),
        Qari(
            id = 4,
            nameKey = "qari_shuraym_gapless",
            defaultName = "Saood Ash-Shuraym",
            url = "https://download.quranicaudio.com/quran/sa3ood_al-shuraym/",
            path = "sa3ood_al-shuraym",
            hasGaplessAlternative = false,
            db = "sa3ood_al-shuraym"
        ),
        Qari(
            id = 5,
            nameKey = "qari_shatri_gapless",
            defaultName = "Abu Bakr Ash-Shatri",
            url = "https://download.quranicaudio.com/quran/abu_bakr_ash-shaatree/",
            path = "abu_bakr_ash-shaatree",
            hasGaplessAlternative = false,
            db = "abu_bakr_ash-shaatree"
        ),
        Qari(
            id = 6,
            nameKey = "qari_saad_al_ghamidi_gapless",
            defaultName = "Saad Al-Ghamdi",
            url = "https://download.quranicaudio.com/quran/sa3d_al-ghaamidi/complete/",
            path = "sa3d_al-ghaamidi",
            hasGaplessAlternative = false,
            db = "sa3d_al-ghaamidi"
        ),
        Qari(
            id = 7,
            nameKey = "qari_muaiqly_gapless",
            defaultName = "Maher Al Muaiqly",
            url = "https://download.quranicaudio.com/quran/maher_256/",
            path = "maher_al_muaiqly",
            hasGaplessAlternative = false,
            db = "maher_al_muaiqly"
        ),
        Qari(
            id = 8,
            nameKey = "qari_yasser_dussary_gapless",
            defaultName = "Yasser Ad-Dussary",
            url = "https://download.quranicaudio.com/quran/yasser_ad-dussary/",
            path = "yasser_ad-dussary",
            hasGaplessAlternative = false,
            db = "yasser_ad-dussary"
        ),
        Qari(
            id = 9,
            nameKey = "qari_ayyoub_gapless",
            defaultName = "Muhammad Ayyoub",
            url = "https://download.quranicaudio.com/quran/muhammad_ayyoob/",
            path = "muhammad_ayyoob",
            hasGaplessAlternative = false,
            db = "muhammad_ayyoob"
        ),
        Qari(
            id = 10,
            nameKey = "qari_hudhayfi_gapless",
            defaultName = "Ali Al-Hudhaify",
            url = "https://download.quranicaudio.com/quran/ali_al-hudhaify/",
            path = "ali_al-hudhaify",
            hasGaplessAlternative = false,
            db = "ali_al-hudhaify"
        ),
        Qari(
            id = 11,
            nameKey = "qari_abdulbaset_gapless",
            defaultName = "Abd Al-Basit",
            url = "https://download.quranicaudio.com/quran/abdul_basit_murattal/",
            path = "abdul_basit_murattal",
            hasGaplessAlternative = false,
            db = "abdul_basit_murattal"
        ),
        Qari(
            id = 12,
            nameKey = "qari_afasy",
            defaultName = "Mishary Al-Afasy (Ayah-by-Ayah Gapped)",
            url = "https://mirrors.quranicaudio.com/everyayah/Alafasy_128kbps/",
            path = "afasy_gapped",
            hasGaplessAlternative = true,
            db = null
        ),
        Qari(
            id = 13,
            nameKey = "qari_husary_mujawwad",
            defaultName = "Husary (Ayah-by-Ayah Gapped)",
            url = "https://mirrors.quranicaudio.com/everyayah/Husary_128kbps/",
            path = "husary_gapped",
            hasGaplessAlternative = true,
            db = null
        ),
        Qari(
            id = 14,
            nameKey = "qari_basfar",
            defaultName = "Abdullah Basfar (Ayah-by-Ayah Gapped)",
            url = "https://mirrors.quranicaudio.com/everyayah/Abdullah_Basfar_192kbps/",
            path = "basfar_gapped",
            hasGaplessAlternative = false,
            db = null
        ),
        Qari(
            id = 15,
            nameKey = "qari_abdulbaset",
            defaultName = "Abd Al-Basit (Ayah-by-Ayah Gapped)",
            url = "https://mirrors.quranicaudio.com/everyayah/Abdul_Basit_Murattal_192kbps/",
            path = "abdulbaset_gapped",
            hasGaplessAlternative = true,
            db = null
        )
    )
}

class MadaniPageProvider : PageProvider {
    override val mushafType = MushafType.MADANI
    override val dataSource: QuranDataSource = MadaniDataSource()
    override val widthParam = "_1024"
    override val imageVersion = 8
    private val baseUrl = "https://android.quran.com/data"
    override val imagesBaseUrl = "$baseUrl/"
    override val imagesZipBaseUrl = "$baseUrl/zips/"
    override val ayahInfoBaseUrl = "$baseUrl/databases/ayahinfo/"
    override val databasesBaseUrl = "$baseUrl/databases/"
    override val audioDatabasesBaseUrl = "$baseUrl/databases/audio/"
    override val arabicDatabaseName = "quran.ar.uthmani.v2.db"
    override val arabicDatabaseZipUrl = "${databasesBaseUrl}quran.ar.uthmani.v2.db.zip"
    override val directoryPrefix = "madani"
    override val qaris: List<Qari> = DefaultQariCatalog.hafsQaris
    override val defaultQariId: Int = 2 // Mishary Al-Afasy
}

class TajweedPageProvider : PageProvider {
    override val mushafType = MushafType.TAJWEED
    override val dataSource: QuranDataSource = TajweedDataSource()
    override val widthParam = "_1280"
    override val imageVersion = 2
    private val baseUrl = "https://android.quran.com/data/tajweed"
    override val imagesBaseUrl = "$baseUrl/"
    override val imagesZipBaseUrl = "$baseUrl/zips/"
    override val ayahInfoBaseUrl = "$baseUrl/databases/ayahinfo/"
    override val databasesBaseUrl = "https://android.quran.com/data/databases/"
    override val audioDatabasesBaseUrl = "https://android.quran.com/data/databases/audio/"
    override val arabicDatabaseName = "quran.ar.uthmani.v2.db"
    override val arabicDatabaseZipUrl = "${databasesBaseUrl}quran.ar.uthmani.v2.db.zip"
    override val directoryPrefix = "tajweed"
    override val qaris: List<Qari> = DefaultQariCatalog.hafsQaris
    override val defaultQariId: Int = 2
}

class WarshPageProvider : PageProvider {
    override val mushafType = MushafType.WARSH
    override val dataSource: QuranDataSource = WarshDataSource()
    override val widthParam = "_1188"
    override val imageVersion = 2
    private val baseUrl = "https://android.quran.com/data/warsh/v2"
    override val imagesBaseUrl = "$baseUrl/"
    override val imagesZipBaseUrl = "$baseUrl/zips/"
    override val ayahInfoBaseUrl = "$baseUrl/databases/ayahinfo/"
    override val databasesBaseUrl = "https://android.quran.com/data/databases/"
    override val audioDatabasesBaseUrl = "https://android.quran.com/data/databases/audio/"
    override val arabicDatabaseName = "quran.ar.warsh.db"
    override val arabicDatabaseZipUrl = "${databasesBaseUrl}quran.ar.warsh.db.zip"
    override val directoryPrefix = "warsh"
    override val qaris: List<Qari> = DefaultQariCatalog.hafsQaris
    override val defaultQariId: Int = 1 // Husary
}

class QaloonPageProvider : PageProvider {
    override val mushafType = MushafType.QALOON
    override val dataSource: QuranDataSource = QaloonDataSource()
    override val widthParam = "_1152"
    override val imageVersion = 2
    private val baseUrl = "https://android.quran.com/data/qaloon/v2"
    override val imagesBaseUrl = "$baseUrl/"
    override val imagesZipBaseUrl = "$baseUrl/zips/"
    override val ayahInfoBaseUrl = "$baseUrl/databases/ayahinfo/"
    override val databasesBaseUrl = "https://android.quran.com/data/databases/"
    override val audioDatabasesBaseUrl = "https://android.quran.com/data/databases/audio/"
    override val arabicDatabaseName = "quran.ar.uthmani.v2.db"
    override val arabicDatabaseZipUrl = "${databasesBaseUrl}quran.ar.uthmani.v2.db.zip"
    override val directoryPrefix = "qaloon"
    override val qaris: List<Qari> = DefaultQariCatalog.hafsQaris
    override val defaultQariId: Int = 1
}

class NaskhPageProvider : PageProvider {
    override val mushafType = MushafType.NASKH
    override val dataSource: QuranDataSource = NaskhDataSource()
    override val widthParam = "_1152"
    override val imageVersion = 2
    private val baseUrl = "https://android.quran.com/data/naskh"
    override val imagesBaseUrl = "$baseUrl/"
    override val imagesZipBaseUrl = "$baseUrl/zips/"
    override val ayahInfoBaseUrl = "$baseUrl/databases/ayahinfo/"
    override val databasesBaseUrl = "https://android.quran.com/data/databases/"
    override val audioDatabasesBaseUrl = "https://android.quran.com/data/databases/audio/"
    override val arabicDatabaseName = "quran.ar.uthmani.v2.db"
    override val arabicDatabaseZipUrl = "${databasesBaseUrl}quran.ar.uthmani.v2.db.zip"
    override val directoryPrefix = "naskh"
    override val qaris: List<Qari> = DefaultQariCatalog.hafsQaris
    override val defaultQariId: Int = 2
}

object PageProviderRegistry {
    private val providers: Map<MushafType, PageProvider> = mapOf(
        MushafType.MADANI to MadaniPageProvider(),
        MushafType.TAJWEED to TajweedPageProvider(),
        MushafType.WARSH to WarshPageProvider(),
        MushafType.QALOON to QaloonPageProvider(),
        MushafType.NASKH to NaskhPageProvider()
    )

    val allProviders: List<PageProvider> = providers.values.toList()

    fun getProvider(type: MushafType): PageProvider =
        providers[type] ?: providers.getValue(MushafType.MADANI)

    fun getProvider(typeId: String): PageProvider =
        getProvider(MushafType.fromId(typeId))
}

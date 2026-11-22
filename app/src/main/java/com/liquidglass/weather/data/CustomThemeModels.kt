package com.liquidglass.weather.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

val Context.weatherSettingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "auracast_weather_settings")

enum class ThemeMode(val title: String) {
    AUTO_WEATHER("От погоды"),
    CUSTOM("Свой стиль"),
    PURE_BLACK("Чёрный AMOLED"),
    PURE_WHITE("Чистый белый"),
    CRIMSON_RED("Красный"),
    LIQUID_OCEAN("Liquid Ocean"),
    MIDNIGHT_AURORA("Aurora"),
    MATERIAL_YOU("Material You")
}

enum class RefreshRateMode(val title: String, val targetHz: Float) {
    AUTO("Авто", 0f),
    HZ_60("60 Гц", 60f),
    HZ_90("90 Гц", 90f),
    HZ_120("120 Гц", 120f),
    MAX("Макс. Гц", 240f)
}

enum class SmoothnessMode(val title: String, val damping: Float, val stiffness: Float) {
    SNAPPY("Резкая", 0.85f, 900f),
    SILK_120("120 Гц шёлк", 0.78f, 380f),
    DREAMY("Мягкая", 0.72f, 220f)
}

enum class WeatherParticleOverride(val title: String) {
    AUTO("Авто"),
    CLEAR("Солнце"),
    CLOUDY("Облака"),
    RAIN("Дождь"),
    SNOW("Снег"),
    STORM("Гроза"),
    OFF("Выкл")
}

enum class TempUnit(val symbol: String) {
    CELSIUS("°C"),
    FAHRENHEIT("°F")
}

/**
 * Fully customizable color & glass profile that the user can edit from scratch
 * (e.g. completely Red, Pure Black, Pure White, Neon Green, etc.) and save by name.
 */
data class CustomStyleProfile(
    val id: String = "active_custom",
    val name: String = "Мой стиль",
    val bgBaseHex: Int = 0xFF060B18.toInt(),
    val accentHex: Int = 0xFF38BDF8.toInt(),
    val secondaryHex: Int = 0xFFC084FC.toInt(),
    val glassTintHex: Int = 0xFFFFFFFF.toInt(),
    val textPrimaryHex: Int = 0xFFFFFFFF.toInt(),
    val autoTextContrast: Boolean = true,
    val showAuroraOrbs: Boolean = true,
    val orbIntensity: Float = 0.58f,
    val glassOpacity: Float = 0.22f,
    val blurRadius: Float = 26f,
    val specularIntensity: Float = 0.90f,
    val chromaticEdge: Float = 0.75f,
    val borderWidthDp: Float = 1.25f,
    val cornerRadiusDp: Float = 26f
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("bgBaseHex", bgBaseHex)
        put("accentHex", accentHex)
        put("secondaryHex", secondaryHex)
        put("glassTintHex", glassTintHex)
        put("textPrimaryHex", textPrimaryHex)
        put("autoTextContrast", autoTextContrast)
        put("showAuroraOrbs", showAuroraOrbs)
        put("orbIntensity", orbIntensity.toDouble())
        put("glassOpacity", glassOpacity.toDouble())
        put("blurRadius", blurRadius.toDouble())
        put("specularIntensity", specularIntensity.toDouble())
        put("chromaticEdge", chromaticEdge.toDouble())
        put("borderWidthDp", borderWidthDp.toDouble())
        put("cornerRadiusDp", cornerRadiusDp.toDouble())
    }

    companion object {
        fun fromJson(o: JSONObject): CustomStyleProfile = CustomStyleProfile(
            id = o.optString("id", "active_custom"),
            name = o.optString("name", "Мой стиль"),
            bgBaseHex = o.optInt("bgBaseHex", 0xFF060B18.toInt()),
            accentHex = o.optInt("accentHex", 0xFF38BDF8.toInt()),
            secondaryHex = o.optInt("secondaryHex", 0xFFC084FC.toInt()),
            glassTintHex = o.optInt("glassTintHex", 0xFFFFFFFF.toInt()),
            textPrimaryHex = o.optInt("textPrimaryHex", 0xFFFFFFFF.toInt()),
            autoTextContrast = o.optBoolean("autoTextContrast", true),
            showAuroraOrbs = o.optBoolean("showAuroraOrbs", true),
            orbIntensity = o.optDouble("orbIntensity", 0.58).toFloat(),
            glassOpacity = o.optDouble("glassOpacity", 0.22).toFloat(),
            blurRadius = o.optDouble("blurRadius", 26.0).toFloat(),
            specularIntensity = o.optDouble("specularIntensity", 0.90).toFloat(),
            chromaticEdge = o.optDouble("chromaticEdge", 0.75).toFloat(),
            borderWidthDp = o.optDouble("borderWidthDp", 1.25).toFloat(),
            cornerRadiusDp = o.optDouble("cornerRadiusDp", 26.0).toFloat()
        )
    }
}

data class WeatherAppConfig(
    val themeMode: ThemeMode = ThemeMode.AUTO_WEATHER,
    val customStyle: CustomStyleProfile = CustomStyleProfile(),
    val refreshRateMode: RefreshRateMode = RefreshRateMode.HZ_120,
    val smoothness: SmoothnessMode = SmoothnessMode.SILK_120,
    val showFpsBadge: Boolean = false,
    val motionBlurEnabled: Boolean = true,
    val motionBlurIntensity: Float = 0.85f,
    val animatedBackground: Boolean = true,
    val animationSpeed: Float = 1.0f,
    val particleOverride: WeatherParticleOverride = WeatherParticleOverride.AUTO,
    val particleIntensity: Float = 0.85f,
    val tempUnit: TempUnit = TempUnit.CELSIUS,
    val customFontPath: String = "",
    val customFontName: String = "",
    val fontScale: Float = 1.0f,
    val hapticFeedback: Boolean = true
)

class WeatherSettingsRepository(private val context: Context) {

    private val savedThemesFile by lazy { File(context.filesDir, "saved_custom_themes.json") }

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val CUSTOM_STYLE_JSON = stringPreferencesKey("custom_style_json")
        val REFRESH_RATE = stringPreferencesKey("refresh_rate")
        val SMOOTHNESS = stringPreferencesKey("smoothness")
        val SHOW_FPS = booleanPreferencesKey("show_fps")
        val MOTION_BLUR = booleanPreferencesKey("motion_blur")
        val MOTION_BLUR_INT = floatPreferencesKey("motion_blur_int")
        val ANIM_BG = booleanPreferencesKey("anim_bg")
        val ANIM_SPEED = floatPreferencesKey("anim_speed")
        val PARTICLE_OVERRIDE = stringPreferencesKey("particle_override")
        val PARTICLE_INT = floatPreferencesKey("particle_int")
        val TEMP_UNIT = stringPreferencesKey("temp_unit")
        val FONT_PATH = stringPreferencesKey("font_path")
        val FONT_NAME = stringPreferencesKey("font_name")
        val FONT_SCALE = floatPreferencesKey("font_scale")
        val HAPTIC = booleanPreferencesKey("haptic")
    }

    val configFlow: Flow<WeatherAppConfig> = context.weatherSettingsDataStore.data.map { p ->
        val customProfile = p[Keys.CUSTOM_STYLE_JSON]?.let { raw ->
            runCatching { CustomStyleProfile.fromJson(JSONObject(raw)) }.getOrNull()
        } ?: CustomStyleProfile()

        WeatherAppConfig(
            themeMode = p[Keys.THEME_MODE]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.AUTO_WEATHER,
            customStyle = customProfile,
            refreshRateMode = p[Keys.REFRESH_RATE]?.let { runCatching { RefreshRateMode.valueOf(it) }.getOrNull() }
                ?: RefreshRateMode.HZ_120,
            smoothness = p[Keys.SMOOTHNESS]?.let { runCatching { SmoothnessMode.valueOf(it) }.getOrNull() }
                ?: SmoothnessMode.SILK_120,
            showFpsBadge = p[Keys.SHOW_FPS] ?: false,
            motionBlurEnabled = p[Keys.MOTION_BLUR] ?: true,
            motionBlurIntensity = p[Keys.MOTION_BLUR_INT] ?: 0.85f,
            animatedBackground = p[Keys.ANIM_BG] ?: true,
            animationSpeed = p[Keys.ANIM_SPEED] ?: 1.0f,
            particleOverride = p[Keys.PARTICLE_OVERRIDE]?.let { runCatching { WeatherParticleOverride.valueOf(it) }.getOrNull() }
                ?: WeatherParticleOverride.AUTO,
            particleIntensity = p[Keys.PARTICLE_INT] ?: 0.85f,
            tempUnit = p[Keys.TEMP_UNIT]?.let { runCatching { TempUnit.valueOf(it) }.getOrNull() }
                ?: TempUnit.CELSIUS,
            customFontPath = p[Keys.FONT_PATH] ?: "",
            customFontName = p[Keys.FONT_NAME] ?: "",
            fontScale = p[Keys.FONT_SCALE] ?: 1.0f,
            hapticFeedback = p[Keys.HAPTIC] ?: true
        )
    }

    suspend fun saveConfig(c: WeatherAppConfig) {
        context.weatherSettingsDataStore.edit { p ->
            p[Keys.THEME_MODE] = c.themeMode.name
            p[Keys.CUSTOM_STYLE_JSON] = c.customStyle.toJson().toString()
            p[Keys.REFRESH_RATE] = c.refreshRateMode.name
            p[Keys.SMOOTHNESS] = c.smoothness.name
            p[Keys.SHOW_FPS] = c.showFpsBadge
            p[Keys.MOTION_BLUR] = c.motionBlurEnabled
            p[Keys.MOTION_BLUR_INT] = c.motionBlurIntensity
            p[Keys.ANIM_BG] = c.animatedBackground
            p[Keys.ANIM_SPEED] = c.animationSpeed
            p[Keys.PARTICLE_OVERRIDE] = c.particleOverride.name
            p[Keys.PARTICLE_INT] = c.particleIntensity
            p[Keys.TEMP_UNIT] = c.tempUnit.name
            p[Keys.FONT_PATH] = c.customFontPath
            p[Keys.FONT_NAME] = c.customFontName
            p[Keys.FONT_SCALE] = c.fontScale
            p[Keys.HAPTIC] = c.hapticFeedback
        }
    }

    @Synchronized
    fun loadSavedThemes(): List<CustomStyleProfile> {
        if (!savedThemesFile.exists()) return emptyList()
        return runCatching {
            val arr = JSONArray(savedThemesFile.readText())
            (0 until arr.length()).map { CustomStyleProfile.fromJson(arr.getJSONObject(it)) }
        }.getOrDefault(emptyList())
    }

    @Synchronized
    fun saveCustomTheme(profile: CustomStyleProfile): List<CustomStyleProfile> {
        val list = loadSavedThemes().filterNot { it.id == profile.id }.toMutableList()
        list.add(0, profile)
        val arr = JSONArray()
        list.forEach { arr.put(it.toJson()) }
        runCatching { savedThemesFile.writeText(arr.toString()) }
        return list
    }

    @Synchronized
    fun deleteCustomTheme(id: String): List<CustomStyleProfile> {
        val list = loadSavedThemes().filterNot { it.id == id }
        val arr = JSONArray()
        list.forEach { arr.put(it.toJson()) }
        runCatching { savedThemesFile.writeText(arr.toString()) }
        return list
    }
}

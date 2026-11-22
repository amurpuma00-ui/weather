package com.liquidglass.weather.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SavedCity(
    val id: String,
    val name: String,
    val country: String,
    val lat: Double,
    val lon: Double
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("country", country)
        put("lat", lat)
        put("lon", lon)
    }

    companion object {
        fun fromJson(o: JSONObject): SavedCity = SavedCity(
            id = o.optString("id"),
            name = o.optString("name"),
            country = o.optString("country"),
            lat = o.optDouble("lat"),
            lon = o.optDouble("lon")
        )
    }
}

data class HourlyForecastPoint(
    val hourLabel: String,
    val tempC: Double,
    val precipProb: Int,
    val windKmh: Double,
    val weatherCode: Int
)

data class DailyForecastPoint(
    val dayLabel: String,
    val dateLabel: String,
    val minTempC: Double,
    val maxTempC: Double,
    val precipProb: Int,
    val weatherCode: Int
)

data class WeatherData(
    val city: SavedCity,
    val tempC: Double,
    val feelsLikeC: Double,
    val humidity: Int,
    val windKmh: Double,
    val pressureHpa: Double,
    val uvIndex: Double,
    val precipProbNow: Int,
    val weatherCode: Int,
    val isDay: Boolean,
    val sunrise: String,
    val sunset: String,
    val hourly: List<HourlyForecastPoint>,
    val daily: List<DailyForecastPoint>,
    val updatedAt: String,
    val isLive: Boolean
)

enum class WeatherConditionKind {
    CLEAR, CLOUDY, RAIN, SNOW, STORM
}

object WeatherCodeMapper {
    fun description(code: Int): String = when (code) {
        0 -> "Ясно"
        1 -> "Преимущественно ясно"
        2 -> "Переменная облачность"
        3 -> "Пасмурно"
        45, 48 -> "Туман"
        51, 53, 55 -> "Морось"
        61, 63, 65 -> "Дождь"
        66, 67 -> "Ледяной дождь"
        71, 73, 75, 77 -> "Снег"
        80, 81, 82 -> "Ливень"
        85, 86 -> "Снегопад"
        95, 96, 99 -> "Гроза"
        else -> "Облачно"
    }

    fun kind(code: Int): WeatherConditionKind = when (code) {
        0, 1 -> WeatherConditionKind.CLEAR
        2, 3, 45, 48 -> WeatherConditionKind.CLOUDY
        51, 53, 55, 61, 63, 65, 66, 67, 80, 81, 82 -> WeatherConditionKind.RAIN
        71, 73, 75, 77, 85, 86 -> WeatherConditionKind.SNOW
        95, 96, 99 -> WeatherConditionKind.STORM
        else -> WeatherConditionKind.CLOUDY
    }
}

class WeatherRepository(private val context: Context) {

    private val citiesFile by lazy { File(context.filesDir, "saved_cities.json") }
    private val prefs by lazy { context.getSharedPreferences("weather_cache", Context.MODE_PRIVATE) }

    companion object {
        val DEFAULT_CITIES = listOf(
            SavedCity("moscow", "Москва", "Россия", 55.7558, 37.6173),
            SavedCity("spb", "Санкт-Петербург", "Россия", 59.9343, 30.3351),
            SavedCity("ekb", "Екатеринбург", "Россия", 56.8389, 60.6057),
            SavedCity("sochi", "Сочи", "Россия", 43.5855, 39.7231),
            SavedCity("tokyo", "Токио", "Япония", 35.6762, 139.6503)
        )
    }

    fun loadCities(): List<SavedCity> {
        if (!citiesFile.exists()) return DEFAULT_CITIES
        return runCatching {
            val arr = JSONArray(citiesFile.readText())
            val list = (0 until arr.length()).map { SavedCity.fromJson(arr.getJSONObject(it)) }
            list.ifEmpty { DEFAULT_CITIES }
        }.getOrDefault(DEFAULT_CITIES)
    }

    fun saveCities(cities: List<SavedCity>) {
        val arr = JSONArray()
        cities.forEach { arr.put(it.toJson()) }
        runCatching { citiesFile.writeText(arr.toString()) }
    }

    suspend fun searchCities(query: String): List<SavedCity> = withContext(Dispatchers.IO) {
        if (query.trim().length < 2) return@withContext emptyList()
        runCatching {
            val encoded = URLEncoder.encode(query.trim(), "UTF-8")
            val url = URL("https://geocoding-api.open-meteo.com/v1/search?name=$encoded&count=10&language=ru&format=json")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 7000
                readTimeout = 7000
            }
            if (conn.responseCode != HttpURLConnection.HTTP_OK) return@withContext emptyList()
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val results = JSONObject(body).optJSONArray("results") ?: return@withContext emptyList()
            (0 until results.length()).map { i ->
                val item = results.getJSONObject(i)
                val lat = item.getDouble("latitude")
                val lon = item.getDouble("longitude")
                SavedCity(
                    id = "${lat}_${lon}",
                    name = item.optString("name", query),
                    country = item.optString("country", item.optString("admin1", "")),
                    lat = lat,
                    lon = lon
                )
            }
        }.getOrDefault(emptyList())
    }

    suspend fun fetchWeather(city: SavedCity): WeatherData = withContext(Dispatchers.IO) {
        val urlStr = "https://api.open-meteo.com/v1/forecast" +
            "?latitude=${city.lat}&longitude=${city.lon}" +
            "&current=temperature_2m,relative_humidity_2m,apparent_temperature,is_day,weather_code,surface_pressure,wind_speed_10m" +
            "&hourly=temperature_2m,precipitation_probability,weather_code,wind_speed_10m,uv_index" +
            "&daily=weather_code,temperature_2m_max,temperature_2m_min,sunrise,sunset,precipitation_probability_max,uv_index_max" +
            "&timezone=auto&forecast_days=10"

        try {
            val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 8000
            }
            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                val raw = conn.inputStream.bufferedReader().use { it.readText() }
                val updated = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                prefs.edit()
                    .putString("cache_${city.id}", raw)
                    .putString("cache_time_${city.id}", updated)
                    .apply()
                return@withContext parseWeatherJson(city, JSONObject(raw), updated, isLive = true)
            }
        } catch (_: Exception) {
        }

        // Fallback to cached JSON or offline sample
        val cachedRaw = prefs.getString("cache_${city.id}", null)
        val cachedTime = prefs.getString("cache_time_${city.id}", "кэш") ?: "кэш"
        if (cachedRaw != null) {
            runCatching {
                return@withContext parseWeatherJson(city, JSONObject(cachedRaw), "$cachedTime (офлайн)", isLive = false)
            }
        }
        buildFallbackWeather(city)
    }

    private fun parseWeatherJson(city: SavedCity, root: JSONObject, updatedAt: String, isLive: Boolean): WeatherData {
        val current = root.getJSONObject("current")
        val hourlyObj = root.getJSONObject("hourly")
        val dailyObj = root.getJSONObject("daily")

        val hTimes = hourlyObj.getJSONArray("time")
        val hTemps = hourlyObj.getJSONArray("temperature_2m")
        val hPrecips = hourlyObj.optJSONArray("precipitation_probability")
        val hWinds = hourlyObj.optJSONArray("wind_speed_10m")
        val hCodes = hourlyObj.getJSONArray("weather_code")
        val hUvs = hourlyObj.optJSONArray("uv_index")

        val hourlyList = mutableListOf<HourlyForecastPoint>()
        val countH = minOf(24, hTimes.length())
        for (i in 0 until countH) {
            val tStr = hTimes.getString(i).substringAfter("T", "00:00").take(5)
            hourlyList.add(
                HourlyForecastPoint(
                    hourLabel = tStr,
                    tempC = hTemps.getDouble(i),
                    precipProb = hPrecips?.optInt(i, 0) ?: 0,
                    windKmh = hWinds?.optDouble(i, 0.0) ?: 0.0,
                    weatherCode = hCodes.getInt(i)
                )
            )
        }

        val dTimes = dailyObj.getJSONArray("time")
        val dMax = dailyObj.getJSONArray("temperature_2m_max")
        val dMin = dailyObj.getJSONArray("temperature_2m_min")
        val dCodes = dailyObj.getJSONArray("weather_code")
        val dPrecips = dailyObj.optJSONArray("precipitation_probability_max")
        val dSunrises = dailyObj.optJSONArray("sunrise")
        val dSunsets = dailyObj.optJSONArray("sunset")
        val dUvs = dailyObj.optJSONArray("uv_index_max")

        val inFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val dayFmt = SimpleDateFormat("EEE", Locale("ru"))
        val dateFmt = SimpleDateFormat("d MMM", Locale("ru"))

        val dailyList = mutableListOf<DailyForecastPoint>()
        for (i in 0 until minOf(10, dTimes.length())) {
            val dateStr = dTimes.getString(i)
            val parsedDate = runCatching { inFmt.parse(dateStr) }.getOrNull()
            val dayLabel = if (i == 0) "Сегодня" else {
                parsedDate?.let { dayFmt.format(it).replaceFirstChar { c -> c.uppercase() } } ?: dateStr
            }
            val shortDate = parsedDate?.let { dateFmt.format(it) } ?: ""
            dailyList.add(
                DailyForecastPoint(
                    dayLabel = dayLabel,
                    dateLabel = shortDate,
                    minTempC = dMin.getDouble(i),
                    maxTempC = dMax.getDouble(i),
                    precipProb = dPrecips?.optInt(i, 0) ?: 0,
                    weatherCode = dCodes.getInt(i)
                )
            )
        }

        val sunrise = dSunrises?.optString(0, "")?.substringAfter("T", "07:00")?.take(5) ?: "07:00"
        val sunset = dSunsets?.optString(0, "")?.substringAfter("T", "18:00")?.take(5) ?: "18:00"
        val uvNow = hUvs?.optDouble(0, dUvs?.optDouble(0, 1.0) ?: 1.0) ?: 1.0

        return WeatherData(
            city = city,
            tempC = current.getDouble("temperature_2m"),
            feelsLikeC = current.getDouble("apparent_temperature"),
            humidity = current.getInt("relative_humidity_2m"),
            windKmh = current.getDouble("wind_speed_10m"),
            pressureHpa = current.getDouble("surface_pressure"),
            uvIndex = uvNow,
            precipProbNow = hourlyList.firstOrNull()?.precipProb ?: 0,
            weatherCode = current.getInt("weather_code"),
            isDay = current.optInt("is_day", 1) == 1,
            sunrise = sunrise,
            sunset = sunset,
            hourly = hourlyList,
            daily = dailyList,
            updatedAt = updatedAt,
            isLive = isLive
        )
    }

    private fun buildFallbackWeather(city: SavedCity): WeatherData {
        val hourly = (0 until 24).map { h ->
            HourlyForecastPoint(
                hourLabel = String.format(Locale.US, "%02d:00", h),
                tempC = -2.0 + kotlin.math.sin(h / 4.0) * 4.5,
                precipProb = if (h in 10..16) 45 else 15,
                windKmh = 12.0 + (h % 5) * 1.8,
                weatherCode = if (h in 10..16) 71 else 2
            )
        }
        val days = listOf("Сегодня", "Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс", "Пн", "Вт").mapIndexed { i, d ->
            DailyForecastPoint(d, "${22 + i} ноя", -5.0 + (i % 3), 2.0 + (i % 4), 20 + (i * 7) % 50, if (i % 3 == 0) 71 else 2)
        }
        return WeatherData(
            city = city,
            tempC = -1.0,
            feelsLikeC = -4.5,
            humidity = 78,
            windKmh = 14.0,
            pressureHpa = 1014.0,
            uvIndex = 1.2,
            precipProbNow = 35,
            weatherCode = 71,
            isDay = false,
            sunrise = "08:15",
            sunset = "16:10",
            hourly = hourly,
            daily = days,
            updatedAt = "Демо-данные (офлайн)",
            isLive = false
        )
    }
}

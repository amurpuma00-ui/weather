package com.liquidglass.weather.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liquidglass.weather.data.*
import com.liquidglass.weather.ui.components.WeatherGlassButton
import com.liquidglass.weather.ui.components.WeatherGlassPanel
import com.liquidglass.weather.ui.theme.ResolvedWeatherStyle
import com.liquidglass.weather.viewmodel.WeatherUiState
import kotlin.math.roundToInt

fun formatTemp(tempC: Double, unit: TempUnit): String {
    val v = if (unit == TempUnit.CELSIUS) tempC else tempC * 9.0 / 5.0 + 32.0
    return "${v.roundToInt()}°"
}

fun weatherIconForCode(code: Int, isDay: Boolean = true): ImageVector = when (WeatherCodeMapper.kind(code)) {
    WeatherConditionKind.CLEAR -> if (isDay) Icons.Rounded.WbSunny else Icons.Rounded.NightsStay
    WeatherConditionKind.CLOUDY -> Icons.Rounded.Cloud
    WeatherConditionKind.RAIN -> Icons.Rounded.WaterDrop
    WeatherConditionKind.SNOW -> Icons.Rounded.AcUnit
    WeatherConditionKind.STORM -> Icons.Rounded.Thunderstorm
}

@Composable
fun WeatherNowScreen(
    state: WeatherUiState,
    style: ResolvedWeatherStyle,
    config: WeatherAppConfig,
    onRefresh: () -> Unit,
    onOpenCities: () -> Unit
) {
    val w = state.weather
    val scale = config.fontScale

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // City Header + Refresh
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            WeatherGlassButton(
                style = style,
                config = config,
                onClick = onOpenCities,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 9.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Rounded.LocationOn, contentDescription = null, tint = style.accent, modifier = Modifier.size(18.dp))
                    Text(
                        text = state.selectedCity.name,
                        color = style.textPrimary,
                        fontSize = (16f * scale).sp,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null, tint = style.textSecondary, modifier = Modifier.size(18.dp))
                }
            }

            WeatherGlassButton(
                style = style,
                config = config,
                onClick = onRefresh,
                contentPadding = PaddingValues(10.dp)
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = style.accent, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Rounded.Refresh, contentDescription = "Обновить", tint = style.textPrimary, modifier = Modifier.size(20.dp))
                }
            }
        }

        if (w == null) {
            Box(modifier = Modifier.fillMaxWidth().height(260.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = style.accent)
            }
            return@Column
        }

        // Hero Liquid Glass Temperature Card
        WeatherGlassPanel(
            style = style,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 22.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = if (w.isLive) "● LIVE • ${w.updatedAt}" else "○ ${w.updatedAt}",
                    color = if (w.isLive) style.accent else style.textSecondary,
                    fontSize = (11f * scale).sp,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Icon(
                        imageVector = weatherIconForCode(w.weatherCode, w.isDay),
                        contentDescription = null,
                        tint = style.accent,
                        modifier = Modifier.size(58.dp)
                    )
                    Text(
                        text = formatTemp(w.tempC, config.tempUnit),
                        color = style.textPrimary,
                        fontSize = (74f * scale).sp,
                        fontWeight = FontWeight.ExtraLight
                    )
                }

                Text(
                    text = WeatherCodeMapper.description(w.weatherCode),
                    color = style.textPrimary,
                    fontSize = (19f * scale).sp,
                    fontWeight = FontWeight.SemiBold
                )

                val today = w.daily.firstOrNull()
                val rangeStr = if (today != null) {
                    "Макс ${formatTemp(today.maxTempC, config.tempUnit)} • Мин ${formatTemp(today.minTempC, config.tempUnit)}"
                } else ""

                Text(
                    text = "Ощущается как ${formatTemp(w.feelsLikeC, config.tempUnit)}  •  $rangeStr",
                    color = style.textSecondary,
                    fontSize = (13f * scale).sp
                )
            }
        }

        // 24-Hour Horizontal Glass Strip
        WeatherGlassPanel(
            style = style,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 14.dp, horizontal = 12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Почасовой прогноз (24ч)",
                    color = style.textSecondary,
                    fontSize = (12f * scale).sp,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    w.hourly.take(24).forEachIndexed { idx, h ->
                        WeatherGlassPanel(
                            style = style,
                            shape = RoundedCornerShape(18.dp),
                            tintOverride = if (idx == 0) style.accent else style.glassTint,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = if (idx == 0) "Сейчас" else h.hourLabel,
                                    color = if (idx == 0) style.textPrimary else style.textSecondary,
                                    fontSize = (11f * scale).sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Icon(
                                    imageVector = weatherIconForCode(h.weatherCode, true),
                                    contentDescription = null,
                                    tint = if (idx == 0) style.textPrimary else style.accent,
                                    modifier = Modifier.size(22.dp)
                                )
                                Text(
                                    text = formatTemp(h.tempC, config.tempUnit),
                                    color = style.textPrimary,
                                    fontSize = (15f * scale).sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${h.precipProb}%",
                                    color = style.secondary,
                                    fontSize = (10f * scale).sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }

        // Detailed Glass Metrics Grid (3 rows x 2 cols)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            WeatherMetricTile(
                icon = Icons.Rounded.Air,
                label = "Ветер",
                value = "${w.windKmh.roundToInt()} км/ч",
                sub = "Порывы умеренные",
                style = style,
                scale = scale,
                modifier = Modifier.weight(1f)
            )
            WeatherMetricTile(
                icon = Icons.Rounded.WaterDrop,
                label = "Влажность",
                value = "${w.humidity}%",
                sub = "Осадки: ${w.precipProbNow}%",
                style = style,
                scale = scale,
                modifier = Modifier.weight(1f)
            )
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            WeatherMetricTile(
                icon = Icons.Rounded.Speed,
                label = "Давление",
                value = "${(w.pressureHpa * 0.75006).roundToInt()} мм",
                sub = "${w.pressureHpa.roundToInt()} гПа",
                style = style,
                scale = scale,
                modifier = Modifier.weight(1f)
            )
            WeatherMetricTile(
                icon = Icons.Rounded.WbSunny,
                label = "УФ-индекс",
                value = String.format(java.util.Locale.US, "%.1f", w.uvIndex),
                sub = when {
                    w.uvIndex < 3 -> "Низкий"
                    w.uvIndex < 6 -> "Умеренный"
                    else -> "Высокий"
                },
                style = style,
                scale = scale,
                modifier = Modifier.weight(1f)
            )
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            WeatherMetricTile(
                icon = Icons.Rounded.WbTwilight,
                label = "Восход",
                value = w.sunrise,
                sub = "Световой день",
                style = style,
                scale = scale,
                modifier = Modifier.weight(1f)
            )
            WeatherMetricTile(
                icon = Icons.Rounded.NightsStay,
                label = "Закат",
                value = w.sunset,
                sub = "Заход солнца",
                style = style,
                scale = scale,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun WeatherMetricTile(
    icon: ImageVector,
    label: String,
    value: String,
    sub: String,
    style: ResolvedWeatherStyle,
    scale: Float,
    modifier: Modifier = Modifier
) {
    WeatherGlassPanel(
        style = style,
        contentPadding = PaddingValues(14.dp),
        modifier = modifier
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(icon, contentDescription = null, tint = style.accent, modifier = Modifier.size(16.dp))
                Text(label, color = style.textSecondary, fontSize = (12f * scale).sp, fontWeight = FontWeight.Medium)
            }
            Text(value, color = style.textPrimary, fontSize = (22f * scale).sp, fontWeight = FontWeight.Bold)
            Text(sub, color = style.textSecondary, fontSize = (11f * scale).sp)
        }
    }
}

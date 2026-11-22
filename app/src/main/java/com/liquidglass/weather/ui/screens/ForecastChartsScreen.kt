package com.liquidglass.weather.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liquidglass.weather.data.HourlyForecastPoint
import com.liquidglass.weather.data.WeatherAppConfig
import com.liquidglass.weather.data.WeatherData
import com.liquidglass.weather.ui.components.WeatherGlassPanel
import com.liquidglass.weather.ui.theme.ResolvedWeatherStyle

@Composable
fun ForecastChartsScreen(
    weather: WeatherData?,
    style: ResolvedWeatherStyle,
    config: WeatherAppConfig
) {
    val scale = config.fontScale

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Графики и 10 дней", color = style.textPrimary, fontSize = (24f * scale).sp, fontWeight = FontWeight.Bold)
        if (weather == null) return@Column

        // 24-Hour Temperature Curve Chart
        WeatherGlassPanel(style = style, modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Кривая температуры (24 часа)", color = style.textPrimary, fontSize = (15f * scale).sp, fontWeight = FontWeight.Bold)
                TemperatureCurveCanvas(
                    hourly = weather.hourly.take(18),
                    style = style,
                    modifier = Modifier.fillMaxWidth().height(160.dp)
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    val sample = weather.hourly.take(18).filterIndexed { i, _ -> i % 4 == 0 }
                    sample.forEach { pt ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(formatTemp(pt.tempC, config.tempUnit), color = style.accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(pt.hourLabel, color = style.textSecondary, fontSize = 10.sp)
                        }
                    }
                }
            }
        }

        // Precipitation Probability Bar Chart
        WeatherGlassPanel(style = style, modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Вероятность осадков (%)", color = style.textPrimary, fontSize = (15f * scale).sp, fontWeight = FontWeight.Bold)
                PrecipBarsCanvas(
                    hourly = weather.hourly.take(16),
                    style = style,
                    modifier = Modifier.fillMaxWidth().height(110.dp)
                )
            }
        }

        // 10-Day Forecast List
        WeatherGlassPanel(style = style, modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Прогноз на 10 дней", color = style.textPrimary, fontSize = (15f * scale).sp, fontWeight = FontWeight.Bold)

                val globalMin = weather.daily.minOfOrNull { it.minTempC } ?: -10.0
                val globalMax = weather.daily.maxOfOrNull { it.maxTempC } ?: 20.0
                val span = (globalMax - globalMin).coerceAtLeast(1.0)

                weather.daily.forEach { d ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(modifier = Modifier.width(68.dp)) {
                            Text(d.dayLabel, color = style.textPrimary, fontWeight = FontWeight.Bold, fontSize = (13f * scale).sp)
                            Text(d.dateLabel, color = style.textSecondary, fontSize = 10.sp)
                        }

                        Icon(
                            imageVector = weatherIconForCode(d.weatherCode, true),
                            contentDescription = null,
                            tint = style.accent,
                            modifier = Modifier.size(22.dp)
                        )

                        Text(
                            text = formatTemp(d.minTempC, config.tempUnit),
                            color = style.textSecondary,
                            fontSize = (13f * scale).sp,
                            modifier = Modifier.width(34.dp)
                        )

                        // Gradient temperature range bar
                        val startFrac = ((d.minTempC - globalMin) / span).toFloat().coerceIn(0f, 0.9f)
                        val widthFrac = ((d.maxTempC - d.minTempC) / span).toFloat().coerceIn(0.1f, 1f - startFrac)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(8.dp)
                                .clip(RoundedCornerShape(50))
                                .background(style.textSecondary.copy(alpha = 0.2f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(startFrac + widthFrac)
                                    .padding(start = (startFrac * 90f).dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(Brush.horizontalGradient(listOf(style.accent, style.secondary)))
                            )
                        }

                        Text(
                            text = formatTemp(d.maxTempC, config.tempUnit),
                            color = style.textPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = (13f * scale).sp,
                            modifier = Modifier.width(34.dp)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))
    }
}

@Composable
private fun TemperatureCurveCanvas(
    hourly: List<HourlyForecastPoint>,
    style: ResolvedWeatherStyle,
    modifier: Modifier = Modifier
) {
    if (hourly.size < 2) return
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val padTop = 16f
        val padBot = 12f
        val chartH = (h - padTop - padBot).coerceAtLeast(10f)

        val minT = hourly.minOf { it.tempC } - 1.5
        val maxT = hourly.maxOf { it.tempC } + 1.5
        val range = (maxT - minT).coerceAtLeast(2.0)

        for (i in 0..3) {
            val y = padTop + chartH * (i / 3f)
            drawLine(style.textSecondary.copy(alpha = 0.14f), Offset(0f, y), Offset(w, y), 1.2f)
        }

        val stepX = w / (hourly.size - 1)
        val pts = hourly.mapIndexed { idx, p ->
            val x = idx * stepX
            val y = padTop + chartH * (1f - ((p.tempC - minT) / range).toFloat().coerceIn(0f, 1f))
            Offset(x, y)
        }

        val line = Path().apply {
            moveTo(pts.first().x, pts.first().y)
            for (i in 0 until pts.lastIndex) {
                val p0 = pts[i]
                val p1 = pts[i + 1]
                val cx = (p0.x + p1.x) / 2f
                cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
            }
        }

        val fill = Path().apply {
            addPath(line)
            lineTo(pts.last().x, padTop + chartH)
            lineTo(pts.first().x, padTop + chartH)
            close()
        }

        drawPath(
            path = fill,
            brush = Brush.verticalGradient(
                listOf(style.accent.copy(alpha = 0.45f), style.secondary.copy(alpha = 0.10f), Color.Transparent),
                startY = padTop,
                endY = padTop + chartH
            )
        )
        drawPath(
            path = line,
            brush = Brush.horizontalGradient(listOf(style.accent, style.secondary)),
            style = Stroke(width = 5f, cap = StrokeCap.Round)
        )
        pts.forEachIndexed { idx, pt ->
            if (idx % 3 == 0) {
                drawCircle(style.accent, 6f, pt)
                drawCircle(Color.White, 3f, pt)
            }
        }
    }
}

@Composable
private fun PrecipBarsCanvas(
    hourly: List<HourlyForecastPoint>,
    style: ResolvedWeatherStyle,
    modifier: Modifier = Modifier
) {
    if (hourly.isEmpty()) return
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val count = hourly.size
        val gap = (w / (count * 2.8f)).coerceAtLeast(4f)
        val barW = ((w - gap * (count - 1)) / count).coerceAtLeast(5f)

        hourly.forEachIndexed { i, p ->
            val ratio = (p.precipProb / 100f).coerceIn(0f, 1f)
            val barH = (h * 0.9f * ratio).coerceAtLeast(6f)
            val x = i * (barW + gap)
            drawRoundRect(
                brush = Brush.verticalGradient(listOf(style.secondary, style.accent)),
                topLeft = Offset(x, h - barH),
                size = Size(barW, barH),
                cornerRadius = CornerRadius(barW / 2f, barW / 2f)
            )
        }
    }
}

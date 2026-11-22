package com.liquidglass.weather.ui.components

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.liquidglass.weather.data.WeatherAppConfig
import com.liquidglass.weather.data.WeatherConditionKind
import com.liquidglass.weather.data.WeatherParticleOverride
import com.liquidglass.weather.ui.theme.ResolvedWeatherStyle
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

fun Modifier.weatherMotionBlur(radiusX: Float, radiusY: Float = 0f): Modifier = this.graphicsLayer {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && (radiusX > 0.4f || radiusY > 0.4f)) {
        renderEffect = RenderEffect.createBlurEffect(
            radiusX.coerceAtLeast(0.01f),
            radiusY.coerceAtLeast(0.01f),
            Shader.TileMode.DECAL
        ).asComposeRenderEffect()
    }
}

@Composable
fun LivingWeatherBackdrop(
    style: ResolvedWeatherStyle,
    config: WeatherAppConfig,
    actualCondition: WeatherConditionKind,
    modifier: Modifier = Modifier
) {
    val dur = (15000f / config.animationSpeed.coerceIn(0.2f, 3.0f)).toInt().coerceAtLeast(3000)
    val inf = rememberInfiniteTransition(label = "weather_bg")

    val phase by if (config.animatedBackground) {
        inf.animateFloat(
            initialValue = 0f,
            targetValue = (2 * PI).toFloat(),
            animationSpec = infiniteRepeatable(tween(dur, easing = LinearEasing), RepeatMode.Restart),
            label = "orb_phase"
        )
    } else {
        remember { mutableFloatStateOf(0.9f) }
    }

    val particleTick by if (config.particleOverride != WeatherParticleOverride.OFF) {
        inf.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Restart),
            label = "particles"
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }

    val activeParticleKind = when (config.particleOverride) {
        WeatherParticleOverride.AUTO -> actualCondition
        WeatherParticleOverride.CLEAR -> WeatherConditionKind.CLEAR
        WeatherParticleOverride.CLOUDY -> WeatherConditionKind.CLOUDY
        WeatherParticleOverride.RAIN -> WeatherConditionKind.RAIN
        WeatherParticleOverride.SNOW -> WeatherConditionKind.SNOW
        WeatherParticleOverride.STORM -> WeatherConditionKind.STORM
        WeatherParticleOverride.OFF -> null
    }

    val orbBlur = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && style.blurRadius > 1f) {
        Modifier.graphicsLayer {
            val r = (style.blurRadius * 1.5f).coerceIn(1f, 80f)
            renderEffect = RenderEffect.createBlurEffect(r, r, Shader.TileMode.CLAMP).asComposeRenderEffect()
        }
    } else Modifier

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(style.bgBase)
    ) {
        // 1. Optional Fluid Aurora Orbs (disabled in Pure Black / Pure White or by user toggle)
        if (style.showOrbs && style.orbIntensity > 0.02f) {
            Canvas(modifier = Modifier.fillMaxSize().then(orbBlur)) {
                val w = size.width
                val h = size.height
                val a = style.orbIntensity.coerceIn(0.05f, 0.95f)

                val c1 = Offset(w * (0.22f + 0.24f * cos(phase)), h * (0.18f + 0.14f * sin(phase * 1.2f)))
                val c2 = Offset(w * (0.78f + 0.20f * sin(phase * 0.9f)), h * (0.50f + 0.20f * cos(phase * 1.1f)))
                val c3 = Offset(w * (0.45f + 0.26f * cos(phase * 0.7f + 1.5f)), h * (0.82f + 0.14f * sin(phase * 0.8f)))

                drawCircle(Brush.radialGradient(listOf(style.orb1.copy(alpha = a), Color.Transparent), c1, w * 0.68f), w * 0.68f, c1)
                drawCircle(Brush.radialGradient(listOf(style.orb2.copy(alpha = a), Color.Transparent), c2, w * 0.72f), w * 0.72f, c2)
                drawCircle(Brush.radialGradient(listOf(style.orb3.copy(alpha = a * 0.85f), Color.Transparent), c3, w * 0.65f), w * 0.65f, c3)
            }
        }

        // 2. Living Weather Particles Layer (Rain, Snow, Sun Flare, Clouds, Storm)
        if (activeParticleKind != null && config.particleIntensity > 0.05f) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val pInt = config.particleIntensity.coerceIn(0.1f, 1.5f)
                val particleColor = if (style.isLightBg) style.accent else Color.White

                when (activeParticleKind) {
                    WeatherConditionKind.RAIN, WeatherConditionKind.STORM -> {
                        val drops = (55 * pInt).toInt().coerceIn(20, 110)
                        for (i in 0 until drops) {
                            val seedX = ((i * 73.13f) % 100f) / 100f
                            val seedY = ((i * 37.71f) % 100f) / 100f
                            val speed = 1f + (i % 3) * 0.45f
                            val yFrac = (seedY + particleTick * speed) % 1f
                            val x = w * seedX + yFrac * 36f
                            val y = h * yFrac
                            val len = 24f + (i % 4) * 10f
                            drawLine(
                                color = particleColor.copy(alpha = (0.18f + (i % 3) * 0.12f) * pInt.coerceAtMost(1f)),
                                start = Offset(x, y),
                                end = Offset(x + 6f, y + len),
                                strokeWidth = 2.2f,
                                cap = StrokeCap.Round
                            )
                        }
                        if (activeParticleKind == WeatherConditionKind.STORM && particleTick in 0.86f..0.90f) {
                            drawRect(Color.White.copy(alpha = 0.16f))
                        }
                    }
                    WeatherConditionKind.SNOW -> {
                        val flakes = (50 * pInt).toInt().coerceIn(20, 95)
                        for (i in 0 until flakes) {
                            val seedX = ((i * 61.3f) % 100f) / 100f
                            val seedY = ((i * 29.7f) % 100f) / 100f
                            val yFrac = (seedY + particleTick * (0.45f + (i % 3) * 0.18f)) % 1f
                            val sway = sin(particleTick * 2 * PI + i).toFloat() * 24f
                            val x = w * seedX + sway
                            val y = h * yFrac
                            val r = 2.5f + (i % 4) * 1.4f
                            drawCircle(
                                color = particleColor.copy(alpha = 0.42f * pInt.coerceAtMost(1f)),
                                radius = r,
                                center = Offset(x, y)
                            )
                        }
                    }
                    WeatherConditionKind.CLEAR -> {
                        val sunCenter = Offset(w * 0.84f, h * 0.12f)
                        val pulse = 1f + 0.08f * sin(particleTick * 2 * PI).toFloat()
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(style.accent.copy(alpha = 0.35f * pInt.coerceAtMost(1f)), Color.Transparent),
                                center = sunCenter,
                                radius = w * 0.45f * pulse
                            ),
                            radius = w * 0.45f * pulse,
                            center = sunCenter
                        )
                    }
                    WeatherConditionKind.CLOUDY -> {
                        for (i in 0..3) {
                            val drift = ((i * 0.28f + particleTick * 0.25f) % 1.3f) - 0.15f
                            val cx = w * drift
                            val cy = h * (0.14f + i * 0.12f)
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(particleColor.copy(alpha = 0.12f * pInt.coerceAtMost(1f)), Color.Transparent),
                                    center = Offset(cx, cy),
                                    radius = w * 0.32f
                                ),
                                radius = w * 0.32f,
                                center = Offset(cx, cy)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WeatherGlassPanel(
    style: ResolvedWeatherStyle,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(style.cornerRadiusDp.dp),
    tintOverride: Color? = null,
    contentPadding: PaddingValues = PaddingValues(15.dp),
    content: @Composable BoxScope.() -> Unit
) {
    val tint = tintOverride ?: style.glassTint
    val op = style.glassOpacity.coerceIn(0.04f, 0.78f)
    val spec = style.specularIntensity.coerceIn(0f, 1f)
    val chroma = style.chromaticEdge.coerceIn(0f, 1f)

    val fillBrush = Brush.linearGradient(
        colors = listOf(
            tint.copy(alpha = (op * 1.35f).coerceAtMost(0.90f)),
            tint.copy(alpha = op * 0.55f),
            style.accent.copy(alpha = op * 0.22f * chroma),
            tint.copy(alpha = (op * 1.05f).coerceAtMost(0.75f))
        ),
        start = Offset(0f, 0f),
        end = Offset(800f, 1000f)
    )

    val rimBase = if (style.isLightBg) Color.Black else Color.White
    val borderBrush = Brush.linearGradient(
        colors = listOf(
            rimBase.copy(alpha = (if (style.isLightBg) 0.24f else 0.78f) * spec),
            style.accent.copy(alpha = 0.52f * chroma * spec),
            rimBase.copy(alpha = 0.10f * spec),
            style.secondary.copy(alpha = 0.48f * chroma * spec),
            rimBase.copy(alpha = (if (style.isLightBg) 0.18f else 0.40f) * spec)
        ),
        start = Offset(0f, 0f),
        end = Offset(600f, 800f)
    )

    Box(
        modifier = modifier
            .clip(shape)
            .background(fillBrush)
            .then(if (style.borderWidthDp > 0.1f) Modifier.border(style.borderWidthDp.dp, borderBrush, shape) else Modifier)
            .padding(contentPadding),
        content = content
    )
}

@Composable
fun WeatherGlassButton(
    style: ResolvedWeatherStyle,
    config: WeatherAppConfig,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(50),
    accentFill: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
    content: @Composable BoxScope.() -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val haptic = LocalHapticFeedback.current

    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.91f else 1f,
        animationSpec = spring(
            dampingRatio = config.smoothness.damping,
            stiffness = config.smoothness.stiffness
        ),
        label = "btn_scale"
    )
    val blur by animateFloatAsState(
        targetValue = if (pressed && config.motionBlurEnabled) 5f * config.motionBlurIntensity else 0f,
        animationSpec = tween(75),
        label = "btn_blur"
    )

    WeatherGlassPanel(
        style = style,
        shape = shape,
        tintOverride = if (accentFill) style.accent else style.glassTint,
        contentPadding = contentPadding,
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .weatherMotionBlur(blur, blur * 0.6f)
            .clickable(interactionSource = interaction, indication = null) {
                if (config.hapticFeedback) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            },
        content = content
    )
}

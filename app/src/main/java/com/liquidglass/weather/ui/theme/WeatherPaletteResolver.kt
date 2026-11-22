package com.liquidglass.weather.ui.theme

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.Typography
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import com.liquidglass.weather.data.*

data class ResolvedWeatherStyle(
    val bgBase: Color,
    val orb1: Color,
    val orb2: Color,
    val orb3: Color,
    val glassTint: Color,
    val accent: Color,
    val secondary: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val showOrbs: Boolean,
    val orbIntensity: Float,
    val glassOpacity: Float,
    val blurRadius: Float,
    val specularIntensity: Float,
    val chromaticEdge: Float,
    val borderWidthDp: Float,
    val cornerRadiusDp: Float,
    val isLightBg: Boolean
)

@Composable
fun rememberResolvedWeatherStyle(
    config: WeatherAppConfig,
    conditionKind: WeatherConditionKind,
    isDay: Boolean
): ResolvedWeatherStyle {
    val context = LocalContext.current
    val cs = config.customStyle

    val target = remember(config.themeMode, cs, conditionKind, isDay) {
        when (config.themeMode) {
            ThemeMode.CUSTOM -> {
                val bg = Color(cs.bgBaseHex)
                val isLight = bg.luminance() > 0.52f
                val textMain = if (cs.autoTextContrast) {
                    if (isLight) Color(0xFF0A0F1A) else Color(0xFFFFFFFF)
                } else {
                    Color(cs.textPrimaryHex)
                }
                val textSub = textMain.copy(alpha = 0.72f)
                ResolvedWeatherStyle(
                    bgBase = bg,
                    orb1 = Color(cs.accentHex),
                    orb2 = Color(cs.secondaryHex),
                    orb3 = Color(cs.accentHex).copy(alpha = 0.75f),
                    glassTint = Color(cs.glassTintHex),
                    accent = Color(cs.accentHex),
                    secondary = Color(cs.secondaryHex),
                    textPrimary = textMain,
                    textSecondary = textSub,
                    showOrbs = cs.showAuroraOrbs,
                    orbIntensity = cs.orbIntensity,
                    glassOpacity = cs.glassOpacity,
                    blurRadius = cs.blurRadius,
                    specularIntensity = cs.specularIntensity,
                    chromaticEdge = cs.chromaticEdge,
                    borderWidthDp = cs.borderWidthDp,
                    cornerRadiusDp = cs.cornerRadiusDp,
                    isLightBg = isLight
                )
            }
            ThemeMode.PURE_BLACK -> ResolvedWeatherStyle(
                bgBase = Color(0xFF000000),
                orb1 = Color(0xFF1A1A1A),
                orb2 = Color(0xFF111111),
                orb3 = Color(0xFF0A0A0A),
                glassTint = Color(0xFFFFFFFF),
                accent = Color(0xFFFFFFFF),
                secondary = Color(0xFF94A3B8),
                textPrimary = Color(0xFFFFFFFF),
                textSecondary = Color(0xFFA3A3A3),
                showOrbs = false,
                orbIntensity = 0f,
                glassOpacity = cs.glassOpacity.coerceIn(0.06f, 0.25f),
                blurRadius = cs.blurRadius,
                specularIntensity = cs.specularIntensity,
                chromaticEdge = 0.15f,
                borderWidthDp = cs.borderWidthDp,
                cornerRadiusDp = cs.cornerRadiusDp,
                isLightBg = false
            )
            ThemeMode.PURE_WHITE -> ResolvedWeatherStyle(
                bgBase = Color(0xFFFFFFFF),
                orb1 = Color(0xFFE2E8F0),
                orb2 = Color(0xFFF1F5F9),
                orb3 = Color(0xFFCBD5E1),
                glassTint = Color(0xFF0F172A),
                accent = Color(0xFF111827),
                secondary = Color(0xFF3B82F6),
                textPrimary = Color(0xFF09090B),
                textSecondary = Color(0xFF52525B),
                showOrbs = false,
                orbIntensity = 0f,
                glassOpacity = cs.glassOpacity.coerceIn(0.05f, 0.18f),
                blurRadius = cs.blurRadius,
                specularIntensity = cs.specularIntensity,
                chromaticEdge = 0.25f,
                borderWidthDp = cs.borderWidthDp,
                cornerRadiusDp = cs.cornerRadiusDp,
                isLightBg = true
            )
            ThemeMode.CRIMSON_RED -> ResolvedWeatherStyle(
                bgBase = Color(0xFF1A0206),
                orb1 = Color(0xFFEF4444),
                orb2 = Color(0xFF991B1B),
                orb3 = Color(0xFFFF1E56),
                glassTint = Color(0xFFFFE4E6),
                accent = Color(0xFFFF2E4C),
                secondary = Color(0xFFFF758F),
                textPrimary = Color(0xFFFFFFFF),
                textSecondary = Color(0xCCFECDD3),
                showOrbs = cs.showAuroraOrbs,
                orbIntensity = cs.orbIntensity,
                glassOpacity = cs.glassOpacity,
                blurRadius = cs.blurRadius,
                specularIntensity = cs.specularIntensity,
                chromaticEdge = cs.chromaticEdge,
                borderWidthDp = cs.borderWidthDp,
                cornerRadiusDp = cs.cornerRadiusDp,
                isLightBg = false
            )
            ThemeMode.LIQUID_OCEAN -> ResolvedWeatherStyle(
                bgBase = Color(0xFF040A18),
                orb1 = Color(0xFF00C2FF),
                orb2 = Color(0xFF3A5BFF),
                orb3 = Color(0xFF7B2CBF),
                glassTint = Color(0xFFFFFFFF),
                accent = Color(0xFF38BDF8),
                secondary = Color(0xFFC084FC),
                textPrimary = Color(0xFFFFFFFF),
                textSecondary = Color(0xBFE2E8F0),
                showOrbs = cs.showAuroraOrbs,
                orbIntensity = cs.orbIntensity,
                glassOpacity = cs.glassOpacity,
                blurRadius = cs.blurRadius,
                specularIntensity = cs.specularIntensity,
                chromaticEdge = cs.chromaticEdge,
                borderWidthDp = cs.borderWidthDp,
                cornerRadiusDp = cs.cornerRadiusDp,
                isLightBg = false
            )
            ThemeMode.MIDNIGHT_AURORA -> ResolvedWeatherStyle(
                bgBase = Color(0xFF040712),
                orb1 = Color(0xFF00F5D4),
                orb2 = Color(0xFF9B5DE5),
                orb3 = Color(0xFFF15BB5),
                glassTint = Color(0xFFE0F2FE),
                accent = Color(0xFF00F5D4),
                secondary = Color(0xFFF15BB5),
                textPrimary = Color(0xFFFFFFFF),
                textSecondary = Color(0xB8CBD5E1),
                showOrbs = cs.showAuroraOrbs,
                orbIntensity = cs.orbIntensity,
                glassOpacity = cs.glassOpacity,
                blurRadius = cs.blurRadius,
                specularIntensity = cs.specularIntensity,
                chromaticEdge = cs.chromaticEdge,
                borderWidthDp = cs.borderWidthDp,
                cornerRadiusDp = cs.cornerRadiusDp,
                isLightBg = false
            )
            ThemeMode.MATERIAL_YOU -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val d = dynamicDarkColorScheme(context)
                    ResolvedWeatherStyle(
                        bgBase = d.background,
                        orb1 = d.primary,
                        orb2 = d.tertiary,
                        orb3 = d.secondary,
                        glassTint = d.surfaceVariant,
                        accent = d.primary,
                        secondary = d.tertiary,
                        textPrimary = d.onBackground,
                        textSecondary = d.onSurfaceVariant,
                        showOrbs = cs.showAuroraOrbs,
                        orbIntensity = cs.orbIntensity,
                        glassOpacity = cs.glassOpacity,
                        blurRadius = cs.blurRadius,
                        specularIntensity = cs.specularIntensity,
                        chromaticEdge = cs.chromaticEdge,
                        borderWidthDp = cs.borderWidthDp,
                        cornerRadiusDp = cs.cornerRadiusDp,
                        isLightBg = false
                    )
                } else {
                    ResolvedWeatherStyle(
                        bgBase = Color(0xFF040A18),
                        orb1 = Color(0xFF00C2FF),
                        orb2 = Color(0xFF3A5BFF),
                        orb3 = Color(0xFF7B2CBF),
                        glassTint = Color(0xFFFFFFFF),
                        accent = Color(0xFF38BDF8),
                        secondary = Color(0xFFC084FC),
                        textPrimary = Color(0xFFFFFFFF),
                        textSecondary = Color(0xBFE2E8F0),
                        showOrbs = cs.showAuroraOrbs,
                        orbIntensity = cs.orbIntensity,
                        glassOpacity = cs.glassOpacity,
                        blurRadius = cs.blurRadius,
                        specularIntensity = cs.specularIntensity,
                        chromaticEdge = cs.chromaticEdge,
                        borderWidthDp = cs.borderWidthDp,
                        cornerRadiusDp = cs.cornerRadiusDp,
                        isLightBg = false
                    )
                }
            }
            ThemeMode.AUTO_WEATHER -> {
                val (bg, o1, o2, o3, acc, sec) = when (conditionKind) {
                    WeatherConditionKind.CLEAR -> if (isDay) {
                        Sextuple(Color(0xFF0B2545), Color(0xFF38BDF8), Color(0xFFFBBF24), Color(0xFF2563EB), Color(0xFFFBBF24), Color(0xFF38BDF8))
                    } else {
                        Sextuple(Color(0xFF050816), Color(0xFF3B82F6), Color(0xFF6366F1), Color(0xFF1E1B4B), Color(0xFF60A5FA), Color(0xFFA78BFA))
                    }
                    WeatherConditionKind.CLOUDY -> Sextuple(Color(0xFF0F172A), Color(0xFF475569), Color(0xFF38BDF8), Color(0xFF334155), Color(0xFF94A3B8), Color(0xFF38BDF8))
                    WeatherConditionKind.RAIN -> Sextuple(Color(0xFF061121), Color(0xFF0284C7), Color(0xFF1D4ED8), Color(0xFF0EA5E9), Color(0xFF38BDF8), Color(0xFF60A5FA))
                    WeatherConditionKind.SNOW -> Sextuple(Color(0xFF0C1829), Color(0xFFBAE6FD), Color(0xFF7DD3FC), Color(0xFF93C5FD), Color(0xFFE0F2FE), Color(0xFF7DD3FC))
                    WeatherConditionKind.STORM -> Sextuple(Color(0xFF0B0618), Color(0xFF7C3AED), Color(0xFF4F46E5), Color(0xFFF59E0B), Color(0xFFC084FC), Color(0xFFFBBF24))
                }
                ResolvedWeatherStyle(
                    bgBase = bg,
                    orb1 = o1,
                    orb2 = o2,
                    orb3 = o3,
                    glassTint = Color(0xFFFFFFFF),
                    accent = acc,
                    secondary = sec,
                    textPrimary = Color(0xFFFFFFFF),
                    textSecondary = Color(0xC2E2E8F0),
                    showOrbs = cs.showAuroraOrbs,
                    orbIntensity = cs.orbIntensity,
                    glassOpacity = cs.glassOpacity,
                    blurRadius = cs.blurRadius,
                    specularIntensity = cs.specularIntensity,
                    chromaticEdge = cs.chromaticEdge,
                    borderWidthDp = cs.borderWidthDp,
                    cornerRadiusDp = cs.cornerRadiusDp,
                    isLightBg = false
                )
            }
        }
    }

    val spec = tween<Color>(550)
    val bgBase by animateColorAsState(target.bgBase, spec, label = "bg")
    val orb1 by animateColorAsState(target.orb1, spec, label = "o1")
    val orb2 by animateColorAsState(target.orb2, spec, label = "o2")
    val orb3 by animateColorAsState(target.orb3, spec, label = "o3")
    val accent by animateColorAsState(target.accent, spec, label = "acc")
    val secondary by animateColorAsState(target.secondary, spec, label = "sec")
    val textPrimary by animateColorAsState(target.textPrimary, spec, label = "tp")
    val textSecondary by animateColorAsState(target.textSecondary, spec, label = "ts")

    return target.copy(
        bgBase = bgBase,
        orb1 = orb1,
        orb2 = orb2,
        orb3 = orb3,
        accent = accent,
        secondary = secondary,
        textPrimary = textPrimary,
        textSecondary = textSecondary
    )
}

private data class Sextuple<T>(val a: T, val b: T, val c: T, val d: T, val e: T, val f: T)

fun buildWeatherTypography(fontFamily: FontFamily): Typography {
    val b = Typography()
    return Typography(
        displayLarge = b.displayLarge.copy(fontFamily = fontFamily),
        displayMedium = b.displayMedium.copy(fontFamily = fontFamily),
        displaySmall = b.displaySmall.copy(fontFamily = fontFamily),
        headlineLarge = b.headlineLarge.copy(fontFamily = fontFamily),
        headlineMedium = b.headlineMedium.copy(fontFamily = fontFamily),
        headlineSmall = b.headlineSmall.copy(fontFamily = fontFamily),
        titleLarge = b.titleLarge.copy(fontFamily = fontFamily),
        titleMedium = b.titleMedium.copy(fontFamily = fontFamily),
        titleSmall = b.titleSmall.copy(fontFamily = fontFamily),
        bodyLarge = b.bodyLarge.copy(fontFamily = fontFamily),
        bodyMedium = b.bodyMedium.copy(fontFamily = fontFamily),
        bodySmall = b.bodySmall.copy(fontFamily = fontFamily),
        labelLarge = b.labelLarge.copy(fontFamily = fontFamily),
        labelMedium = b.labelMedium.copy(fontFamily = fontFamily),
        labelSmall = b.labelSmall.copy(fontFamily = fontFamily)
    )
}

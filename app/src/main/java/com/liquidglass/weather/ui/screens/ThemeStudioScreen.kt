package com.liquidglass.weather.ui.screens

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liquidglass.weather.data.*
import com.liquidglass.weather.ui.components.WeatherGlassButton
import com.liquidglass.weather.ui.components.WeatherGlassPanel
import com.liquidglass.weather.ui.theme.ResolvedWeatherStyle
import java.util.Locale

private enum class ColorSlot(val title: String) {
    BACKGROUND("Фон"),
    ACCENT("Акцент"),
    SECONDARY("Призма"),
    GLASS_TINT("Стекло"),
    TEXT("Текст")
}

@Composable
fun ThemeStudioScreen(
    config: WeatherAppConfig,
    style: ResolvedWeatherStyle,
    savedThemes: List<CustomStyleProfile>,
    supportedHz: List<Int>,
    activeHz: Int,
    onUpdateConfig: ((WeatherAppConfig) -> WeatherAppConfig) -> Unit,
    onUpdateCustomStyle: ((CustomStyleProfile) -> CustomStyleProfile) -> Unit,
    onSaveTheme: (String) -> Unit,
    onApplySavedTheme: (CustomStyleProfile) -> Unit,
    onDeleteSavedTheme: (String) -> Unit,
    onReset: () -> Unit,
    onPickCustomFont: () -> Unit
) {
    var editingSlot by remember { mutableStateOf(ColorSlot.BACKGROUND) }
    var showSaveDialog by remember { mutableStateOf(false) }

    val cs = config.customStyle

    fun currentSlotColor(): Int = when (editingSlot) {
        ColorSlot.BACKGROUND -> cs.bgBaseHex
        ColorSlot.ACCENT -> cs.accentHex
        ColorSlot.SECONDARY -> cs.secondaryHex
        ColorSlot.GLASS_TINT -> cs.glassTintHex
        ColorSlot.TEXT -> cs.textPrimaryHex
    }

    fun applySlotColor(colorInt: Int) {
        onUpdateCustomStyle { cur ->
            when (editingSlot) {
                ColorSlot.BACKGROUND -> cur.copy(bgBaseHex = colorInt)
                ColorSlot.ACCENT -> cur.copy(accentHex = colorInt)
                ColorSlot.SECONDARY -> cur.copy(secondaryHex = colorInt)
                ColorSlot.GLASS_TINT -> cur.copy(glassTintHex = colorInt)
                ColorSlot.TEXT -> cur.copy(textPrimaryHex = colorInt, autoTextContrast = false)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Студия стиля", color = style.textPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text("Создай любой цвет: красный, чёрный, белый и др.", color = style.textSecondary, fontSize = 12.sp)
            }
            TextButton(onClick = onReset) {
                Text("Сброс", color = style.accent, fontWeight = FontWeight.Bold)
            }
        }

        // --- 1. Theme Mode Selector (Quick Bases + Custom DIY) ---
        WeatherGlassPanel(style = style, modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Режим темы", color = style.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ThemeMode.entries.forEach { mode ->
                        StudioChip(
                            label = mode.title,
                            selected = config.themeMode == mode,
                            style = style,
                            onClick = { onUpdateConfig { it.copy(themeMode = mode) } }
                        )
                    }
                }

                // One-tap DIY Full Style Templates (fills Custom Constructor so user can tweak further!)
                Text("Быстрый старт для «Своего стиля»", color = style.textSecondary, fontSize = 12.sp)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickTemplateChip("Полностью чёрный", Color.Black, Color.White, style) {
                        onUpdateCustomStyle {
                            it.copy(
                                bgBaseHex = 0xFF000000.toInt(),
                                accentHex = 0xFFFFFFFF.toInt(),
                                secondaryHex = 0xFF94A3B8.toInt(),
                                glassTintHex = 0xFFFFFFFF.toInt(),
                                autoTextContrast = true,
                                showAuroraOrbs = false,
                                glassOpacity = 0.12f,
                                chromaticEdge = 0.15f
                            )
                        }
                    }
                    QuickTemplateChip("Полностью белый", Color.White, Color.Black, style) {
                        onUpdateCustomStyle {
                            it.copy(
                                bgBaseHex = 0xFFFFFFFF.toInt(),
                                accentHex = 0xFF111827.toInt(),
                                secondaryHex = 0xFF3B82F6.toInt(),
                                glassTintHex = 0xFF0F172A.toInt(),
                                autoTextContrast = true,
                                showAuroraOrbs = false,
                                glassOpacity = 0.08f,
                                chromaticEdge = 0.25f
                            )
                        }
                    }
                    QuickTemplateChip("Тотальный красный", Color(0xFFB91C1C), Color.White, style) {
                        onUpdateCustomStyle {
                            it.copy(
                                bgBaseHex = 0xFF45030B.toInt(),
                                accentHex = 0xFFFF1E42.toInt(),
                                secondaryHex = 0xFFFF6B81.toInt(),
                                glassTintHex = 0xFFFFCCD5.toInt(),
                                autoTextContrast = true,
                                showAuroraOrbs = true,
                                orbIntensity = 0.65f,
                                glassOpacity = 0.20f,
                                chromaticEdge = 0.85f
                            )
                        }
                    }
                    QuickTemplateChip("Кибер-зелёный", Color(0xFF052E16), Color(0xFF22C55E), style) {
                        onUpdateCustomStyle {
                            it.copy(
                                bgBaseHex = 0xFF021A0B.toInt(),
                                accentHex = 0xFF00FF85.toInt(),
                                secondaryHex = 0xFF10B981.toInt(),
                                glassTintHex = 0xFFD1FAE5.toInt(),
                                autoTextContrast = true,
                                showAuroraOrbs = true,
                                glassOpacity = 0.18f
                            )
                        }
                    }
                    QuickTemplateChip("Золотой люкс", Color(0xFF1C1404), Color(0xFFFBBF24), style) {
                        onUpdateCustomStyle {
                            it.copy(
                                bgBaseHex = 0xFF120C02.toInt(),
                                accentHex = 0xFFFBBF24.toInt(),
                                secondaryHex = 0xFFF59E0B.toInt(),
                                glassTintHex = 0xFFFEF3C7.toInt(),
                                autoTextContrast = true,
                                showAuroraOrbs = true,
                                glassOpacity = 0.18f
                            )
                        }
                    }
                }
            }
        }

        // --- 2. Full DIY Color Constructor (HSV + Swatches for every layer) ---
        WeatherGlassPanel(style = style, modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Rounded.Palette, contentDescription = null, tint = style.accent)
                        Text("Конструктор цветов", color = style.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                    WeatherGlassButton(
                        style = style,
                        config = config,
                        accentFill = true,
                        onClick = { showSaveDialog = true },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Сохранить стиль", color = if (style.isLightBg && style.accent == Color(0xFF111827)) Color.White else Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Which element we are coloring right now
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ColorSlot.entries.forEach { slot ->
                        StudioChip(
                            label = slot.title,
                            selected = editingSlot == slot,
                            style = style,
                            onClick = { editingSlot = slot }
                        )
                    }
                }

                // Quick pure color swatches (Black, White, Red, Crimson, Orange, Yellow, Green, Cyan, Blue, Purple, Pink)
                val quickColors = listOf(
                    0xFF000000.toInt(), 0xFF121212.toInt(), 0xFFFFFFFF.toInt(), 0xFFF1F5F9.toInt(),
                    0xFFDC2626.toInt(), 0xFF7F1D1D.toInt(), 0xFFFF1E56.toInt(), 0xFFF97316.toInt(),
                    0xFFFACC15.toInt(), 0xFF10B981.toInt(), 0xFF06B6D4.toInt(), 0xFF2563EB.toInt(),
                    0xFF7C3AED.toInt(), 0xFFEC4899.toInt()
                )
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    quickColors.forEach { cInt ->
                        val isSelected = currentSlotColor() == cInt
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(cInt))
                                .border(
                                    width = if (isSelected) 3.dp else 1.2.dp,
                                    color = if (isSelected) style.accent else style.textSecondary.copy(alpha = 0.5f),
                                    shape = CircleShape
                                )
                                .clickable { applySlotColor(cInt) }
                        )
                    }
                }

                // Full HSV Sliders (Hue + Saturation + Brightness) -> allows ANY color in existence
                HsvColorSliders(
                    colorInt = currentSlotColor(),
                    style = style,
                    onColorChange = { applySlotColor(it) }
                )

                StudioSwitch("Авто-контраст текста (для белого/чёрного фона)", cs.autoTextContrast, style) { v ->
                    onUpdateCustomStyle { it.copy(autoTextContrast = v) }
                }
                StudioSwitch("Светящиеся сферы на фоне (Aurora Orbs)", cs.showAuroraOrbs, style) { v ->
                    onUpdateCustomStyle { it.copy(showAuroraOrbs = v) }
                }
                if (cs.showAuroraOrbs) {
                    StudioSlider("Яркость сфер фона", "${(cs.orbIntensity * 100).toInt()}%", cs.orbIntensity, 0.05f..0.95f, style) { v ->
                        onUpdateCustomStyle { it.copy(orbIntensity = v) }
                    }
                }
            }
        }

        // --- 3. Saved Custom Themes ---
        if (savedThemes.isNotEmpty()) {
            WeatherGlassPanel(style = style, modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Мои сохранённые стили", color = style.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    savedThemes.forEach { theme ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onApplySavedTheme(theme) }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(Color(theme.bgBaseHex))
                                        .border(2.5.dp, Color(theme.accentHex), CircleShape)
                                )
                                Text(theme.name, color = style.textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            }
                            IconButton(onClick = { onDeleteSavedTheme(theme.id) }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Удалить", tint = style.textSecondary)
                            }
                        }
                    }
                }
            }
        }

        // --- 4. Liquid Glass Geometry & Weather Effects ---
        WeatherGlassPanel(style = style, modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Жидкое стекло и погодные эффекты", color = style.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)

                StudioSlider("Плотность стекла", "${(cs.glassOpacity * 100).toInt()}%", cs.glassOpacity, 0.04f..0.75f, style) { v ->
                    onUpdateConfig { it.copy(customStyle = it.customStyle.copy(glassOpacity = v)) }
                }
                StudioSlider("Глубина размытия (Blur)", "${cs.blurRadius.toInt()} dp", cs.blurRadius, 0f..50f, style) { v ->
                    onUpdateConfig { it.copy(customStyle = it.customStyle.copy(blurRadius = v)) }
                }
                StudioSlider("Зеркальные блики граней", "${(cs.specularIntensity * 100).toInt()}%", cs.specularIntensity, 0f..1f, style) { v ->
                    onUpdateConfig { it.copy(customStyle = it.customStyle.copy(specularIntensity = v)) }
                }
                StudioSlider("Хроматическая призма", "${(cs.chromaticEdge * 100).toInt()}%", cs.chromaticEdge, 0f..1f, style) { v ->
                    onUpdateConfig { it.copy(customStyle = it.customStyle.copy(chromaticEdge = v)) }
                }
                StudioSlider("Толщина рамки стекла", "${String.format(Locale.US, "%.1f", cs.borderWidthDp)} dp", cs.borderWidthDp, 0f..3.5f, style) { v ->
                    onUpdateConfig { it.copy(customStyle = it.customStyle.copy(borderWidthDp = v)) }
                }
                StudioSlider("Скругление карточек", "${cs.cornerRadiusDp.toInt()} dp", cs.cornerRadiusDp, 6f..40f, style) { v ->
                    onUpdateConfig { it.copy(customStyle = it.customStyle.copy(cornerRadiusDp = v)) }
                }

                HorizontalDivider(color = style.textSecondary.copy(alpha = 0.2f))

                Text("Погодные частицы (дождь / снег / солнце)", color = style.textSecondary, fontSize = 12.sp)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    WeatherParticleOverride.entries.forEach { po ->
                        StudioChip(po.title, config.particleOverride == po, style) {
                            onUpdateConfig { it.copy(particleOverride = po) }
                        }
                    }
                }
                if (config.particleOverride != WeatherParticleOverride.OFF) {
                    StudioSlider("Интенсивность частиц", "${(config.particleIntensity * 100).toInt()}%", config.particleIntensity, 0.2f..1.5f, style) { v ->
                        onUpdateConfig { it.copy(particleIntensity = v) }
                    }
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Единицы температуры", color = style.textPrimary, fontSize = 13.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TempUnit.entries.forEach { u ->
                            StudioChip(u.symbol, config.tempUnit == u, style) {
                                onUpdateConfig { it.copy(tempUnit = u) }
                            }
                        }
                    }
                }
            }
        }

        // --- 5. 120 Hz, Motion Blur & Custom Font ---
        WeatherGlassPanel(style = style, modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Rounded.Speed, contentDescription = null, tint = style.accent)
                        Text("120 Гц, Motion Blur и Шрифт", color = style.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                    Text("${activeHz} Гц", color = style.accent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                Text("Экран поддерживает: ${supportedHz.joinToString(", ") { "${it} Гц" }}", color = style.textSecondary, fontSize = 11.sp)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RefreshRateMode.entries.forEach { m ->
                        StudioChip(m.title, config.refreshRateMode == m, style) {
                            onUpdateConfig { it.copy(refreshRateMode = m) }
                        }
                    }
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SmoothnessMode.entries.forEach { sm ->
                        StudioChip(sm.title, config.smoothness == sm, style, Modifier.weight(1f)) {
                            onUpdateConfig { it.copy(smoothness = sm) }
                        }
                    }
                }

                StudioSwitch("Показывать индикатор Гц / FPS", config.showFpsBadge, style) { v ->
                    onUpdateConfig { it.copy(showFpsBadge = v) }
                }
                StudioSwitch("Motion Blur при свайпах и нажатиях", config.motionBlurEnabled, style) { v ->
                    onUpdateConfig { it.copy(motionBlurEnabled = v) }
                }
                if (config.motionBlurEnabled) {
                    StudioSlider("Сила Motion Blur", "${(config.motionBlurIntensity * 100).toInt()}%", config.motionBlurIntensity, 0.2f..1.6f, style) { v ->
                        onUpdateConfig { it.copy(motionBlurIntensity = v) }
                    }
                }
                StudioSwitch("Тактильный виброотклик", config.hapticFeedback, style) { v ->
                    onUpdateConfig { it.copy(hapticFeedback = v) }
                }

                HorizontalDivider(color = style.textSecondary.copy(alpha = 0.2f))

                WeatherGlassButton(
                    style = style,
                    config = config,
                    accentFill = true,
                    onClick = onPickCustomFont,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (config.customFontName.isNotBlank()) "Шрифт: ${config.customFontName} (сменить .ttf/.otf)" else "Загрузить свой шрифт (.ttf / .otf)",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                StudioSlider("Масштаб шрифта", "${(config.fontScale * 100).toInt()}%", config.fontScale, 0.85f..1.25f, style) { v ->
                    onUpdateConfig { it.copy(fontScale = v) }
                }
            }
        }

        Spacer(Modifier.height(14.dp))
    }

    if (showSaveDialog) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            containerColor = style.bgBase.copy(alpha = 0.96f),
            title = { Text("Сохранить свой стиль", color = style.textPrimary) },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("Например: Красный неон", color = style.textSecondary) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = style.textPrimary,
                        unfocusedTextColor = style.textPrimary,
                        focusedBorderColor = style.accent
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onSaveTheme(name)
                    showSaveDialog = false
                }) { Text("Сохранить", color = style.accent, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) { Text("Отмена", color = style.textSecondary) }
            }
        )
    }
}

@Composable
private fun HsvColorSliders(
    colorInt: Int,
    style: ResolvedWeatherStyle,
    onColorChange: (Int) -> Unit
) {
    val hsv = remember(colorInt) {
        FloatArray(3).also { AndroidColor.colorToHSV(colorInt, it) }
    }
    var h by remember(colorInt) { mutableFloatStateOf(hsv[0]) }
    var s by remember(colorInt) { mutableFloatStateOf(hsv[1]) }
    var v by remember(colorInt) { mutableFloatStateOf(hsv[2]) }

    val hexLabel = String.format("#%06X", 0xFFFFFF and colorInt)

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Текущий цвет: $hexLabel", color = style.textSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color(colorInt))
                    .border(1.5.dp, style.textPrimary.copy(alpha = 0.7f), CircleShape)
            )
        }

        StudioSlider("Оттенок (Hue)", "${h.toInt()}°", h, 0f..360f, style) { newH ->
            h = newH
            onColorChange(AndroidColor.HSVToColor(floatArrayOf(h, s, v)))
        }
        StudioSlider("Насыщенность (Sat)", "${(s * 100).toInt()}%", s, 0f..1f, style) { newS ->
            s = newS
            onColorChange(AndroidColor.HSVToColor(floatArrayOf(h, s, v)))
        }
        StudioSlider("Яркость (Чёрный ↔ Белый)", "${(v * 100).toInt()}%", v, 0f..1f, style) { newV ->
            v = newV
            onColorChange(AndroidColor.HSVToColor(floatArrayOf(h, s, v)))
        }
    }
}

@Composable
private fun QuickTemplateChip(
    label: String,
    bgColor: Color,
    fgColor: Color,
    style: ResolvedWeatherStyle,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .border(1.2.dp, style.textSecondary.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(label, color = fgColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun StudioChip(
    label: String,
    selected: Boolean,
    style: ResolvedWeatherStyle,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    WeatherGlassPanel(
        style = style,
        shape = RoundedCornerShape(14.dp),
        tintOverride = if (selected) style.accent else style.glassTint,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        modifier = modifier.clickable { onClick() }
    ) {
        val textCol = if (selected) {
            if (style.isLightBg && style.accent == Color(0xFF111827)) Color.White else Color.White
        } else style.textPrimary
        Text(label, color = textCol, fontSize = 12.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, modifier = Modifier.align(Alignment.Center))
    }
}

@Composable
private fun StudioSlider(
    title: String,
    valText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    style: ResolvedWeatherStyle,
    onChange: (Float) -> Unit
) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, color = style.textPrimary, fontSize = 13.sp)
            Text(valText, color = style.accent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = style.accent,
                activeTrackColor = style.accent,
                inactiveTrackColor = style.textSecondary.copy(alpha = 0.25f)
            )
        )
    }
}

@Composable
private fun StudioSwitch(title: String, checked: Boolean, style: ResolvedWeatherStyle, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = style.textPrimary, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = style.accent)
        )
    }
}

package com.liquidglass.weather

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.liquidglass.weather.data.WeatherCodeMapper
import com.liquidglass.weather.data.WeatherConditionKind
import com.liquidglass.weather.ui.components.LivingWeatherBackdrop
import com.liquidglass.weather.ui.components.WeatherGlassPanel
import com.liquidglass.weather.ui.components.weatherMotionBlur
import com.liquidglass.weather.ui.screens.*
import com.liquidglass.weather.ui.theme.buildWeatherTypography
import com.liquidglass.weather.ui.theme.rememberResolvedWeatherStyle
import com.liquidglass.weather.util.CustomFontManager
import com.liquidglass.weather.util.DisplayRefreshController
import com.liquidglass.weather.viewmodel.WeatherViewModel
import kotlinx.coroutines.launch
import kotlin.math.abs

private data class WeatherTab(val title: String, val icon: ImageVector)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
        )
        setContent {
            LiquidGlassWeatherRoot(activity = this)
        }
    }
}

@Composable
fun LiquidGlassWeatherRoot(
    activity: ComponentActivity,
    viewModel: WeatherViewModel = viewModel()
) {
    val context = LocalContext.current
    val config by viewModel.config.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val savedThemes by viewModel.savedThemes.collectAsState()

    val supportedHz = remember { DisplayRefreshController.getSupportedRefreshRates(activity) }
    var activeHz by remember { mutableIntStateOf(DisplayRefreshController.getCurrentRefreshRate(activity)) }

    LaunchedEffect(config.refreshRateMode) {
        activeHz = DisplayRefreshController.applyRefreshRate(activity, config.refreshRateMode)
    }

    var measuredFps by remember { mutableIntStateOf(activeHz) }
    LaunchedEffect(config.showFpsBadge) {
        if (!config.showFpsBadge) return@LaunchedEffect
        var frames = 0
        var last = System.nanoTime()
        while (true) {
            withFrameNanos { now ->
                frames++
                val diff = now - last
                if (diff >= 500_000_000L) {
                    measuredFps = ((frames * 1_000_000_000L) / diff).toInt()
                    frames = 0
                    last = now
                }
            }
        }
    }

    val fontPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val ok = viewModel.importCustomFont(uri)
            Toast.makeText(context, if (ok) "Свой шрифт применён!" else "Ошибка чтения .ttf/.otf", Toast.LENGTH_SHORT).show()
        }
    }

    val conditionKind = uiState.weather?.let { WeatherCodeMapper.kind(it.weatherCode) } ?: WeatherConditionKind.CLEAR
    val isDay = uiState.weather?.isDay ?: true

    val style = rememberResolvedWeatherStyle(config, conditionKind, isDay)
    val fontFamily = remember(config.customFontPath) { CustomFontManager.resolveFontFamily(config.customFontPath) }
    val typography = remember(fontFamily) { buildWeatherTypography(fontFamily) }

    val tabs = remember {
        listOf(
            WeatherTab("Погода", Icons.Rounded.WbSunny),
            WeatherTab("Графики", Icons.Rounded.ShowChart),
            WeatherTab("Города", Icons.Rounded.LocationCity),
            WeatherTab("Стиль", Icons.Rounded.Palette)
        )
    }
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { tabs.size })
    val scope = rememberCoroutineScope()

    val motionBlurX by remember(config.motionBlurEnabled, config.motionBlurIntensity) {
        derivedStateOf {
            if (!config.motionBlurEnabled) 0f
            else {
                val offset = abs(pagerState.currentPageOffsetFraction)
                val curve = (1f - abs(offset - 0.5f) * 2f).coerceAtLeast(0f) * (if (offset > 0.01f) 1f else 0f)
                val direct = (offset * 2f).coerceAtMost(1f)
                maxOf(curve, direct * 0.65f) * 24f * config.motionBlurIntensity
            }
        }
    }

    MaterialTheme(typography = typography) {
        Box(modifier = Modifier.fillMaxSize()) {
            LivingWeatherBackdrop(
                style = style,
                config = config,
                actualCondition = conditionKind
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.systemBars)
            ) {
                AnimatedVisibility(visible = config.showFpsBadge) {
                    Box(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), contentAlignment = Alignment.Center) {
                        WeatherGlassPanel(
                            style = style,
                            shape = RoundedCornerShape(50),
                            tintOverride = style.accent,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 3.dp)
                        ) {
                            Text("${activeHz} Гц • ${measuredFps} FPS", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .weatherMotionBlur(radiusX = motionBlurX, radiusY = motionBlurX * 0.1f)
                ) { page ->
                    when (page) {
                        0 -> WeatherNowScreen(
                            state = uiState,
                            style = style,
                            config = config,
                            onRefresh = { viewModel.refreshWeather() },
                            onOpenCities = { scope.launch { pagerState.animateScrollToPage(2) } }
                        )
                        1 -> ForecastChartsScreen(
                            weather = uiState.weather,
                            style = style,
                            config = config
                        )
                        2 -> CitiesScreen(
                            state = uiState,
                            style = style,
                            config = config,
                            onSearchChange = viewModel::updateCitySearch,
                            onAddCity = { city ->
                                viewModel.addAndSelectCity(city)
                                scope.launch { pagerState.animateScrollToPage(0) }
                            },
                            onSelectCity = { city ->
                                viewModel.refreshWeather(city)
                                scope.launch { pagerState.animateScrollToPage(0) }
                            },
                            onRemoveCity = viewModel::removeCity
                        )
                        3 -> ThemeStudioScreen(
                            config = config,
                            style = style,
                            savedThemes = savedThemes,
                            supportedHz = supportedHz,
                            activeHz = activeHz,
                            onUpdateConfig = viewModel::updateConfig,
                            onUpdateCustomStyle = viewModel::updateCustomStyle,
                            onSaveTheme = { name ->
                                viewModel.saveCurrentCustomTheme(name)
                                Toast.makeText(context, "Стиль сохранён!", Toast.LENGTH_SHORT).show()
                            },
                            onApplySavedTheme = { profile ->
                                viewModel.applySavedTheme(profile)
                                Toast.makeText(context, "Применён стиль: ${profile.name}", Toast.LENGTH_SHORT).show()
                            },
                            onDeleteSavedTheme = viewModel::deleteSavedTheme,
                            onReset = viewModel::resetConfig,
                            onPickCustomFont = {
                                fontPicker.launch(arrayOf("font/ttf", "font/otf", "application/x-font-ttf", "application/octet-stream", "*/*"))
                            }
                        )
                    }
                }

                // Floating Liquid Glass Bottom Navigation Dock
                WeatherGlassPanel(
                    style = style,
                    shape = RoundedCornerShape(50),
                    contentPadding = PaddingValues(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        tabs.forEachIndexed { idx, tab ->
                            val sel = pagerState.currentPage == idx
                            WeatherGlassPanel(
                                style = style,
                                shape = RoundedCornerShape(50),
                                tintOverride = if (sel) style.accent else Color.Transparent,
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { scope.launch { pagerState.animateScrollToPage(idx) } }
                            ) {
                                Row(
                                    modifier = Modifier.align(Alignment.Center),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    val fg = if (sel) Color.White else style.textSecondary
                                    Icon(tab.icon, contentDescription = tab.title, tint = fg, modifier = Modifier.size(18.dp))
                                    if (sel) {
                                        Text(tab.title, color = fg, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

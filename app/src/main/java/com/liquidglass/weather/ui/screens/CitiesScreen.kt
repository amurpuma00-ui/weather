package com.liquidglass.weather.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liquidglass.weather.data.SavedCity
import com.liquidglass.weather.data.WeatherAppConfig
import com.liquidglass.weather.ui.components.WeatherGlassPanel
import com.liquidglass.weather.ui.theme.ResolvedWeatherStyle
import com.liquidglass.weather.viewmodel.WeatherUiState

@Composable
fun CitiesScreen(
    state: WeatherUiState,
    style: ResolvedWeatherStyle,
    config: WeatherAppConfig,
    onSearchChange: (String) -> Unit,
    onAddCity: (SavedCity) -> Unit,
    onSelectCity: (SavedCity) -> Unit,
    onRemoveCity: (SavedCity) -> Unit
) {
    val scale = config.fontScale

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Города мира", color = style.textPrimary, fontSize = (24f * scale).sp, fontWeight = FontWeight.Bold)

        // Search Bar
        WeatherGlassPanel(
            style = style,
            shape = RoundedCornerShape(50),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            TextField(
                value = state.searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("Найти город (например: Казань, Дубай)...", color = style.textSecondary, fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = style.accent) },
                trailingIcon = {
                    if (state.isSearching) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = style.accent, strokeWidth = 2.dp)
                    } else if (state.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(Icons.Rounded.Close, contentDescription = null, tint = style.textSecondary)
                        }
                    }
                },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = style.textPrimary,
                    unfocusedTextColor = style.textPrimary,
                    cursorColor = style.accent
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (state.searchResults.isNotEmpty()) {
            Text("Результаты поиска", color = style.accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.searchResults, key = { it.id }) { city ->
                    WeatherGlassPanel(
                        style = style,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onAddCity(city) }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(city.name, color = style.textPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text(city.country, color = style.textSecondary, fontSize = 12.sp)
                            }
                            Icon(Icons.Rounded.AddCircleOutline, contentDescription = "Добавить", tint = style.accent)
                        }
                    }
                }
            }
        } else {
            Text("Сохранённые города", color = style.textSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 12.dp)
            ) {
                items(state.cities, key = { it.id }) { city ->
                    val selected = city.id == state.selectedCity.id
                    WeatherGlassPanel(
                        style = style,
                        tintOverride = if (selected) style.accent else style.glassTint,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectCity(city) }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(city.name, color = style.textPrimary, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                                Text(city.country, color = style.textSecondary, fontSize = 12.sp)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (selected) {
                                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = style.textPrimary, modifier = Modifier.size(20.dp))
                                }
                                if (state.cities.size > 1) {
                                    IconButton(onClick = { onRemoveCity(city) }) {
                                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "Удалить", tint = style.textSecondary)
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

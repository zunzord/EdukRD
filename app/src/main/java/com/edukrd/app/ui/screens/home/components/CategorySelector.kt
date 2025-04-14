package com.edukrd.app.ui.screens.home.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape

@Composable
fun CategorySelector(
    categories: List<String>,
    currentCategory: String?,
    onCategorySelected: (String?) -> Unit
) {
    LazyRow(
        // Ajusta los paddings según tu gusto
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Chip "Todas" (sin filtro)
        item {
            Surface(
                shape = RoundedCornerShape(50), // Hace los bordes circulares
                color = if (currentCategory == null) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .clickable { onCategorySelected(null) }
            ) {
                Text(
                    text = "Todas",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (currentCategory == null) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }

        // Chips de cada categoría
        items(categories) { category ->
            Surface(
                shape = RoundedCornerShape(50),  // Mismo redondeo
                color = if (category == currentCategory) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .clickable { onCategorySelected(category) }
            ) {
                Text(
                    text = category,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (category == currentCategory) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
    }
}

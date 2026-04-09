package com.cephalon.lucyApp.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun NasScreen(onBack: () -> Unit) {
    var selectedCategory by remember { mutableStateOf(NasCategory.Photos) }
    val monthSections = listOf(
        NasMonthSection(label = "4\n月", itemCount = 12),
        NasMonthSection(label = "3\n月", itemCount = 6)
    )

    Scaffold(containerColor = Color(0xF0FFFFFF)) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "NAS",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF121212)
                )
                TextButton(onClick = onBack) {
                    Text("返回")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                modifier = Modifier.fillMaxSize(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 16.dp)
                ) {
                    NasCategoryAndAddRow(
                        selected = selectedCategory,
                        onSelect = { selectedCategory = it },
                        onAddClick = {}
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier.weight(1f)
                    ) {
                        when (selectedCategory) {
                            NasCategory.Photos -> NasPhotosContent(monthSections = monthSections)
                            NasCategory.Recordings -> NasRecordingsContent()
                            NasCategory.Documents -> NasDocumentsContent()
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    NasSearchBar(onClick = {})
                }
            }
        }
    }
}

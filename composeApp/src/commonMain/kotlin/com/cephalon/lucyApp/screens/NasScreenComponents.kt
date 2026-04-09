package com.cephalon.lucyApp.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

internal enum class NasCategory(val title: String) {
    Photos("照片"),
    Recordings("录音"),
    Documents("文档")
}

internal data class NasMonthSection(
    val label: String,
    val itemCount: Int
)

@Composable
internal fun NasCategoryAndAddRow(
    selected: NasCategory,
    onSelect: (NasCategory) -> Unit,
    onAddClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            NasCategory.entries.forEach { category ->
                NasCategoryChip(
                    title = category.title,
                    selected = selected == category,
                    onClick = { onSelect(category) }
                )
            }
        }

        IconButton(
            onClick = onAddClick,
            modifier = Modifier
                .size(36.dp)
                .background(Color.White, CircleShape)
        ) {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = "新增",
                tint = Color(0xFF3A3A3A)
            )
        }
    }
}

@Composable
internal fun NasSearchBar(
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(999.dp),
        color = Color.White
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "搜索资源文件",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF8E8E93)
            )
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = "搜索",
                tint = Color(0xFF3A3A3A)
            )
        }
    }
}

@Composable
internal fun NasPhotosContent(
    monthSections: List<NasMonthSection>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        monthSections.forEachIndexed { index, section ->
            NasMonthGrid(section = section)
            if (index != monthSections.lastIndex) {
                Spacer(modifier = Modifier.height(14.dp))
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
    }
}

@Composable
internal fun NasRecordingsContent() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "录音",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = Color(0xFF222222)
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "这里显示录音内容列表（待接入数据）。",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF5A5A5A)
        )
    }
}

@Composable
internal fun NasDocumentsContent() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "文档",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = Color(0xFF222222)
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "这里显示文档内容列表（待接入数据）。",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF5A5A5A)
        )
    }
}

@Composable
private fun NasCategoryChip(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(999.dp),
        color = if (selected) Color.Black else Color.White
    ) {
        Text(
            text = title,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = if (selected) Color.White else Color(0xFF1F1F1F)
        )
    }
}

@Composable
private fun NasMonthGrid(section: NasMonthSection) {
    val rows = (section.itemCount + 2) / 3

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = section.label,
            modifier = Modifier.width(20.dp),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
            color = Color(0xFF222222)
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            repeat(rows) { rowIndex ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    repeat(3) { columnIndex ->
                        val itemIndex = rowIndex * 3 + columnIndex
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(106.dp)
                                .background(
                                    color = if (itemIndex < section.itemCount) Color.White else Color.Transparent,
                                    shape = RoundedCornerShape(14.dp)
                                )
                        )
                    }
                }
            }
        }
    }
}

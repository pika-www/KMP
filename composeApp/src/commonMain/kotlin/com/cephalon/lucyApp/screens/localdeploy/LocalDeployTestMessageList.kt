package com.cephalon.lucyApp.screens.localdeploy

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cephalon.lucyApp.media.AudioRecording
import com.cephalon.lucyApp.media.PlatformImageThumbnail

@Composable
internal fun LocalDeployTestMessageList(
    messages: List<ChatItem>,
    recordings: List<AudioRecording>,
    onPlayRecording: (AudioRecording) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(12.dp))
        }

        items(items = messages) { item ->
            when (item) {
                is ChatItem.Assistant -> {
                    Bubble(
                        text = item.text,
                        background = Color.White,
                        textColor = Color(0xFF111111),
                        alignEnd = false
                    )
                }

                is ChatItem.User -> {
                    Bubble(
                        text = item.text,
                        background = Color(0xFF111111),
                        textColor = Color.White,
                        alignEnd = true
                    )
                }

                is ChatItem.UserAttachments -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF111111)),
                            modifier = Modifier.fillMaxWidth(0.82f)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                val messageText = item.text
                                if (!messageText.isNullOrBlank()) {
                                    Text(
                                        text = messageText,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White
                                    )
                                }

                                val images = item.attachments.filter { it.type == DraftAttachmentType.Image }
                                val files = item.attachments.filter { it.type == DraftAttachmentType.File }

                                if (images.isNotEmpty()) {
                                    LazyVerticalGrid(
                                        columns = GridCells.Fixed(2),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(if (images.size <= 2) 140.dp else 220.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        items(images) { attachment ->
                                            Card(
                                                shape = RoundedCornerShape(12.dp),
                                                colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2B2B)),
                                            ) {
                                                PlatformImageThumbnail(
                                                    uri = attachment.uri,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .fillMaxHeight()
                                                )
                                            }
                                        }
                                    }
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    files.forEach { attachment ->
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = Color(0xFF2B2B2B)
                                        ) {
                                            Text(
                                                text = uriDisplayName(attachment.uri),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                is ChatItem.System -> {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        Text(
                            text = item.text,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF8A8A8A),
                            modifier = Modifier
                                .background(Color.Transparent)
                                .padding(vertical = 2.dp)
                        )
                    }
                }

                is ChatItem.RecordingItem -> {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFFE7E7E7)),
                            modifier = Modifier.fillMaxWidth(0.82f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                androidx.compose.material3.Icon(
                                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = null,
                                    tint = Color(0xFF111111)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = Color(0xFF111111)
                                    )
                                    Text(
                                        text = item.path,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF777777)
                                    )
                                }
                                OutlinedButton(
                                    onClick = {
                                        val recording = recordings.firstOrNull { it.id == item.id }
                                        if (recording != null) {
                                            onPlayRecording(recording)
                                        }
                                    }
                                ) {
                                    Text("播放")
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

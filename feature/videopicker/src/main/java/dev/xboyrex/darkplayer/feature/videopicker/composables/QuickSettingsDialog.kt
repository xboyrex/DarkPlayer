package dev.xboyrex.darkplayer.feature.videopicker.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderCopy
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Hd
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.SlowMotionVideo
import androidx.compose.material.icons.rounded.SmartDisplay
import androidx.compose.material.icons.rounded.SortByAlpha
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import dev.xboyrex.darkplayer.core.model.ApplicationPreferences
import dev.xboyrex.darkplayer.core.model.MediaLayoutMode
import dev.xboyrex.darkplayer.core.model.MediaViewMode
import dev.xboyrex.darkplayer.core.model.Sort
import dev.xboyrex.darkplayer.core.ui.R
import dev.xboyrex.darkplayer.core.ui.components.NextSwitch
import dev.xboyrex.darkplayer.core.ui.designsystem.NextIcons
import dev.xboyrex.darkplayer.core.ui.theme.NextPlayerTheme

@OptIn(
    ExperimentalLayoutApi::class,
    ExperimentalMaterial3Api::class,
)
@Composable
fun QuickSettingsDialog(
    applicationPreferences: ApplicationPreferences,
    onDismiss: () -> Unit,
    updatePreferences: (ApplicationPreferences) -> Unit,
) {
    var preferences by remember { mutableStateOf(value = applicationPreferences) }
    var fieldsExpanded by remember { mutableStateOf(false) }
    var advancedExpanded by remember { mutableStateOf(false) }
    val mxBlue = Color(0xFF38BDF8)
    val surfaceBg = Color(0xFF1A222D)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = surfaceBg,
            contentColor = Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .border(0.8.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(22.dp)),
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // Top Row: View Mode (left) & Layout (right) — matching 155332_MX Player Pro.png
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(
                        modifier = Modifier.weight(1.45f),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            text = "View Mode",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            MxIconOptionTile(
                                label = "All folders",
                                icon = Icons.Rounded.FolderCopy,
                                selected = preferences.mediaViewMode == MediaViewMode.FOLDERS,
                                accentColor = mxBlue,
                                onClick = {
                                    preferences = preferences.copy(mediaViewMode = MediaViewMode.FOLDERS)
                                },
                            )
                            MxIconOptionTile(
                                label = "Files",
                                icon = Icons.AutoMirrored.Rounded.InsertDriveFile,
                                selected = preferences.mediaViewMode == MediaViewMode.VIDEOS,
                                accentColor = mxBlue,
                                onClick = {
                                    preferences = preferences.copy(mediaViewMode = MediaViewMode.VIDEOS)
                                },
                            )
                            MxIconOptionTile(
                                label = "Folders",
                                icon = Icons.Rounded.Folder,
                                selected = preferences.mediaViewMode == MediaViewMode.FOLDER_TREE,
                                accentColor = mxBlue,
                                onClick = {
                                    preferences = preferences.copy(mediaViewMode = MediaViewMode.FOLDER_TREE)
                                },
                            )
                        }
                    }

                    Spacer(modifier = Modifier.size(16.dp))

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            text = "Layout",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround,
                        ) {
                            MxIconOptionTile(
                                label = "List",
                                icon = Icons.Rounded.ViewAgenda,
                                selected = preferences.mediaLayoutMode == MediaLayoutMode.LIST,
                                accentColor = mxBlue,
                                onClick = {
                                    preferences = preferences.copy(mediaLayoutMode = MediaLayoutMode.LIST)
                                },
                            )
                            MxIconOptionTile(
                                label = "Grid",
                                icon = Icons.Rounded.GridView,
                                selected = preferences.mediaLayoutMode == MediaLayoutMode.GRID,
                                accentColor = mxBlue,
                                onClick = {
                                    preferences = preferences.copy(mediaLayoutMode = MediaLayoutMode.GRID)
                                },
                            )
                        }
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.12f))

                // Sort 5x2 Grid + [↑ Oldest | ↓ Newest] — matching 155332_MX Player Pro.png
                Text(
                    text = "Sort",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )

                val sortItems = listOf(
                    Triple("Title", Icons.Rounded.SortByAlpha, Sort.By.TITLE),
                    Triple("Date", Icons.Rounded.CalendarToday, Sort.By.DATE),
                    Triple("Played time", Icons.Rounded.History, Sort.By.DATE),
                    Triple("Status", Icons.Rounded.SlowMotionVideo, Sort.By.DATE),
                    Triple("Length", Icons.Rounded.SmartDisplay, Sort.By.LENGTH),
                    Triple("Size", Icons.Rounded.Memory, Sort.By.SIZE),
                    Triple("Resolution", Icons.Rounded.Hd, Sort.By.SIZE),
                    Triple("Path", Icons.Rounded.Storage, Sort.By.PATH),
                    Triple("Frame rate", Icons.Rounded.Speed, Sort.By.LENGTH),
                    Triple("Type", Icons.AutoMirrored.Rounded.InsertDriveFile, Sort.By.TITLE),
                )

                sortItems.chunked(5).forEach { rowList ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        rowList.forEachIndexed { idx, (label, icon, sortBy) ->
                            val isPrimaryMatch = preferences.sortBy == sortBy &&
                                (label in listOf("Title", "Date", "Length", "Size", "Path"))
                            MxIconOptionTile(
                                label = label,
                                icon = icon,
                                selected = isPrimaryMatch,
                                accentColor = mxBlue,
                                onClick = {
                                    preferences = preferences.copy(sortBy = sortBy)
                                },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }

                // [↑ Oldest] [↓ Newest] segmented bar
                val isAsc = preferences.sortOrder == Sort.Order.ASCENDING
                val ascLabel = if (preferences.sortBy == Sort.By.DATE) "↑ Oldest" else "↑ Ascending"
                val descLabel = if (preferences.sortBy == Sort.By.DATE) "↓ Newest" else "↓ Descending"

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(8.dp)),
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .background(if (isAsc) mxBlue.copy(alpha = 0.18f) else Color.Transparent)
                            .then(
                                if (isAsc) Modifier.border(1.dp, mxBlue, RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp))
                                else Modifier,
                            )
                            .clickable {
                                preferences = preferences.copy(sortOrder = Sort.Order.ASCENDING)
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = ascLabel,
                            fontSize = 13.sp,
                            fontWeight = if (isAsc) FontWeight.Bold else FontWeight.Medium,
                            color = if (isAsc) mxBlue else Color.White.copy(alpha = 0.72f),
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .background(if (!isAsc) mxBlue.copy(alpha = 0.18f) else Color.Transparent)
                            .then(
                                if (!isAsc) Modifier.border(1.dp, mxBlue, RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp))
                                else Modifier,
                            )
                            .clickable {
                                preferences = preferences.copy(sortOrder = Sort.Order.DESCENDING)
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = descLabel,
                            fontSize = 13.sp,
                            fontWeight = if (!isAsc) FontWeight.Bold else FontWeight.Medium,
                            color = if (!isAsc) mxBlue else Color.White.copy(alpha = 0.72f),
                        )
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.12f))

                // Expandable Fields Accordion (matching 155332_MX Player Pro.png)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { fieldsExpanded = !fieldsExpanded }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.fields),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                    Icon(
                        imageVector = if (fieldsExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.75f),
                    )
                }

                AnimatedVisibility(visible = fieldsExpanded) {
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(align = Alignment.Top),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FieldChip(
                            label = stringResource(id = R.string.duration),
                            selected = preferences.showDurationField,
                            onClick = {
                                preferences = preferences.copy(showDurationField = !preferences.showDurationField)
                            },
                        )
                        FieldChip(
                            label = stringResource(id = R.string.folder_duration),
                            selected = preferences.showFolderDurationField,
                            onClick = {
                                preferences = preferences.copy(showFolderDurationField = !preferences.showFolderDurationField)
                            },
                        )
                        FieldChip(
                            label = stringResource(id = R.string.extension),
                            selected = preferences.showExtensionField,
                            onClick = {
                                preferences = preferences.copy(showExtensionField = !preferences.showExtensionField)
                            },
                        )
                        FieldChip(
                            label = stringResource(id = R.string.path),
                            selected = preferences.showPathField,
                            onClick = {
                                preferences = preferences.copy(showPathField = !preferences.showPathField)
                            },
                        )
                        FieldChip(
                            label = stringResource(id = R.string.played_progress),
                            selected = preferences.showPlayedProgress,
                            onClick = {
                                preferences = preferences.copy(showPlayedProgress = !preferences.showPlayedProgress)
                            },
                        )
                        FieldChip(
                            label = stringResource(id = R.string.resolution),
                            selected = preferences.showResolutionField,
                            onClick = {
                                preferences = preferences.copy(showResolutionField = !preferences.showResolutionField)
                            },
                        )
                        FieldChip(
                            label = stringResource(id = R.string.size),
                            selected = preferences.showSizeField,
                            onClick = {
                                preferences = preferences.copy(showSizeField = !preferences.showSizeField)
                            },
                        )
                        FieldChip(
                            label = stringResource(id = R.string.thumbnail),
                            selected = preferences.showThumbnailField,
                            onClick = {
                                preferences = preferences.copy(showThumbnailField = !preferences.showThumbnailField)
                            },
                        )
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.12f))

                // Expandable Advanced Accordion (matching 155332_MX Player Pro.png)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { advancedExpanded = !advancedExpanded }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Advanced",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                    Icon(
                        imageVector = if (advancedExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.75f),
                    )
                }

                AnimatedVisibility(visible = advancedExpanded) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("Highlight last played media", color = Color.White, fontSize = 14.sp)
                            NextSwitch(
                                checked = preferences.markLastPlayedMedia,
                                onCheckedChange = {
                                    preferences = preferences.copy(markLastPlayedMedia = it)
                                },
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("Include network watch history", color = Color.White, fontSize = 14.sp)
                            NextSwitch(
                                checked = preferences.includeNetworkWatchHistory,
                                onCheckedChange = {
                                    preferences = preferences.copy(includeNetworkWatchHistory = it)
                                },
                            )
                        }
                    }
                }

                // Bottom Cancel / Done buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(
                            text = "Cancel",
                            color = Color.White.copy(alpha = 0.72f),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                        )
                    }
                    Spacer(modifier = Modifier.size(8.dp))
                    TextButton(
                        onClick = {
                            updatePreferences(preferences)
                            onDismiss()
                        },
                    ) {
                        Text(
                            text = "Done",
                            color = mxBlue,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MxIconOptionTile(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) accentColor else Color.White.copy(alpha = 0.80f),
            modifier = Modifier.size(24.dp),
        )
        Text(
            text = label,
            fontSize = 11.5.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) accentColor else Color.White.copy(alpha = 0.80f),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun FieldChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selectedIcon: ImageVector = NextIcons.CheckBox,
    unselectedIcon: ImageVector = NextIcons.CheckBoxOutline,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text = label) },
        leadingIcon = {
            Icon(
                imageVector = if (selected) selectedIcon else unselectedIcon,
                contentDescription = "",
                modifier = Modifier.size(FilterChipDefaults.IconSize),
                tint = Color(0xFF38BDF8),
            )
        },
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            selectedBorderWidth = 1.dp,
            selectedBorderColor = Color(0xFF38BDF8),
        ),
        modifier = modifier,
    )
}

@Preview
@Composable
private fun QuickSettingsPreview() {
    NextPlayerTheme {
        QuickSettingsDialog(
            applicationPreferences = ApplicationPreferences(),
            onDismiss = {},
            updatePreferences = {},
        )
    }
}

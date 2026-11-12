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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DataUsage
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderCopy
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Hd
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.SortByAlpha
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.material.icons.rounded.ViewList
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
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
import dev.xboyrex.darkplayer.core.model.MediaListCardStyle
import dev.xboyrex.darkplayer.core.model.MediaViewMode
import dev.xboyrex.darkplayer.core.model.Sort
import dev.xboyrex.darkplayer.core.ui.R
import dev.xboyrex.darkplayer.core.ui.components.NextSwitch
import dev.xboyrex.darkplayer.core.ui.designsystem.NextIcons
import dev.xboyrex.darkplayer.core.ui.glass.LocalNovaAccentColor
import dev.xboyrex.darkplayer.core.ui.glass.LocalNovaHapticsEnabled
import dev.xboyrex.darkplayer.core.ui.glass.novaHaptic

/**
 * MX Player Pro View Mode / Layout / Sort / Fields / Advanced Dialog
 * Exact implementation of Screenshot_20261110-155332_MX Player Pro.png (#1 in mx_screenshots).
 */
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
    var fieldsExpanded by rememberSaveable { mutableStateOf(false) }
    var advancedExpanded by rememberSaveable { mutableStateOf(false) }
    val accent = LocalNovaAccentColor.current
    val view = LocalView.current
    val hapticsEnabled = LocalNovaHapticsEnabled.current

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF1F2430),
            contentColor = Color.White,
            tonalElevation = 8.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 20.dp),
            ) {
                // Top Row: View Mode (left) & Layout (right) — exact match to MX #1 (155332)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1.55f)) {
                        Text(
                            text = "View Mode",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            MxIconOptionTile(
                                label = "All folders",
                                icon = Icons.Rounded.FolderCopy,
                                selected = preferences.mediaViewMode == MediaViewMode.FOLDER_TREE,
                                accent = accent,
                                onClick = {
                                    view.novaHaptic(10L, 100, hapticsEnabled)
                                    preferences = preferences.copy(mediaViewMode = MediaViewMode.FOLDER_TREE)
                                },
                            )
                            MxIconOptionTile(
                                label = "Files",
                                icon = Icons.Rounded.Description,
                                selected = preferences.mediaViewMode == MediaViewMode.VIDEOS,
                                accent = accent,
                                onClick = {
                                    view.novaHaptic(10L, 100, hapticsEnabled)
                                    preferences = preferences.copy(mediaViewMode = MediaViewMode.VIDEOS)
                                },
                            )
                            MxIconOptionTile(
                                label = "Folders",
                                icon = Icons.Rounded.Folder,
                                selected = preferences.mediaViewMode == MediaViewMode.FOLDERS,
                                accent = accent,
                                onClick = {
                                    view.novaHaptic(10L, 100, hapticsEnabled)
                                    preferences = preferences.copy(mediaViewMode = MediaViewMode.FOLDERS)
                                },
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Layout",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                        ) {
                            MxIconOptionTile(
                                label = "List",
                                icon = Icons.Rounded.ViewList,
                                selected = preferences.mediaLayoutMode == MediaLayoutMode.LIST,
                                accent = accent,
                                onClick = {
                                    view.novaHaptic(10L, 100, hapticsEnabled)
                                    preferences = preferences.copy(mediaLayoutMode = MediaLayoutMode.LIST)
                                },
                            )
                            MxIconOptionTile(
                                label = "Grid",
                                icon = Icons.Rounded.GridView,
                                selected = preferences.mediaLayoutMode == MediaLayoutMode.GRID,
                                accent = accent,
                                onClick = {
                                    view.novaHaptic(10L, 100, hapticsEnabled)
                                    preferences = preferences.copy(mediaLayoutMode = MediaLayoutMode.GRID)
                                },
                            )
                        }
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 16.dp),
                    color = Color.White.copy(alpha = 0.10f),
                )

                // Sort Section — 2x5 Grid matching MX #1 (155332)
                Text(
                    text = "Sort",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                )
                Spacer(modifier = Modifier.height(12.dp))

                val row1Sort = listOf(
                    Triple("Title", Icons.Rounded.SortByAlpha, Sort.By.TITLE),
                    Triple("Date", Icons.Rounded.CalendarMonth, Sort.By.DATE),
                    Triple("Played time", Icons.Rounded.History, Sort.By.DATE),
                    Triple("Status", Icons.Rounded.CheckCircle, Sort.By.DATE),
                    Triple("Length", Icons.Rounded.Schedule, Sort.By.LENGTH),
                )
                val row2Sort = listOf(
                    Triple("Size", Icons.Rounded.DataUsage, Sort.By.SIZE),
                    Triple("Resolution", Icons.Rounded.Hd, Sort.By.SIZE),
                    Triple("Path", Icons.Rounded.LocationOn, Sort.By.PATH),
                    Triple("Frame rate", Icons.Rounded.Speed, Sort.By.LENGTH),
                    Triple("Type", Icons.Rounded.Straighten, Sort.By.TITLE),
                )
                var selectedSortLabel by remember {
                    mutableStateOf(
                        when (preferences.sortBy) {
                            Sort.By.TITLE -> "Title"
                            Sort.By.DATE -> "Date"
                            Sort.By.LENGTH -> "Length"
                            Sort.By.SIZE -> "Size"
                            Sort.By.PATH -> "Path"
                        },
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    row1Sort.forEach { (label, icon, sortBy) ->
                        MxIconOptionTile(
                            label = label,
                            icon = icon,
                            selected = selectedSortLabel == label && preferences.sortBy == sortBy,
                            accent = accent,
                            onClick = {
                                view.novaHaptic(10L, 100, hapticsEnabled)
                                selectedSortLabel = label
                                preferences = preferences.copy(sortBy = sortBy)
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    row2Sort.forEach { (label, icon, sortBy) ->
                        MxIconOptionTile(
                            label = label,
                            icon = icon,
                            selected = selectedSortLabel == label && preferences.sortBy == sortBy,
                            accent = accent,
                            onClick = {
                                view.novaHaptic(10L, 100, hapticsEnabled)
                                selectedSortLabel = label
                                preferences = preferences.copy(sortBy = sortBy)
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Ascending / Descending Pill Buttons (↑ Oldest / ↓ Newest) matching MX #1
                val ascLabel = when (preferences.sortBy) {
                    Sort.By.DATE -> "↑ Oldest"
                    Sort.By.LENGTH -> "↑ Shortest"
                    Sort.By.SIZE -> "↑ Smallest"
                    else -> "↑ A — Z"
                }
                val descLabel = when (preferences.sortBy) {
                    Sort.By.DATE -> "↓ Newest"
                    Sort.By.LENGTH -> "↓ Longest"
                    Sort.By.SIZE -> "↓ Largest"
                    else -> "↓ Z — A"
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    val isAsc = preferences.sortOrder == Sort.Order.ASCENDING
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isAsc) accent.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.06f))
                            .border(
                                1.dp,
                                if (isAsc) accent else Color.Transparent,
                                RoundedCornerShape(8.dp),
                            )
                            .clickable {
                                view.novaHaptic(10L, 100, hapticsEnabled)
                                preferences = preferences.copy(sortOrder = Sort.Order.ASCENDING)
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = ascLabel,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isAsc) accent else Color(0xFFB8C0CC),
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (!isAsc) accent.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.06f))
                            .border(
                                1.dp,
                                if (!isAsc) accent else Color.Transparent,
                                RoundedCornerShape(8.dp),
                            )
                            .clickable {
                                view.novaHaptic(10L, 100, hapticsEnabled)
                                preferences = preferences.copy(sortOrder = Sort.Order.DESCENDING)
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = descLabel,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (!isAsc) accent else Color(0xFFB8C0CC),
                        )
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(top = 16.dp),
                    color = Color.White.copy(alpha = 0.10f),
                )

                // Expandable "Fields" Section matching MX #1
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { fieldsExpanded = !fieldsExpanded }
                        .padding(vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(R.string.fields),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                    )
                    Icon(
                        imageVector = if (fieldsExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        contentDescription = null,
                        tint = Color(0xFFB8C0CC),
                    )
                }

                AnimatedVisibility(visible = fieldsExpanded) {
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        FieldChip(
                            label = stringResource(id = R.string.thumbnail),
                            selected = preferences.showThumbnailField,
                            onClick = {
                                preferences = preferences.copy(showThumbnailField = !preferences.showThumbnailField)
                            },
                        )
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
                                preferences = preferences.copy(
                                    showFolderDurationField = !preferences.showFolderDurationField,
                                )
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
                            label = stringResource(id = R.string.size),
                            selected = preferences.showSizeField,
                            onClick = {
                                preferences = preferences.copy(showSizeField = !preferences.showSizeField)
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
                            label = stringResource(id = R.string.path),
                            selected = preferences.showPathField,
                            onClick = {
                                preferences = preferences.copy(showPathField = !preferences.showPathField)
                            },
                        )
                        FieldChip(
                            label = stringResource(id = R.string.extension),
                            selected = preferences.showExtensionField,
                            onClick = {
                                preferences = preferences.copy(showExtensionField = !preferences.showExtensionField)
                            },
                        )
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.10f))

                // Expandable "Advanced" Section matching MX #1
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { advancedExpanded = !advancedExpanded }
                        .padding(vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "Advanced",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                    )
                    Icon(
                        imageVector = if (advancedExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        contentDescription = null,
                        tint = Color(0xFFB8C0CC),
                    )
                }

                AnimatedVisibility(visible = advancedExpanded) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = "Clean MX List Style (No Box Borders)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White,
                                modifier = Modifier.weight(1f),
                            )
                            NextSwitch(
                                checked = preferences.mediaListCardStyle == MediaListCardStyle.MX_PRO_CLEAN,
                                onCheckedChange = { clean ->
                                    preferences = preferences.copy(
                                        mediaListCardStyle = if (clean) {
                                            MediaListCardStyle.MX_PRO_CLEAN
                                        } else {
                                            MediaListCardStyle.SEGMENTED_CARD
                                        },
                                    )
                                },
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = "Show Top Quick Category Bar",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White,
                                modifier = Modifier.weight(1f),
                            )
                            NextSwitch(
                                checked = preferences.showMxTopCategoryChips,
                                onCheckedChange = {
                                    preferences = preferences.copy(showMxTopCategoryChips = it)
                                },
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = "Highlight Last Played Media",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White,
                                modifier = Modifier.weight(1f),
                            )
                            NextSwitch(
                                checked = preferences.markLastPlayedMedia,
                                onCheckedChange = {
                                    preferences = preferences.copy(markLastPlayedMedia = it)
                                },
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bottom Cancel / Done buttons matching MX #1
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(
                            text = stringResource(R.string.cancel),
                            color = Color(0xFFB8C0CC),
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = {
                            updatePreferences(preferences)
                            onDismiss()
                        },
                    ) {
                        Text(
                            text = stringResource(R.string.done),
                            color = accent,
                            fontWeight = FontWeight.Bold,
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
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tint = if (selected) accent else Color(0xFFD0D6E2)
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(22.dp),
        )
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = tint,
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
    val accent = LocalNovaAccentColor.current
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text = label, color = Color.White) },
        leadingIcon = {
            Icon(
                imageVector = if (selected) selectedIcon else unselectedIcon,
                contentDescription = "",
                modifier = Modifier.size(FilterChipDefaults.IconSize),
                tint = if (selected) accent else Color(0xFFB8C0CC),
            )
        },
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            selectedBorderWidth = 1.dp,
            selectedBorderColor = accent,
        ),
        modifier = modifier,
    )
}

@Preview
@Composable
fun QuickSettingsPreview() {
    Surface {
        QuickSettingsDialog(
            applicationPreferences = ApplicationPreferences(),
            onDismiss = {},
            updatePreferences = {},
        )
    }
}

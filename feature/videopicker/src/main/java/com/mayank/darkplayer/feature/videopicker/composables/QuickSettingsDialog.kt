package com.mayank.darkplayer.feature.videopicker.composables

import android.os.Build
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
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
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderCopy
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.SdStorage
import androidx.compose.material.icons.rounded.SortByAlpha
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.mayank.darkplayer.core.model.ApplicationPreferences
import com.mayank.darkplayer.core.model.MediaLayoutMode
import com.mayank.darkplayer.core.model.MediaViewMode
import com.mayank.darkplayer.core.model.Sort
import com.mayank.darkplayer.core.ui.R
import com.mayank.darkplayer.core.ui.theme.DarkPlayerUiPrefs
import com.mayank.darkplayer.core.ui.theme.calmPressScale
import androidx.compose.animation.scaleOut
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun QuickSettingsDialog(
    applicationPreferences: ApplicationPreferences,
    onDismiss: () -> Unit,
    updatePreferences: (ApplicationPreferences) -> Unit,
) {
    var preferences by remember { mutableStateOf(applicationPreferences) }
    var fieldsExpanded by remember { mutableStateOf(false) }
    var advancedExpanded by remember { mutableStateOf(false) }
    var entered by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val smoothDismiss: () -> Unit = { scope.launch { entered = false; delay(120); onDismiss() } }

    DisposableEffect(Unit) {
        DarkPlayerUiPrefs.pushModalBlur()
        onDispose { DarkPlayerUiPrefs.popModalBlur() }
    }
    LaunchedEffect(Unit) { entered = true }

    val cardBg = MaterialTheme.colorScheme.surfaceContainerHigh
    val innerTonal = MaterialTheme.colorScheme.surfaceContainerHighest
    val accent = MaterialTheme.colorScheme.primary
    val onAccent = MaterialTheme.colorScheme.onPrimary
    val accentContainer = MaterialTheme.colorScheme.primaryContainer
    val onAccentContainer = MaterialTheme.colorScheme.onPrimaryContainer
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    Dialog(
        onDismissRequest = smoothDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        val dialogView = LocalView.current
        androidx.compose.runtime.SideEffect {
            val window = (dialogView.parent as? DialogWindowProvider)?.window
            if (window != null) {
                window.setDimAmount(0.42f)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                    window.attributes = window.attributes.apply {
                        blurBehindRadius = 64
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = entered,
            enter = fadeIn(tween(160)) +
                scaleIn(
                    initialScale = 0.88f,
                    animationSpec = spring(dampingRatio = 0.80f, stiffness = 460f),
                ) +
                slideInVertically(
                    initialOffsetY = { it / 12 },
                    animationSpec = spring(dampingRatio = 0.84f, stiffness = 440f),
                ),
            exit = fadeOut(tween(115)) + scaleOut(targetScale = 0.90f, animationSpec = tween(115)),
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(28.dp),
                color = cardBg,
                tonalElevation = 0.dp,
                shadowElevation = 24.dp,
            ) {
                Column(
                    modifier = Modifier
                        .animateContentSize(animationSpec = spring(dampingRatio = 0.86f, stiffness = 440f))
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                ) {
                    // Compact View Mode & Layout Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "View Mode",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(3f),
                        )
                        Text(
                            text = "Layout",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(2f).padding(start = 10.dp),
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(innerTonal.copy(alpha = 0.65f))
                            .padding(vertical = 6.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CalmIconTile(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Rounded.FolderCopy,
                            label = "All",
                            selected = preferences.mediaViewMode == MediaViewMode.FOLDER_TREE,
                            accent = accent,
                            accentContainer = accentContainer,
                            muted = muted,
                            onClick = { preferences = preferences.copy(mediaViewMode = MediaViewMode.FOLDER_TREE) },
                        )
                        CalmIconTile(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Rounded.InsertDriveFile,
                            label = "Files",
                            selected = preferences.mediaViewMode == MediaViewMode.VIDEOS,
                            accent = accent,
                            accentContainer = accentContainer,
                            muted = muted,
                            onClick = { preferences = preferences.copy(mediaViewMode = MediaViewMode.VIDEOS) },
                        )
                        CalmIconTile(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Rounded.Folder,
                            label = "Folders",
                            selected = preferences.mediaViewMode == MediaViewMode.FOLDERS,
                            accent = accent,
                            accentContainer = accentContainer,
                            muted = muted,
                            onClick = { preferences = preferences.copy(mediaViewMode = MediaViewMode.FOLDERS) },
                        )
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 3.dp)
                                .width(1.dp)
                                .height(30.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                        )
                        CalmIconTile(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Rounded.ViewAgenda,
                            label = "List",
                            selected = preferences.mediaLayoutMode == MediaLayoutMode.LIST,
                            accent = accent,
                            accentContainer = accentContainer,
                            muted = muted,
                            onClick = { preferences = preferences.copy(mediaLayoutMode = MediaLayoutMode.LIST) },
                        )
                        CalmIconTile(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Rounded.GridView,
                            label = "Grid",
                            selected = preferences.mediaLayoutMode == MediaLayoutMode.GRID,
                            accent = accent,
                            accentContainer = accentContainer,
                            muted = muted,
                            onClick = { preferences = preferences.copy(mediaLayoutMode = MediaLayoutMode.GRID) },
                        )
                    }

                    Spacer(Modifier.height(14.dp))

                    // Sort Section
                    Text(
                        text = "Sort",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(innerTonal.copy(alpha = 0.65f))
                            .padding(vertical = 6.dp, horizontal = 4.dp),
                    ) {
                        CalmIconTile(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Rounded.SortByAlpha,
                            label = "Title",
                            selected = preferences.sortBy == Sort.By.TITLE,
                            accent = accent,
                            accentContainer = accentContainer,
                            muted = muted,
                            onClick = { preferences = preferences.copy(sortBy = Sort.By.TITLE) },
                        )
                        CalmIconTile(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Rounded.CalendarToday,
                            label = "Date",
                            selected = preferences.sortBy == Sort.By.DATE,
                            accent = accent,
                            accentContainer = accentContainer,
                            muted = muted,
                            onClick = { preferences = preferences.copy(sortBy = Sort.By.DATE) },
                        )
                        CalmIconTile(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Rounded.Timer,
                            label = "Length",
                            selected = preferences.sortBy == Sort.By.LENGTH,
                            accent = accent,
                            accentContainer = accentContainer,
                            muted = muted,
                            onClick = { preferences = preferences.copy(sortBy = Sort.By.LENGTH) },
                        )
                        CalmIconTile(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Rounded.SdStorage,
                            label = "Size",
                            selected = preferences.sortBy == Sort.By.SIZE,
                            accent = accent,
                            accentContainer = accentContainer,
                            muted = muted,
                            onClick = { preferences = preferences.copy(sortBy = Sort.By.SIZE) },
                        )
                        CalmIconTile(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Rounded.LocationOn,
                            label = "Path",
                            selected = preferences.sortBy == Sort.By.PATH,
                            accent = accent,
                            accentContainer = accentContainer,
                            muted = muted,
                            onClick = { preferences = preferences.copy(sortBy = Sort.By.PATH) },
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    // CALM / KIT Segmented Ascending / Descending pill
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(innerTonal.copy(alpha = 0.75f))
                            .padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        val isAsc = preferences.sortOrder == Sort.Order.ASCENDING
                        val ascBg by animateColorAsState(if (isAsc) accent else Color.Transparent, tween(150), label = "asc_bg")
                        val ascFg by animateColorAsState(if (isAsc) onAccent else muted, tween(150), label = "asc_fg")
                        val descBg by animateColorAsState(if (!isAsc) accent else Color.Transparent, tween(150), label = "desc_bg")
                        val descFg by animateColorAsState(if (!isAsc) onAccent else muted, tween(150), label = "desc_fg")

                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(11.dp))
                                .background(ascBg)
                                .clickable { preferences = preferences.copy(sortOrder = Sort.Order.ASCENDING) }
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Rounded.ArrowUpward, contentDescription = null, tint = ascFg, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(5.dp))
                            Text(
                                text = "A to Z",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = ascFg,
                            )
                        }
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(11.dp))
                                .background(descBg)
                                .clickable { preferences = preferences.copy(sortOrder = Sort.Order.DESCENDING) }
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Rounded.ArrowDownward, contentDescription = null, tint = descFg, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(5.dp))
                            Text(
                                text = "Z to A",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = descFg,
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    // Expandable Fields Pill Card
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(innerTonal.copy(alpha = 0.55f)),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { fieldsExpanded = !fieldsExpanded }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = "Fields",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Icon(
                                imageVector = if (fieldsExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                                contentDescription = null,
                                tint = muted,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        AnimatedVisibility(
                            visible = fieldsExpanded,
                            enter = expandVertically(spring(dampingRatio = 0.85f, stiffness = 420f)) + fadeIn(tween(140)),
                            exit = shrinkVertically(tween(130)) + fadeOut(tween(110)),
                        ) {
                            Column(
                                modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    CalmCheckCell(Modifier.weight(1f), "Thumbnail", preferences.showThumbnailField, accent, onAccent, muted) {
                                        preferences = preferences.copy(showThumbnailField = !preferences.showThumbnailField)
                                    }
                                    CalmCheckCell(Modifier.weight(1f), "Length", preferences.showDurationField, accent, onAccent, muted) {
                                        preferences = preferences.copy(showDurationField = !preferences.showDurationField)
                                    }
                                    CalmCheckCell(Modifier.weight(1f), "Ext", preferences.showExtensionField, accent, onAccent, muted) {
                                        preferences = preferences.copy(showExtensionField = !preferences.showExtensionField)
                                    }
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    CalmCheckCell(Modifier.weight(1f), "Played", preferences.showPlayedProgress, accent, onAccent, muted) {
                                        preferences = preferences.copy(showPlayedProgress = !preferences.showPlayedProgress)
                                    }
                                    CalmCheckCell(Modifier.weight(1f), "Res", preferences.showResolutionField, accent, onAccent, muted) {
                                        preferences = preferences.copy(showResolutionField = !preferences.showResolutionField)
                                    }
                                    CalmCheckCell(Modifier.weight(1f), "Path", preferences.showPathField, accent, onAccent, muted) {
                                        preferences = preferences.copy(showPathField = !preferences.showPathField)
                                    }
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    CalmCheckCell(Modifier.weight(1f), "Size", preferences.showSizeField, accent, onAccent, muted) {
                                        preferences = preferences.copy(showSizeField = !preferences.showSizeField)
                                    }
                                    CalmCheckCell(Modifier.weight(1f), "Folder", preferences.showFolderDurationField, accent, onAccent, muted) {
                                        preferences = preferences.copy(showFolderDurationField = !preferences.showFolderDurationField)
                                    }
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // Expandable Advanced Pill Card
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(innerTonal.copy(alpha = 0.55f)),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { advancedExpanded = !advancedExpanded }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = "Advanced",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Icon(
                                imageVector = if (advancedExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                                contentDescription = null,
                                tint = muted,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        AnimatedVisibility(
                            visible = advancedExpanded,
                            enter = expandVertically(spring(dampingRatio = 0.85f, stiffness = 420f)) + fadeIn(tween(140)),
                            exit = shrinkVertically(tween(130)) + fadeOut(tween(110)),
                        ) {
                            Column(
                                modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                CalmSwitchRow("Length over thumbnail", preferences.showDurationField, accent, onAccent) {
                                    preferences = preferences.copy(showDurationField = it)
                                }
                                CalmSwitchRow("Mark last played media", preferences.markLastPlayedMedia, accent, onAccent) {
                                    preferences = preferences.copy(markLastPlayedMedia = it)
                                }
                                CalmSwitchRow("Pause watch history", preferences.isHistoryPaused, accent, onAccent) {
                                    preferences = preferences.copy(isHistoryPaused = it)
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // CALM / KIT Action Footer
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(innerTonal)
                                .calmPressScale(0.93f).clickable(onClick = smoothDismiss)
                                .padding(horizontal = 18.dp, vertical = 9.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.cancel),
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = muted,
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(accent)
                                .calmPressScale(0.93f)
                                .clickable {
                                    updatePreferences(preferences)
                                    smoothDismiss()
                                }
                                .padding(horizontal = 22.dp, vertical = 9.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.okay),
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = onAccent,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalmIconTile(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    selected: Boolean,
    accent: Color,
    accentContainer: Color,
    muted: Color,
    onClick: () -> Unit,
) {
    val tileBg by animateColorAsState(
        targetValue = if (selected) accentContainer else Color.Transparent,
        animationSpec = tween(140),
        label = "calm_tile_bg",
    )
    val tint by animateColorAsState(
        targetValue = if (selected) accent else muted,
        animationSpec = tween(140),
        label = "calm_tile_tint",
    )
    Column(
        modifier = modifier
            .calmPressScale(pressedScale = 0.92f)
            .clip(RoundedCornerShape(14.dp))
            .background(tileBg)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(imageVector = icon, contentDescription = label, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = tint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun CalmCheckCell(
    modifier: Modifier = Modifier,
    label: String,
    checked: Boolean,
    accent: Color,
    onAccent: Color,
    muted: Color,
    onClick: () -> Unit,
) {
    val boxBg by animateColorAsState(
        targetValue = if (checked) accent else MaterialTheme.colorScheme.surfaceContainer,
        animationSpec = tween(130),
        label = "calm_chk_bg",
    )
    Row(
        modifier = modifier
            .calmPressScale(pressedScale = 0.94f)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(boxBg),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) {
                Icon(Icons.Rounded.Check, contentDescription = null, tint = onAccent, modifier = Modifier.size(13.dp))
            }
        }
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = if (checked) MaterialTheme.colorScheme.onSurface else muted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun CalmSwitchRow(
    title: String,
    checked: Boolean,
    accent: Color,
    onAccent: Color,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = onAccent,
                checkedTrackColor = accent,
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainer,
                uncheckedBorderColor = Color.Transparent,
            ),
        )
    }
}

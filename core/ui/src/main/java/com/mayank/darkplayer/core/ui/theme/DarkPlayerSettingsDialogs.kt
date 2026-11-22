package com.mayank.darkplayer.core.ui.theme

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.os.Environment
import android.os.LocaleList
import android.os.StatFs
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.animation.slideInVertically
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.os.LocaleListCompat
import java.io.File
import java.util.Locale

enum class DarkPlayerThemeMode(val label: String) {
    SYSTEM("System"),
    DARK("Dark"),
    LIGHT("Light"),
    AMOLED("AMOLED"),
}

enum class DarkPlayerPalette(
    val label: String,
    val accentDark: Color,
    val onAccentDark: Color,
    val accentLight: Color,
    val onAccentLight: Color,
    val swatchSecondary: Color,
) {
    CALM_SAGE(
        label = "Calm Sage",
        accentDark = Color(0xFFA7B8A7),
        onAccentDark = Color(0xFF0E130F),
        accentLight = Color(0xFF6B7F6E),
        onAccentLight = Color(0xFFF4EFE8),
        swatchSecondary = Color(0xFFC9BDAF),
    ),
    OBSIDIAN(
        label = "Obsidian",
        accentDark = Color(0xFFE8E8EE),
        onAccentDark = Color(0xFF0E0E10),
        accentLight = Color(0xFF1F2026),
        onAccentLight = Color(0xFFFFFFFF),
        swatchSecondary = Color(0xFF8A8A98),
    ),
    COBALT(
        label = "Cobalt",
        accentDark = Color(0xFF56A8FF),
        onAccentDark = Color(0xFF041A33),
        accentLight = Color(0xFF1668C7),
        onAccentLight = Color(0xFFFFFFFF),
        swatchSecondary = Color(0xFF2B6CB0),
    ),
    EMERALD(
        label = "Emerald",
        accentDark = Color(0xFF34D399),
        onAccentDark = Color(0xFF032318),
        accentLight = Color(0xFF0D8A5E),
        onAccentLight = Color(0xFFFFFFFF),
        swatchSecondary = Color(0xFF059669),
    ),
    AMBER(
        label = "Amber",
        accentDark = Color(0xFFFBBF24),
        onAccentDark = Color(0xFF241800),
        accentLight = Color(0xFFB4690E),
        onAccentLight = Color(0xFFFFFFFF),
        swatchSecondary = Color(0xFFD97706),
    ),
    VIOLET(
        label = "Violet",
        accentDark = Color(0xFFA78BFA),
        onAccentDark = Color(0xFF1A0D3D),
        accentLight = Color(0xFF6D42D8),
        onAccentLight = Color(0xFFFFFFFF),
        swatchSecondary = Color(0xFF7C3AED),
    ),
    CRIMSON(
        label = "Crimson",
        accentDark = Color(0xFFFB7185),
        onAccentDark = Color(0xFF330611),
        accentLight = Color(0xFFC82848),
        onAccentLight = Color(0xFFFFFFFF),
        swatchSecondary = Color(0xFFE11D48),
    ),
}

object DarkPlayerUiPrefs {
    private const val PREF_NAME = "darkplayer_ui_prefs"
    private const val KEY_THEME_MODE = "theme_mode"
    private const val KEY_PALETTE = "palette"
    private const val KEY_LIQUID_GLASS = "liquid_glass"
    private const val KEY_LANG = "language_tag"
    private const val KEY_PLAYER_BTN_DARK = "player_btn_dark"

    @Volatile
    private var loaded = false

    var themeMode by mutableStateOf(DarkPlayerThemeMode.DARK)
    var palette by mutableStateOf(DarkPlayerPalette.CALM_SAGE)
    var liquidGlassEnabled by mutableStateOf(true)
    var languageTag by mutableStateOf("")
    var playerButtonsDark by mutableStateOf(true)
    var activeModalBlurCount by mutableStateOf(0)

    fun pushModalBlur() { activeModalBlurCount += 1 }
    fun popModalBlur() { activeModalBlurCount = (activeModalBlurCount - 1).coerceAtLeast(0) }

    fun ensureLoaded(context: Context) {
        if (loaded) return
        loaded = true
        val sp = context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val savedMode = sp.getString(KEY_THEME_MODE, DarkPlayerThemeMode.DARK.name)
        val savedPalette = sp.getString(KEY_PALETTE, DarkPlayerPalette.CALM_SAGE.name)
        themeMode = DarkPlayerThemeMode.entries.firstOrNull { it.name == savedMode } ?: DarkPlayerThemeMode.DARK
        palette = DarkPlayerPalette.entries.firstOrNull { it.name == savedPalette } ?: DarkPlayerPalette.CALM_SAGE
        liquidGlassEnabled = sp.getBoolean(KEY_LIQUID_GLASS, true)
        languageTag = sp.getString(KEY_LANG, "") ?: ""
        if (!sp.getBoolean("v7_calm_migrated", false)) {
            palette = DarkPlayerPalette.CALM_SAGE
            playerButtonsDark = true
            sp.edit().putString(KEY_PALETTE, DarkPlayerPalette.CALM_SAGE.name).putBoolean(KEY_PLAYER_BTN_DARK, true).putBoolean("v7_calm_migrated", true).apply()
        } else {
            playerButtonsDark = sp.getBoolean(KEY_PLAYER_BTN_DARK, true)
        }
    }

    fun updateThemeMode(context: Context, mode: DarkPlayerThemeMode) {
        themeMode = mode
        context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_THEME_MODE, mode.name).apply()
    }

    fun updatePalette(context: Context, newPalette: DarkPlayerPalette) {
        palette = newPalette
        context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_PALETTE, newPalette.name).apply()
    }

    fun togglePlayerButtonsDark(context: Context): Boolean {
        playerButtonsDark = !playerButtonsDark
        context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_PLAYER_BTN_DARK, playerButtonsDark).apply()
        return playerButtonsDark
    }

    fun updateLiquidGlass(context: Context, enabled: Boolean) {
        liquidGlassEnabled = enabled
        context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_LIQUID_GLASS, enabled).apply()
    }

    private tailrec fun Context.findActivityOrNull(): Activity? = when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivityOrNull()
        else -> null
    }

    fun updateLanguage(context: Context, tag: String) {
        languageTag = tag
        context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_LANG, tag).apply()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            runCatching {
                val lm = context.getSystemService(LocaleManager::class.java)
                lm?.applicationLocales = if (tag.isBlank()) {
                    LocaleList.getEmptyLocaleList()
                } else {
                    LocaleList.forLanguageTags(tag)
                }
            }
        } else {
            val locales = if (tag.isBlank()) {
                LocaleListCompat.getEmptyLocaleList()
            } else {
                LocaleListCompat.forLanguageTags(tag)
            }
            AppCompatDelegate.setApplicationLocales(locales)
            val locale = if (tag.isBlank()) Locale.getDefault() else Locale.forLanguageTag(tag)
            Locale.setDefault(locale)
            val res = context.resources
            val config = res.configuration
            config.setLocale(locale)
            @Suppress("DEPRECATION")
            res.updateConfiguration(config, res.displayMetrics)
        }
        context.findActivityOrNull()?.recreate()
    }
}

@Composable
private fun DarkModalFrame(
    title: String,
    subtitle: String,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    val surfaceColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
    val chipBg = MaterialTheme.colorScheme.surfaceContainerHighest

    var entered by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val smoothDismiss: () -> Unit = { scope.launch { entered = false; delay(120); onDismiss() } }
    DisposableEffect(Unit) {
        DarkPlayerUiPrefs.pushModalBlur()
        onDispose { DarkPlayerUiPrefs.popModalBlur() }
    }
    LaunchedEffect(Unit) { entered = true }

    Dialog(
        onDismissRequest = smoothDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        val dialogView = LocalView.current
        SideEffect {
            val window = (dialogView.parent as? DialogWindowProvider)?.window
            if (window != null) {
                window.setDimAmount(0.45f)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                    window.attributes = window.attributes.apply {
                        blurBehindRadius = 48
                    }
                }
            }
        }
        AnimatedVisibility(
            visible = entered,
            enter = fadeIn(tween(160)) +
                scaleIn(initialScale = 0.88f, animationSpec = spring(dampingRatio = 0.80f, stiffness = 460f)) +
                slideInVertically(initialOffsetY = { it / 12 }, animationSpec = spring(dampingRatio = 0.84f, stiffness = 440f)),
            exit = fadeOut(tween(115)) + scaleOut(targetScale = 0.90f, animationSpec = tween(115)),
        ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
            shape = RoundedCornerShape(28.dp),
            color = surfaceColor,
            tonalElevation = 0.dp,
            shadowElevation = 18.dp,
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(chipBg)
                            .clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(chipBg)
                            .clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                HorizontalDivider(color = borderColor)
                Spacer(Modifier.height(16.dp))
                content()
            }
        }
        }
    }
}

@Composable
fun DarkPlayerAppearanceDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    DarkPlayerUiPrefs.ensureLoaded(context)
    val accent = MaterialTheme.colorScheme.primary
    val onAccent = MaterialTheme.colorScheme.onPrimary
    val pillBg = MaterialTheme.colorScheme.surfaceContainerHighest

    DarkModalFrame(
        title = "Appearance & Theme",
        subtitle = "Theme mode, color palette and UI style",
        onDismiss = onDismiss,
    ) {
        // 1. Liquid Glass Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Liquid Glass",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Frosted, see-through navigation bar with depth & blur",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(12.dp))
            Switch(
                checked = DarkPlayerUiPrefs.liquidGlassEnabled,
                onCheckedChange = { DarkPlayerUiPrefs.updateLiquidGlass(context, it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = onAccent,
                    checkedTrackColor = accent,
                ),
            )
        }

        Spacer(Modifier.height(16.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
        Spacer(Modifier.height(16.dp))

        // 2. Theme Mode
        Text(
            text = "Theme mode",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = "Choose how Dark Player looks",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DarkPlayerThemeMode.entries.forEach { mode ->
                val selected = DarkPlayerUiPrefs.themeMode == mode
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (selected) accent else pillBg)
                        .clickable { DarkPlayerUiPrefs.updateThemeMode(context, mode) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = mode.label,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        ),
                        color = if (selected) onAccent else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                    )
                }
            }
        }

        Spacer(Modifier.height(18.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
        Spacer(Modifier.height(16.dp))

        // 3. Color Palette
        Text(
            text = "Color palette",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = "High-contrast accent combinations",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            DarkPlayerPalette.entries.forEach { pal ->
                val selected = DarkPlayerUiPrefs.palette == pal
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { DarkPlayerUiPrefs.updatePalette(context, pal) }
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(pal.accentDark, pal.swatchSecondary),
                                ),
                            )
                            .then(
                                if (selected) {
                                    Modifier.border(2.5.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                } else {
                                    Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                                },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (selected) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                tint = pal.onAccentDark,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = pal.label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        ),
                        color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private data class AppLanguageOption(val tag: String, val nativeName: String, val subtitle: String)

private val DARK_PLAYER_LANGUAGES = listOf(
    AppLanguageOption("", "Auto (System default)", "Follow device language"),
    AppLanguageOption("en", "English", "English"),
    AppLanguageOption("hi", "हिन्दी", "Hindi"),
    AppLanguageOption("es", "Español", "Spanish"),
    AppLanguageOption("fr", "Français", "French"),
    AppLanguageOption("de", "Deutsch", "German"),
    AppLanguageOption("ar", "العربية", "Arabic"),
    AppLanguageOption("pt", "Português", "Portuguese"),
    AppLanguageOption("ru", "Русский", "Russian"),
    AppLanguageOption("ja", "日本語", "Japanese"),
    AppLanguageOption("ko", "한국어", "Korean"),
    AppLanguageOption("id", "Bahasa Indonesia", "Indonesian"),
    AppLanguageOption("tr", "Türkçe", "Turkish"),
    AppLanguageOption("zh", "中文", "Chinese"),
)

@Composable
fun DarkPlayerLanguageDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    DarkPlayerUiPrefs.ensureLoaded(context)
    var query by remember { mutableStateOf("") }
    val filtered = remember(query) {
        val q = query.trim().lowercase(Locale.ROOT)
        if (q.isEmpty()) DARK_PLAYER_LANGUAGES
        else DARK_PLAYER_LANGUAGES.filter {
            it.nativeName.lowercase(Locale.ROOT).contains(q) ||
                it.subtitle.lowercase(Locale.ROOT).contains(q)
        }
    }
    val accent = MaterialTheme.colorScheme.primary
    val onAccent = MaterialTheme.colorScheme.onPrimary

    DarkModalFrame(
        title = "Language",
        subtitle = "Choose application language",
        onDismiss = onDismiss,
    ) {
        // Search box
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(10.dp))
            Box(modifier = Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text(
                        text = "Search languages...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    textStyle = TextStyle(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp,
                        fontFamily = DarkPlayerFontFamily,
                    ),
                    cursorBrush = SolidColor(accent),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 360.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(filtered, key = { it.tag }) { lang ->
                val selected = DarkPlayerUiPrefs.languageTag == lang.tag
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (selected) accent.copy(alpha = 0.16f)
                            else MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.65f),
                        )
                        .then(
                            if (selected) Modifier
                            else Modifier,
                        )
                        .clickable {
                            DarkPlayerUiPrefs.updateLanguage(context, lang.tag)
                            Toast.makeText(context, "Language: ${lang.nativeName}", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = lang.nativeName,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (selected) accent else MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = lang.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (selected) {
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(accent),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                tint = onAccent,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun dirSizeBytes(file: File?): Long {
    if (file == null || !file.exists()) return 0L
    if (file.isFile) return file.length()
    var sum = 0L
    file.listFiles()?.forEach { sum += dirSizeBytes(it) }
    return sum
}

private fun formatBytesReadable(bytes: Long): String {
    if (bytes < 1024L) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024.0) return String.format(Locale.US, "%.1f KB", kb)
    val mb = kb / 1024.0
    if (mb < 1024.0) return String.format(Locale.US, "%.1f MB", mb)
    return String.format(Locale.US, "%.1f GB", mb / 1024.0)
}

@Composable
fun DarkPlayerStorageDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var refreshTick by remember { mutableIntStateOf(0) }
    val accent = MaterialTheme.colorScheme.primary

    val stats = remember(refreshTick) {
        val statFs = runCatching { StatFs(Environment.getDataDirectory().path) }.getOrNull()
        val totalBytes = statFs?.totalBytes ?: 1L
        val freeBytes = statFs?.availableBytes ?: 0L
        val usedBytes = (totalBytes - freeBytes).coerceAtLeast(0L)

        val cacheBytes = dirSizeBytes(context.cacheDir) +
            dirSizeBytes(context.externalCacheDir) +
            dirSizeBytes(context.codeCacheDir)
        val dataBytes = dirSizeBytes(context.filesDir)
        val apkBytes = runCatching { File(context.applicationInfo.sourceDir).length() }.getOrDefault(0L)
        val appTotal = apkBytes + cacheBytes + dataBytes

        arrayOf(totalBytes, usedBytes, freeBytes, apkBytes, cacheBytes, dataBytes, appTotal)
    }

    val totalBytes = stats[0].coerceAtLeast(1L)
    val usedBytes = stats[1]
    val freeBytes = stats[2]
    val apkBytes = stats[3]
    val cacheBytes = stats[4]
    val dataBytes = stats[5]
    val appTotal = stats[6]
    val usedFraction = (usedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0.02f, 1f)

    DarkModalFrame(
        title = "Storage Usage",
        subtitle = "See phone storage & clear Dark Player cache",
        onDismiss = onDismiss,
    ) {
        Text(
            text = "Device storage",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(usedFraction)
                    .height(10.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(accent, MaterialTheme.colorScheme.onSurface),
                        ),
                    ),
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "${formatBytesReadable(usedBytes)} used of ${formatBytesReadable(totalBytes)}",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "${formatBytesReadable(freeBytes)} free",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(16.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
        Spacer(Modifier.height(16.dp))

        Text(
            text = "Used by Dark Player",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Total space: ${formatBytesReadable(appTotal)}",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = accent,
        )

        Spacer(Modifier.height(14.dp))
        StorageMetricRow("App & Core engine", formatBytesReadable(apkBytes))
        Spacer(Modifier.height(10.dp))
        StorageMetricRow("Video & thumbnail cache", formatBytesReadable(cacheBytes))
        Spacer(Modifier.height(10.dp))
        StorageMetricRow("Saved offline data & config", formatBytesReadable(dataBytes))

        Spacer(Modifier.height(20.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(accent.copy(alpha = 0.16f))
                
                .clickable {
                    runCatching {
                        context.cacheDir?.listFiles()?.forEach { it.deleteRecursively() }
                        context.externalCacheDir?.listFiles()?.forEach { it.deleteRecursively() }
                        context.codeCacheDir?.listFiles()?.forEach { it.deleteRecursively() }
                    }
                    refreshTick++
                    Toast.makeText(context, "Dark Player cache cleared", Toast.LENGTH_SHORT).show()
                }
                .padding(vertical = 14.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.DeleteOutline,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Clear Dark Player Cache",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = accent,
            )
        }
    }
}

@Composable
private fun StorageMetricRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

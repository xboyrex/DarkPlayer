package dev.xboyrex.darkplayer.core.ui.display

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlin.math.cos
import kotlin.math.sin

enum class NovaColourProfile(
    val id: String,
    val title: String,
    val saturation: Float,
    val contrast: Float,
    val brightnessOffset: Float,
    val redScale: Float = 1f,
    val greenScale: Float = 1f,
    val blueScale: Float = 1f,
) {
    VIVID("vivid", "Vivid", saturation = 1.32f, contrast = 1.12f, brightnessOffset = 6f, redScale = 1.04f, greenScale = 1.02f, blueScale = 1.05f),
    SATURATED("saturated", "Saturated", saturation = 1.48f, contrast = 1.16f, brightnessOffset = 4f, redScale = 1.06f, greenScale = 1.03f, blueScale = 1.06f),
    STANDARD("standard", "Standard", saturation = 1.08f, contrast = 1.04f, brightnessOffset = 2f),
    ORIGINAL("original", "Original", saturation = 1.0f, contrast = 1.0f, brightnessOffset = 0f),
    P3("p3", "P3", saturation = 1.24f, contrast = 1.08f, brightnessOffset = 3f, redScale = 1.05f, greenScale = 1.01f, blueScale = 1.03f),
    SRGB("srgb", "sRGB", saturation = 0.98f, contrast = 1.02f, brightnessOffset = 0f, redScale = 0.99f, greenScale = 1.0f, blueScale = 1.0f),
}

enum class RealityEngineMode(
    val id: String,
    val title: String,
    val saturationBoost: Float,
    val contrastBoost: Float,
    val brightnessBoost: Float,
    val rMult: Float = 1f,
    val gMult: Float = 1f,
    val bMult: Float = 1f,
) {
    DEFAULT("default", "Default", saturationBoost = 1.0f, contrastBoost = 1.0f, brightnessBoost = 0f),
    X_REALITY("x_reality", "X-Reality engine", saturationBoost = 1.22f, contrastBoost = 1.18f, brightnessBoost = 5f, rMult = 1.03f, gMult = 1.02f, bMult = 1.04f),
    VIVID_DISPLAY("vivid_display", "Vivid display", saturationBoost = 1.38f, contrastBoost = 1.15f, brightnessBoost = 10f, rMult = 1.05f, gMult = 1.03f, bMult = 1.06f),
    TRILUMINOUS("triluminous", "Triluminous display", saturationBoost = 1.30f, contrastBoost = 1.12f, brightnessBoost = 4f, rMult = 1.08f, gMult = 1.04f, bMult = 1.02f),
    NATURAL("natural", "Natural display", saturationBoost = 1.05f, contrastBoost = 1.05f, brightnessBoost = 1f, rMult = 1.01f, gMult = 1.01f, bMult = 0.99f),
    CINEMA("cinema", "Cinema display", saturationBoost = 1.12f, contrastBoost = 1.14f, brightnessBoost = -2f, rMult = 1.04f, gMult = 1.00f, bMult = 0.94f),
    COMFORT("comfort", "Comfort display (Reading mode)", saturationBoost = 0.85f, contrastBoost = 0.96f, brightnessBoost = -4f, rMult = 1.06f, gMult = 0.97f, bMult = 0.82f),
}

object RealityDisplayEngine {
    private const val PREFS_NAME = "nova_reality_display_engine"
    private const val KEY_COLOUR_PROFILE = "colour_profile"
    private const val KEY_ENGINE_MODE = "engine_mode"

    private var prefs: SharedPreferences? = null

    var colourProfile by mutableStateOf(NovaColourProfile.VIVID)
        private set

    var engineMode by mutableStateOf(RealityEngineMode.DEFAULT)
        private set

    val isProcessingActive: Boolean
        get() = colourProfile != NovaColourProfile.ORIGINAL || engineMode != RealityEngineMode.DEFAULT

    fun init(context: Context) {
        if (prefs != null) return
        val sp = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs = sp
        val savedColour = sp.getString(KEY_COLOUR_PROFILE, NovaColourProfile.VIVID.id)
        val savedEngine = sp.getString(KEY_ENGINE_MODE, RealityEngineMode.DEFAULT.id)
        colourProfile = NovaColourProfile.entries.find { it.id == savedColour } ?: NovaColourProfile.VIVID
        engineMode = RealityEngineMode.entries.find { it.id == savedEngine } ?: RealityEngineMode.DEFAULT
    }

    fun selectColourProfile(profile: NovaColourProfile) {
        colourProfile = profile
        prefs?.edit()?.putString(KEY_COLOUR_PROFILE, profile.id)?.apply()
    }

    fun selectEngineMode(mode: RealityEngineMode) {
        engineMode = mode
        prefs?.edit()?.putString(KEY_ENGINE_MODE, mode.id)?.apply()
    }

    /**
     * Builds a composite 4x5 Compose ColorMatrix combining the active Colour Profile
     * (Vivid, Saturated, Standard, Original, P3, sRGB) and the active Sony Bravia-inspired
     * Reality Display Engine mode (X-Reality, Vivid, Triluminous, Natural, Cinema, Comfort).
     */
    fun buildColorMatrix(): ColorMatrix {
        val totalSat = (colourProfile.saturation * engineMode.saturationBoost).coerceIn(0f, 2.5f)
        val totalContrast = (colourProfile.contrast * engineMode.contrastBoost).coerceIn(0.5f, 2.0f)
        val totalBrightness = (colourProfile.brightnessOffset + engineMode.brightnessBoost).coerceIn(-40f, 40f)
        val rScale = colourProfile.redScale * engineMode.rMult
        val gScale = colourProfile.greenScale * engineMode.gMult
        val bScale = colourProfile.blueScale * engineMode.bMult

        val satMatrix = ColorMatrix().apply { setToSaturation(totalSat) }
        val translate = (1f - totalContrast) * 128f + totalBrightness
        val contrastAndTintMatrix = ColorMatrix(
            floatArrayOf(
                totalContrast * rScale, 0f, 0f, 0f, translate,
                0f, totalContrast * gScale, 0f, 0f, translate,
                0f, 0f, totalContrast * bScale, 0f, translate,
                0f, 0f, 0f, 1f, 0f,
            )
        )
        satMatrix.timesAssign(contrastAndTintMatrix)
        return satMatrix
    }
}

/**
 * Applies the live Reality Display Engine + Colour Profile matrix to any Composable
 * (including TextureView video surfaces and preview canvases).
 */
fun Modifier.applyRealityDisplayEngine(): Modifier = this.drawWithContent {
    if (!RealityDisplayEngine.isProcessingActive) {
        drawContent()
        return@drawWithContent
    }
    val matrix = RealityDisplayEngine.buildColorMatrix()
    val paint = Paint().apply {
        colorFilter = ColorFilter.colorMatrix(matrix)
    }
    drawIntoCanvas { canvas ->
        canvas.saveLayer(Rect(0f, 0f, size.width, size.height), paint)
        drawContent()
        canvas.restore()
    }
}

private val CoralAccent = Color(0xFFF4A298)
private val CardSurfaceDark = Color(0xFF141618)
private val DialogSurfaceDark = Color(0xFF1A1C1E)

@Composable
fun ColoursAndDisplayEngineContent(
    modifier: Modifier = Modifier,
    showPreviewBanner: Boolean = true,
) {
    var showEngineDialog by remember { mutableStateOf(false) }
    val currentColour = RealityDisplayEngine.colourProfile
    val currentEngine = RealityDisplayEngine.engineMode

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        if (showPreviewBanner) {
            // Vivid Hot-Air Balloon / Bravia Spectrum Live Preview Card (matches 1229.png & 1231.png)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(176.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .applyRealityDisplayEngine()
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.BottomStart,
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width * 0.52f, size.height * 0.65f)
                    val maxRadius = size.width * 0.85f
                    val sliceColors = listOf(
                        Color(0xFFE53935),
                        Color(0xFFFB8C00),
                        Color(0xFFFDD835),
                        Color(0xFF43A047),
                        Color(0xFF1E88E5),
                        Color(0xFF3949AB),
                        Color(0xFF8E24AA),
                        Color(0xFFD81B60),
                        Color(0xFFE53935),
                        Color(0xFFFB8C00),
                        Color(0xFFFDD835),
                        Color(0xFF43A047),
                    )
                    val sweep = 360f / sliceColors.size
                    sliceColors.forEachIndexed { idx, col ->
                        drawArc(
                            color = col,
                            startAngle = idx * sweep,
                            sweepAngle = sweep + 1f,
                            useCenter = true,
                            topLeft = Offset(center.x - maxRadius, center.y - maxRadius),
                            size = androidx.compose.ui.geometry.Size(maxRadius * 2f, maxRadius * 2f),
                        )
                    }
                    // Concentric balloon ribs & structural lines
                    for (r in 1..5) {
                        drawCircle(
                            color = Color.Black.copy(alpha = 0.22f),
                            radius = (maxRadius / 5f) * r,
                            center = center,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx()),
                        )
                    }
                    for (i in sliceColors.indices) {
                        val rad = Math.toRadians((i * sweep).toDouble())
                        drawLine(
                            color = Color.White.copy(alpha = 0.35f),
                            start = center,
                            end = Offset(
                                center.x + (maxRadius * cos(rad)).toFloat(),
                                center.y + (maxRadius * sin(rad)).toFloat(),
                            ),
                            strokeWidth = 1.5.dp.toPx(),
                        )
                    }
                    drawCircle(
                        color = Color(0xFFECEFF1),
                        radius = 12.dp.toPx(),
                        center = center,
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.72f))
                            )
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "${currentColour.title} • ${currentEngine.title}",
                            color = Color.White,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "LIVE PREVIEW",
                            color = CoralAccent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Page indicator dots + chevron row (matches 1231.png)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
            ) {
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(6.dp).clip(CircleShape).background(Color.White))
                    Box(Modifier.size(6.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.38f)))
                    Box(Modifier.size(6.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.38f)))
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.75f),
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 8.dp)
                        .size(22.dp),
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
        }

        // 6 Segmented Colour Profile Radio Cards (Vivid, Saturated, Standard, Original, P3, sRGB)
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            NovaColourProfile.entries.forEachIndexed { index, profile ->
                val isSelected = currentColour == profile
                val shape = when (index) {
                    0 -> RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
                    NovaColourProfile.entries.lastIndex -> RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 20.dp, bottomEnd = 20.dp)
                    else -> RoundedCornerShape(4.dp)
                }
                val bg by animateColorAsState(
                    targetValue = if (isSelected) Color(0xFF1C1E22) else CardSurfaceDark,
                    animationSpec = tween(180),
                    label = "colourCardBg",
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shape)
                        .background(bg)
                        .clickable { RealityDisplayEngine.selectColourProfile(profile) }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = { RealityDisplayEngine.selectColourProfile(profile) },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = CoralAccent,
                            unselectedColor = Color(0xFF9E9E9E),
                        ),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = profile.title,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(26.dp))

        // Display engine section header (matches 1231.png)
        Text(
            text = "Display engine",
            color = CoralAccent,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(start = 6.dp, bottom = 10.dp),
        )

        // Reality display engine card (matches 1231.png)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(CardSurfaceDark)
                .clickable { showEngineDialog = true }
                .padding(horizontal = 20.dp, vertical = 18.dp),
        ) {
            Text(
                text = "Reality display engine",
                color = Color.White,
                fontSize = 16.5.sp,
                fontWeight = FontWeight.Medium,
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = currentEngine.title,
                color = Color(0xFF9E9E9E),
                fontSize = 14.sp,
            )
        }

        Spacer(modifier = Modifier.height(22.dp))

        // Info icon + Bravia/X-Reality description (matches 1231.png verbatim)
        Icon(
            imageVector = Icons.Outlined.Info,
            contentDescription = null,
            tint = Color(0xFFB0B0B0),
            modifier = Modifier
                .padding(start = 4.dp)
                .size(22.dp),
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "Inspired by Sony's Bravia/X-Reality engine, the Display Engine features Vivid mode for enhanced brightness and color saturation, X-Reality mode for improved detail and color accuracy, and Triluminous mode for a wider color range.",
            color = Color(0xFFA8A8A8),
            fontSize = 13.5.sp,
            lineHeight = 19.sp,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        Spacer(modifier = Modifier.height(24.dp))
    }

    if (showEngineDialog) {
        RealityDisplayEngineDialog(
            selectedMode = currentEngine,
            onSelectMode = {
                RealityDisplayEngine.selectEngineMode(it)
                showEngineDialog = false
            },
            onDismiss = { showEngineDialog = false },
        )
    }
}

@Composable
fun RealityDisplayEngineDialog(
    selectedMode: RealityEngineMode,
    onSelectMode: (RealityEngineMode) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 380.dp),
            shape = RoundedCornerShape(28.dp),
            color = DialogSurfaceDark,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 24.dp),
            ) {
                Text(
                    text = "Reality display engine",
                    color = Color.White,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(start = 8.dp, bottom = 14.dp),
                )

                RealityEngineMode.entries.forEach { mode ->
                    val isSelected = mode == selectedMode
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onSelectMode(mode) }
                            .padding(horizontal = 6.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { onSelectMode(mode) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = CoralAccent,
                                unselectedColor = Color(0xFF9E9E9E),
                            ),
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = mode.title,
                            color = Color.White,
                            fontSize = 16.sp,
                            lineHeight = 21.sp,
                        )
                    }
                }
            }
        }
    }
}

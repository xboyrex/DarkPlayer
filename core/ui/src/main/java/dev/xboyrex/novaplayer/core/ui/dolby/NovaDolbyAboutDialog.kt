package dev.xboyrex.novaplayer.core.ui.dolby

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AlternateEmail
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.Speaker
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.xboyrex.novaplayer.core.ui.R
import dev.xboyrex.novaplayer.core.ui.glass.NovaRoundedHeavy
import dev.xboyrex.novaplayer.core.ui.glass.novaPressable
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Immutable
data class NovaDeveloperSocial(
    val title: String,
    val handle: String,
    val subtitle: String,
    val url: String,
    val icon: ImageVector,
    val accent: Color,
)

private val avatarCache = LruCache<String, Bitmap>(16)

private suspend fun fetchGithubAvatar(username: String): Bitmap? = withContext(Dispatchers.IO) {
    if (username.isBlank()) return@withContext null
    avatarCache.get(username)?.let { return@withContext it }
    var connection: HttpURLConnection? = null
    try {
        val url = URL("https://github.com/$username.png?size=128")
        connection = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 8000
            readTimeout = 10000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "NovaPlayer/2.5.0")
        }
        if (connection.responseCode == HttpURLConnection.HTTP_OK) {
            connection.inputStream.use { input ->
                val bmp = BitmapFactory.decodeStream(input)
                if (bmp != null) avatarCache.put(username, bmp)
                bmp
            }
        } else {
            null
        }
    } catch (_: Exception) {
        null
    } finally {
        connection?.disconnect()
    }
}

@Composable
fun NovaDolbyAboutDialog(
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val repoUrl = "https://github.com/xboyrex/NovaPlayer"

    val socials = listOf(
        NovaDeveloperSocial(
            title = "GitHub Profile",
            handle = "@xboyrex",
            subtitle = "Open-source repositories, releases & Android modding tools",
            url = "https://github.com/xboyrex",
            icon = Icons.Rounded.Code,
            accent = Color(0xFFA78BFA),
        ),
        NovaDeveloperSocial(
            title = "Telegram Channel",
            handle = "@MayankDroid7",
            subtitle = "Official Nova Player updates, builds & announcements",
            url = "https://t.me/MayankDroid7",
            icon = Icons.Rounded.Send,
            accent = Color(0xFF38BDF8),
        ),
        NovaDeveloperSocial(
            title = "Onyx Hub Community",
            handle = "@OnyxHub7",
            subtitle = "Discussion group, bug reports & feature requests",
            url = "https://t.me/OnyxHub7",
            icon = Icons.Rounded.Forum,
            accent = Color(0xFF2DD4BF),
        ),
        NovaDeveloperSocial(
            title = "YouTube · Mayank Droid",
            handle = "@mayankdroid",
            subtitle = "Android modding, app showcases & technical tutorials",
            url = "https://youtube.com/@mayankdroid?si=slX8DDU0dpLSIYyY",
            icon = Icons.Rounded.PlayCircle,
            accent = Color(0xFFF43F5E),
        ),
        NovaDeveloperSocial(
            title = "YouTube · Onyx Droid",
            handle = "@onyxdroid89",
            subtitle = "Custom ROMs, audio mods & performance guides",
            url = "https://youtube.com/@onyxdroid89?si=9gxpZfa3bpI9PKku",
            icon = Icons.Rounded.PlayCircle,
            accent = Color(0xFFFB7185),
        ),
        NovaDeveloperSocial(
            title = "Instagram",
            handle = "@mayank.zx7",
            subtitle = "Connect with Mayank on Instagram",
            url = "https://www.instagram.com/mayank.zx7",
            icon = Icons.Rounded.PhotoCamera,
            accent = Color(0xFFE879F9),
        ),
        NovaDeveloperSocial(
            title = "Developer Email",
            handle = "xboyrex496@gmail.com",
            subtitle = "Direct contact for development & collaboration",
            url = "mailto:xboyrex496@gmail.com",
            icon = Icons.Rounded.AlternateEmail,
            accent = Color(0xFF60A5FA),
        ),
    )

    val openLink: (String) -> Unit = { url ->
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(148.dp)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF3B1D82),
                                    Color(0xFF6C2BD9),
                                    Color(0xFF9333EA),
                                ),
                            ),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Image(
                                painter = painterResource(R.drawable.ic_nova_logo),
                                contentDescription = "Nova Player Logo",
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape),
                            )
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color.White.copy(alpha = 0.16f),
                                modifier = Modifier.size(48.dp),
                            ) {
                                NovaDolbyVectorLogo(
                                    modifier = Modifier.padding(10.dp),
                                    leftColor = Color.White,
                                    rightColor = Color(0xFFE9D5FF),
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Nova Player · Dolby Studio",
                            fontFamily = NovaRoundedHeavy,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = Color.White,
                        )
                        Text(
                            text = "Credits, Architecture & Developer Info",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.82f),
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    // Lead Developer Highlight Card
                    item {
                        Text(
                            text = "Lead Developer & Architect",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 4.dp),
                        )
                    }

                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(24.dp))
                                .clickable { openLink("https://github.com/xboyrex") },
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            ),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                NovaGithubAvatar(
                                    username = "xboyrex",
                                    modifier = Modifier.size(58.dp),
                                    shape = RoundedCornerShape(18.dp),
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "ㅤᏞᴀᴢʏㅤᎷꫝʏᴀɴк まやんく",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    )
                                    Text(
                                        text = "Mayank · @xboyrex (he/him)",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.5.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Android modding - Web/ App ( developer - etc.. Creator of Nova Player, Liquid Glass UI & Nova Dolby Audio Engine.",
                                        fontSize = 11.5.sp,
                                        lineHeight = 16.sp,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Rounded.OpenInNew,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    }

                    // View Repository Card
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { openLink(repoUrl) },
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            ),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Surface(
                                    modifier = Modifier.size(42.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Rounded.Code,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(22.dp),
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Nova Player Open-Source Repository",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.5.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Text(
                                        text = "github.com/xboyrex/NovaPlayer · GPL-3.0 License",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Rounded.OpenInNew,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }

                    // Audio Engine Architecture Section
                    item {
                        Text(
                            text = "Dolby Atmos & Audio Engine Highlights",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 4.dp, top = 4.dp),
                        )
                    }

                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            ),
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                EngineFeatureRow(
                                    icon = Icons.Rounded.GraphicEq,
                                    title = "Hardware & Software DAP + 20-Band GEQ",
                                    desc = "Supports vendor Dolby DAP (libswdap.so / libhwdap.so) and Android DynamicsProcessing 10/15/20-band interactive frequency curve.",
                                )
                                EngineFeatureRow(
                                    icon = Icons.Rounded.Speaker,
                                    title = "Surround Virtualizer & Dialogue Enhancer",
                                    desc = "Headphone & speaker spatial virtualization (4–64 stereo widening) with vocal clarity enhancement (1–12).",
                                )
                                EngineFeatureRow(
                                    icon = Icons.Rounded.Equalizer,
                                    title = "Intelligent Equalizer & Bass Curves",
                                    desc = "Real-time Detailed, Balanced, and Warm IEQ targets plus Balanced, Sub Boost, and Punch bass curves.",
                                )
                            }
                        }
                    }

                    // Socials & Community Section
                    item {
                        Text(
                            text = "Connect with Mayank (@xboyrex)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 4.dp, top = 4.dp),
                        )
                    }

                    items(socials) { social ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .clickable { openLink(social.url) },
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            ),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Surface(
                                    modifier = Modifier.size(40.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    color = social.accent.copy(alpha = 0.16f),
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = social.icon,
                                            contentDescription = null,
                                            tint = social.accent,
                                            modifier = Modifier.size(21.dp),
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        Text(
                                            text = social.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                        Text(
                                            text = social.handle,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 12.sp,
                                            color = social.accent,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = social.subtitle,
                                        fontSize = 11.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Rounded.OpenInNew,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(17.dp),
                                )
                            }
                        }
                    }
                }

                // Footer Close Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth(0.55f)
                            .novaPressable(onClick = onDismiss),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Close", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun EngineFeatureRow(
    icon: ImageVector,
    title: String,
    desc: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(20.dp),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.5.sp,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = desc,
                fontSize = 11.5.sp,
                lineHeight = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun NovaGithubAvatar(
    username: String,
    modifier: Modifier = Modifier,
    iconSize: Dp = 28.dp,
    shape: Shape = RoundedCornerShape(16.dp),
) {
    val bitmapState = produceState<Bitmap?>(initialValue = avatarCache.get(username), username) {
        if (value == null) {
            value = fetchGithubAvatar(username)
        }
    }
    val bitmap = bitmapState.value
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = username,
            contentScale = ContentScale.Crop,
            modifier = modifier.clip(shape),
        )
    } else {
        Surface(
            modifier = modifier,
            shape = shape,
            color = MaterialTheme.colorScheme.primary,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Rounded.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(iconSize),
                )
            }
        }
    }
}

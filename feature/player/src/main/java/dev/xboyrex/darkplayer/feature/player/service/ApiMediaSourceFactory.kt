package dev.xboyrex.darkplayer.feature.player.service

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.extractor.DefaultExtractorsFactory
import dev.xboyrex.darkplayer.feature.player.utils.apiHeaders

private fun createSeekableExtractorsFactory(): DefaultExtractorsFactory =
    DefaultExtractorsFactory()
        .setConstantBitrateSeekingEnabled(true)
        .setConstantBitrateSeekingAlwaysEnabled(true)

/** Creates independent sources so headers follow an item, including manifests, segments and subtitles. */
@UnstableApi
internal class ApiMediaSourceFactory(
    private val context: Context,
    private val dataSourceFactory: DataSource.Factory,
) : MediaSource.Factory by DefaultMediaSourceFactory(
    context,
    createSeekableExtractorsFactory(),
).setDataSourceFactory(dataSourceFactory) {
    override fun createMediaSource(mediaItem: MediaItem): MediaSource =
        DefaultMediaSourceFactory(
            context,
            createSeekableExtractorsFactory(),
        )
            .setDataSourceFactory(
                RequestHeadersDataSourceFactory(
                    dataSourceFactory,
                    mediaItem.apiHeaders,
                ),
            )
            .createMediaSource(mediaItem)
}

@UnstableApi
internal class RequestHeadersDataSourceFactory(
    private val delegate: DataSource.Factory,
    private val headers: Map<String, String>,
) : DataSource.Factory {
    override fun createDataSource(): DataSource {
        val source = delegate.createDataSource()
        return object : DataSource by source {
            override fun open(dataSpec: DataSpec): Long = source.open(
                if (dataSpec.uri.scheme in listOf(
                        "http",
                        "https",
                    )
                ) {
                    dataSpec.buildUpon().setHttpRequestHeaders(
                        headers + dataSpec.httpRequestHeaders,
                    ).build()
                } else {
                    dataSpec
                },
            )
        }
    }
}

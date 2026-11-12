package dev.xboyrex.darkplayer.core.domain

import dev.xboyrex.darkplayer.core.data.repository.fake.FakeMediaRepository
import dev.xboyrex.darkplayer.core.data.repository.fake.FakePreferencesRepository
import dev.xboyrex.darkplayer.core.model.Folder
import dev.xboyrex.darkplayer.core.model.Video
import java.util.Date
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GetPopularFoldersUseCaseTest {

    private val mediaRepository = FakeMediaRepository()
    private val preferencesRepository = FakePreferencesRepository()
    private val getSortedVideosUseCase = GetSortedVideosUseCase(
        mediaRepository = mediaRepository,
        preferencesRepository = preferencesRepository,
        defaultDispatcher = Dispatchers.Unconfined,
    )
    private val getSortedFoldersUseCase = GetSortedFoldersUseCase(
        mediaRepository = mediaRepository,
        preferencesRepository = preferencesRepository,
        defaultDispatcher = Dispatchers.Unconfined,
    )
    private val getPopularFoldersUseCase = GetPopularFoldersUseCase(
        getSortedFoldersUseCase = getSortedFoldersUseCase,
        getSortedVideosUseCase = getSortedVideosUseCase,
        defaultDispatcher = Dispatchers.Unconfined,
    )

    @Test
    fun `popular folders rank by played count then latest play timestamp then video count`() = runTest {
        val folderA = Folder(name = "A", path = "/storage/emulated/0/A", videosCount = 2)
        val folderB = Folder(name = "B", path = "/storage/emulated/0/B", videosCount = 5)
        val folderC = Folder(name = "C", path = "/storage/emulated/0/C", videosCount = 1)
        mediaRepository.folders.addAll(listOf(folderA, folderB, folderC))

        mediaRepository.videos.addAll(
            listOf(
                Video.sample.copy(
                    id = 1,
                    path = "/storage/emulated/0/A/1.mp4",
                    parentPath = folderA.path,
                    lastPlayedAt = Date(1_000L),
                ),
                Video.sample.copy(
                    id = 2,
                    path = "/storage/emulated/0/A/2.mp4",
                    parentPath = folderA.path,
                    lastPlayedAt = Date(2_000L),
                ),
                Video.sample.copy(
                    id = 3,
                    path = "/storage/emulated/0/C/1.mp4",
                    parentPath = folderC.path,
                    lastPlayedAt = Date(9_000L),
                ),
            ),
        )

        val ranked = getPopularFoldersUseCase(limit = 3).first()
        assertEquals(
            listOf(folderA.path, folderC.path, folderB.path),
            ranked.map { it.path },
        )
    }
}

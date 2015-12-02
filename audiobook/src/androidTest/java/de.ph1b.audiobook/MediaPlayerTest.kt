package de.ph1b.audiobook

import android.app.Application
import android.os.Environment
import android.test.AndroidTestCase
import android.test.suitebuilder.annotation.MediumTest
import android.test.suitebuilder.annotation.SmallTest
import com.google.common.collect.ImmutableList
import com.google.common.collect.Lists
import com.google.common.io.ByteStreams
import dagger.Component
import de.ph1b.audiobook.injection.AndroidModule
import de.ph1b.audiobook.injection.BaseModule
import de.ph1b.audiobook.mediaplayer.MediaPlayerController
import de.ph1b.audiobook.model.Book
import de.ph1b.audiobook.model.Bookmark
import de.ph1b.audiobook.model.Chapter
import java.io.File
import java.io.FileOutputStream
import java.util.*
import java.util.concurrent.CountDownLatch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Simple test for our MediaPlayer.

 * @author Paul Woitaschek
 */
class MediaPlayerTest : AndroidTestCase () {

    @Inject internal lateinit var mediaPlayerController: MediaPlayerController
    lateinit var file1: File
    lateinit var file2: File
    lateinit var book: Book

    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()

        val mock: MockAppComponent = DaggerMediaPlayerTest_MockAppComponent.builder()
                .androidModule(AndroidModule(context.applicationContext as Application))
                .baseModule(BaseModule())
                .build()
        mock.inject(this)

        val externalStorage = Environment.getExternalStorageDirectory()

        file1 = File(externalStorage, "1.mp3")
        file2 = File(externalStorage, "2.mp3")

        ByteStreams.copy(context.assets.open("3rdState.mp3"), FileOutputStream(file1))
        ByteStreams.copy(context.assets.open("Crashed.mp3"), FileOutputStream(file2))

        val id = 1L
        val bookmarks = ArrayList<Bookmark>()
        val type = Book.Type.SINGLE_FILE
        val useCoverReplacement = false
        val author = "TestAuthor"
        val currentFile = file1
        val time = 0
        val name = "TestBook"
        val chapter1 = Chapter(file1, file1.name, 100000)
        val chapter2 = Chapter(file2, file2.name, 200000)
        val chapters = Lists.newArrayList(chapter1, chapter2)
        val playbackSpeed = 1F
        val root = Environment.getExternalStorageDirectory().path
        book = Book(id,
                ImmutableList.copyOf(bookmarks),
                type, useCoverReplacement,
                author,
                currentFile,
                time,
                name,
                ImmutableList.copyOf(chapters),
                playbackSpeed,
                root)

        mediaPlayerController.init(book)
    }

    override fun testAndroidTestCaseSetupProperly() {
        super.testAndroidTestCaseSetupProperly()

        checkNotNull(mediaPlayerController)
        check(file1.exists())
        check(file2.exists())
    }

    /**
     * Tests simple play pause controls
     */
    @SmallTest
    fun testSimplePlayback() {
        mediaPlayerController.play()
        check(mediaPlayerController.playState.value == MediaPlayerController.PlayState.PLAYING)
        Thread.sleep(1000)
        mediaPlayerController.pause(false)
        check(mediaPlayerController.playState.value == MediaPlayerController.PlayState.PAUSED)
    }

    private val rnd = Random()

    private fun playPauseRandom() {
        synchronized(mediaPlayerController, {
            if (rnd.nextBoolean()) {
                mediaPlayerController.play()
                check(mediaPlayerController.playState.value == MediaPlayerController.PlayState.PLAYING)
            } else {
                mediaPlayerController.pause(false)
                check(mediaPlayerController.playState.value == MediaPlayerController.PlayState.PAUSED)
            }
        })
    }

    /**
     * Tests for threading issues by letting two threads against each other
     */
    @MediumTest
    fun testThreading() {
        val commandsToExecute = 1..1000
        val readyLatch = CountDownLatch(2)
        val t1 = Thread(Runnable {
            readyLatch.countDown()
            readyLatch.await()

            for (i in commandsToExecute) {
                playPauseRandom()
            }
        })
        val t2 = Thread(Runnable {
            readyLatch.countDown()
            readyLatch.await()

            for (i in commandsToExecute) {
                playPauseRandom()
            }
        })

        t1.join()
        t2.join()
    }


    @Throws(Exception::class)
    override fun tearDown() {
        super.tearDown()

        file1.delete()
        file2.delete()
    }

    @Singleton
    @Component(modules = arrayOf(BaseModule::class, AndroidModule::class))
    interface MockAppComponent {

        fun inject(target: MediaPlayerTest)
    }
}

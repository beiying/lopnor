package com.beiying.lopnor

import android.Manifest
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.rule.GrantPermissionRule
import androidx.test.runner.AndroidJUnit4
import com.beiying.lopnor.base.utils.FileUtil
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Rule
    var mRuntimePermissionRule = GrantPermissionRule.grant(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )


    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext: Context = ApplicationProvider.getApplicationContext()
        assertEquals("com.beiying.lopnor.base.test", appContext.getPackageName())
    }

    @Test
    fun getAllVideo() {
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }
        val projection = arrayOf(
            MediaStore.Video.Media._ID, MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION, MediaStore.Video.Media.SIZE
        )
        val selection = MediaStore.Video.Media.DURATION + " >= ?"
        val selectionArgs = arrayOf("100")
        val sortOrder = MediaStore.Video.Media.DISPLAY_NAME + " ASC"
        val videos: List<FileUtil.Video> = FileUtil.getAllVideo(
            ApplicationProvider.getApplicationContext(),
            collection,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )
        assertEquals(videos.size, 8)
    }
}

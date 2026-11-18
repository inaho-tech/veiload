package dev.noirload.app

import android.app.Application
import android.util.Log
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import dev.noirload.app.download.DownloadsStore
import kotlin.concurrent.thread

class NoirLoadApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppLocale.initialize(this)
        DownloadsStore.scan(this)
        thread(name = "noir-engine-init") {
            runCatching {
                YoutubeDL.getInstance().init(this)
                FFmpeg.getInstance().init(this)
            }.onFailure { Log.e("NoirLoad", "Download engine initialization failed", it) }
        }
    }
}

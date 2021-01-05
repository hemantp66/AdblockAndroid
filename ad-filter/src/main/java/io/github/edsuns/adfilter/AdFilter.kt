package io.github.edsuns.adfilter

import android.app.Application
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import androidx.work.WorkInfo
import io.github.edsuns.adblockclient.ResourceType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Created by Edsuns@qq.com on 2020/10/24.
 */
class AdFilter internal constructor(application: Application) {

    private val detector: Detector = Detector()
    internal val binaryDataStore: BinaryDataStore =
        BinaryDataStore(File(application.filesDir, FILE_STORE_DIR))
    private val filterDataLoader: FilterDataLoader = FilterDataLoader(detector, binaryDataStore)
    val viewModel = FilterViewModel(application, filterDataLoader)

    val hasInstallation: Boolean
        get() = viewModel.sharedPreferences.hasInstallation

    init {
        viewModel.isEnabled.observeForever { enable ->
            if (enable) {
                viewModel.filters.value?.values?.forEach {
                    if (it.isEnabled && it.hasDownloaded()) {
                        filterDataLoader.load(it.id)
                    }
                }
            } else {
                filterDataLoader.unloadAll()
            }
            viewModel.sharedPreferences.isEnabled = enable
        }
        viewModel.workInfo.observeForever { list ->
            processWorkInfo(list)
        }
    }

    private fun processWorkInfo(list: List<WorkInfo>?) {
        list?.forEach { workInfo ->
            val state = workInfo.state
            val filterId = viewModel.downloadFilterIdMap[workInfo.id.toString()]
            viewModel.filters.value?.get(filterId)?.let {
                it.downloadState = when (state) {
                    WorkInfo.State.ENQUEUED -> DownloadState.ENQUEUED
                    WorkInfo.State.RUNNING -> {
                        val installing =
                            workInfo.progress.getBoolean(KEY_INSTALLING, false)
                        if (installing) DownloadState.INSTALLING else DownloadState.DOWNLOADING
                    }
                    WorkInfo.State.SUCCEEDED -> {
                        it.updateTime = System.currentTimeMillis()
                        if (it.isEnabled)
                            viewModel.enableFilter(it.id)
                        DownloadState.SUCCESS
                    }
                    WorkInfo.State.FAILED -> DownloadState.FAILED
                    WorkInfo.State.CANCELLED -> DownloadState.CANCELLED
                    else -> DownloadState.NONE
                }
                if (state.isFinished) {
                    viewModel.downloadFilterIdMap.remove(workInfo.id.toString())
                    // save shared preferences
                    viewModel.sharedPreferences.downloadFilterIdMap = viewModel.downloadFilterIdMap
                    viewModel.workManager.pruneWork()
                }
                viewModel.updateFilter(it)
            }
        }
    }

    /**
     * Notify the application of a resource request and allow the application to return the data.
     *
     * If the return value is null, the WebView will continue to load the resource as usual.
     * Otherwise, the return response and data will be used.
     *
     * NOTE: This method is called on a thread other than the UI thread so clients should exercise
     * caution when accessing private data or the view system.
     */
    fun shouldIntercept(
        webView: WebView,
        request: WebResourceRequest
    ): WebResourceResponse? {
        return runBlocking {
            if (request.isForMainFrame) {
                return@runBlocking null
            }

            val url = request.url.toString()
            val documentUrl = withContext(Dispatchers.Main) { webView.url }

            val shouldBlock = detector.shouldBlock(url, documentUrl, ResourceType.from(request))
            if (shouldBlock)
                WebResourceResponse(null, null, null)
            else
                null
        }
    }

    companion object {
        @Volatile
        private var instance: AdFilter? = null

        fun get(): AdFilter {
            if (instance == null) {
                throw RuntimeException("Should call create() before get()")
            }
            return instance!!
        }

        fun create(application: Application): AdFilter {
            return instance ?: synchronized(this) {
                instance = instance ?: AdFilter(application)
                instance!!
            }
        }
    }
}
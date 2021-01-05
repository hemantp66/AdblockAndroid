package io.github.edsuns.adfilter

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import io.github.edsuns.adblockclient.AdBlockClient
import io.github.edsuns.net.HttpRequest
import timber.log.Timber
import java.io.IOException

/**
 * Created by Edsuns@qq.com on 2021/1/1.
 */
class DownloadWorker(context: Context, params: WorkerParameters) : Worker(
    context,
    params
) {
    override fun doWork(): Result {
        val id = inputData.getString(KEY_FILTER_ID)
        val url = inputData.getString(KEY_DOWNLOAD_URL)
        Timber.v("DownloadWorker: start download $url $id")
        try {
            url?.let {
                id?.let {
                    val request = HttpRequest(url).get()
                    setProgressAsync(workDataOf(KEY_INSTALLING to true))
                    persistFilterData(it, request.bodyBytes)
                    return Result.success(inputData)
                }
            }
        } catch (e: IOException) {
            Timber.v(e, "DownloadWorker: failed to download $url $id")
        }
        return Result.failure(inputData)
    }

    private fun persistFilterData(id: String, bodyBytes: ByteArray) {
        val client = AdBlockClient(id)
        client.loadBasicData(bodyBytes)
        AdFilter.get().binaryDataStore.saveData(id, client.getProcessedData())
    }
}
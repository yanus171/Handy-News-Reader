package ru.yanus171.feedexfork.utils

import android.os.Build
import okhttp3.*

import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection

import org.jsoup.Jsoup
import org.w3c.dom.Document
import ru.yanus171.feedexfork.service.FetcherService

class Connection(url: String) {
    private var mConnection: HttpURLConnection? = null
    private var mResponse: Response? = null
    val inputStream: InputStream
        @Throws(IOException::class)
        get() = if (IsOkHttp())
            mResponse!!.body!!.byteStream()
        else
            mConnection!!.inputStream

    val contentLength: Long?
        get() = if (IsOkHttp()) {
            mResponse!!.body?.contentLength()
        } else {
            mConnection!!.contentLength.toLong()
        }

    val contentType: String?
        get() = if (IsOkHttp()) {
            mResponse!!.body?.contentType()?.type
        } else {
            mConnection!!.contentType
        }

    val parse: org.jsoup.nodes.Document?
        get() = if (IsOkHttp()) {
            Jsoup.parse(inputStream, null, "")
        } else {
            Jsoup.parse(inputStream, "UTF-8", mConnection?.url.toString())
        }

    init {
        if (IsOkHttp()) {
            val request = Request.Builder()
                    .url(url)
                    .build()

            val call = OkHttpClient().newCall(request)
            mResponse = call.execute()
        } else
            mConnection = NetworkUtils.setupConnection1(url)

    }

    fun disconnect() {
        if (IsOkHttp()) {
            mResponse?.close()
        } else {
            mConnection?.disconnect()
            mConnection = null
        }
    }

    companion object {

        fun IsOkHttp(): Boolean {
            return Build.VERSION.SDK_INT >= 21
        }
    }

}

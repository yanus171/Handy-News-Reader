package ru.yanus171.feedexfork.utils

import android.os.Build
import okhttp3.*
import org.jsoup.Jsoup
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.security.cert.X509Certificate
import java.security.SecureRandom
import java.util.concurrent.TimeUnit
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import javax.net.ssl.SSLContext

class Connection(url: String, var mIsOKHttp: Boolean = true) {
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
        val timeout = PrefUtils.getIntFromText( "connection_timeout", 10000 )

        if (IsOkHttp()) {

            fun OkHttpClient.Builder.ignoreAllSSLErrors(): OkHttpClient.Builder {
                val naiveTrustManager = object : X509TrustManager {
                    override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                    override fun checkClientTrusted(certs: Array<X509Certificate>, authType: String) = Unit
                    override fun checkServerTrusted(certs: Array<X509Certificate>, authType: String) = Unit
                }

                val insecureSocketFactory = SSLContext.getInstance("TLSv1.2").apply {
                    val trustAllCerts = arrayOf<TrustManager>(naiveTrustManager)
                    init(null, trustAllCerts, SecureRandom())
                }.socketFactory

                sslSocketFactory(insecureSocketFactory, naiveTrustManager)
                hostnameVerifier { _, _ -> true }
                return this
            }


            var request = Request.Builder()
                    .url(url.trim())
                    .build()
            val client = OkHttpClient.Builder()
                    .connectTimeout(timeout.toLong(), TimeUnit.MILLISECONDS)
                    .readTimeout(timeout.toLong(), TimeUnit.MILLISECONDS)
            if ( PrefUtils.getBoolean("ignore_all_ssl_errors", false) )
                client.apply {
                    ignoreAllSSLErrors()
                }
            var call = client.build().newCall(request)

            try {
                mResponse = call.execute()
            } catch (e: IOException) {
                e.printStackTrace();
                if ( url.startsWith("https") ) {
                    try {
                        mResponse?.close()
                        request = Request.Builder()
                                .url(url.replace("https", "http"))
                                .build()
                        call = OkHttpClient().newCall(request)
                        mResponse = call.execute()
                    } catch (e: IOException) {
                        disconnect();
                        mIsOKHttp = false;
                        mConnection = NetworkUtils.setupConnection(url.trim(), timeout)
                    }
                } else
                    throw e
            }

        } else
            mConnection = NetworkUtils.setupConnection(url.trim(), timeout)

    }

    fun disconnect() {
        if (IsOkHttp()) {
            mResponse?.close()
        } else {
            mConnection?.disconnect()
            mConnection = null
        }
    }

    fun getText(): String {
        if (IsOkHttp()) {
            return mResponse?.body!!.string()
        } else
            return "";
    }
    fun IsOkHttp(): Boolean {
        return mIsOKHttp && Build.VERSION.SDK_INT >= 21
    }
}

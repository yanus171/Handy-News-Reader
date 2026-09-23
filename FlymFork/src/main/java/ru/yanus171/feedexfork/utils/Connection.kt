package ru.yanus171.feedexfork.utils

import android.os.Build
import okhttp3.*
import org.jsoup.Jsoup
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.security.cert.X509Certificate
import java.security.SecureRandom
import java.util.concurrent.TimeUnit
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import javax.net.ssl.SSLContext

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

class Connection(url: String, var mIsOKHttp: Boolean = true) {
    private var mConnection: HttpURLConnection? = null
    private var mResponse: Response? = null

    companion object {
        // One shared OkHttp client: keeps a bounded keep-alive pool and never leaks
        // native sockets / file descriptors. A client per Connection() leaked pools
        // and caused EMFILE crashes ("Could not read input channel file descriptors
        // from parcel") on long refresh cycles.
        private var mSharedClient: OkHttpClient? = null
        private var mSharedClientConfig = ""

        // Rebuild the shared client whenever relevant preferences change
        // (timeouts, SSL override, or the opt-out flag itself).
        private fun clientConfig(): String {
            val timeout = PrefUtils.getIntFromText( "connection_timeout", 10000 )
            val ssl = PrefUtils.getBoolean("ignore_all_ssl_errors", false)
            val shared = PrefUtils.getBoolean("okhttp_shared_client", true)
            return "$timeout|$ssl|$shared"
        }

        private fun sharedClient(): OkHttpClient {
            val cfg = clientConfig()
            if (!PrefUtils.getBoolean("okhttp_shared_client", true)) {
                // Per-connection clients (old behaviour): no pooling, but each
                // new OkHttpClient also creates its own ConnectionPool. Used only
                // when the user opted out for compatibility on non-Huawei devices.
                return buildClient()
            }
            if (mSharedClient == null || mSharedClientConfig != cfg) {
                // Preferences changed since we built the client: drop pooled
                // connections so the new timeouts/SSL settings take effect.
                mSharedClient?.connectionPool?.evictAll()
                mSharedClient = buildClient()
                mSharedClientConfig = cfg
            }
            return mSharedClient!!
        }

        private fun buildClient(): OkHttpClient {
            val timeout = PrefUtils.getIntFromText( "connection_timeout", 10000 )
            val builder = OkHttpClient.Builder()
                    .connectTimeout(timeout.toLong(), TimeUnit.MILLISECONDS)
                    .readTimeout(timeout.toLong(), TimeUnit.MILLISECONDS)
            if ( PrefUtils.getBoolean("ignore_all_ssl_errors", false) )
                builder.ignoreAllSSLErrors()
            return builder.build()
        }
    }
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

            val client = sharedClient()
            var request = Request.Builder()
                    .url(url.trim())
                    .header( "user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/109.0.0.0 Safari/537.36" )
                    .header( "referer", NetworkUtils.getBaseUrl( url ) )
                            .build()
            var call = client.newCall(request)

            try {
                mResponse = call.execute()
            } catch (e: SocketTimeoutException) {
                throw e
            } catch (e: IOException) {
                e.printStackTrace();
                if ( url.startsWith("https") ) {
                    try {
                        mResponse?.close()
                        request = Request.Builder()
                                .url(url.replace("https", "http"))
                                .build()
                        call = sharedClient().newCall(request)
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

    fun getCode(): Int {
        return if (IsOkHttp())
            mResponse!!.code
        else
            mConnection!!.responseCode
    }

    fun IsOkHttp(): Boolean {
        return mIsOKHttp && Build.VERSION.SDK_INT >= 21
    }
}

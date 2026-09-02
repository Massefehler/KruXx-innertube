package me.knighthat.innertube

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import okhttp3.logging.HttpLoggingInterceptor

class InnertubeProvider: Innertube.KtorProvider {

    override val client = HttpClient( OkHttp ) {
        expectSuccess = true

        install(ContentNegotiation ) {
            json( JSON )
        }

        install( ContentEncoding ) {
            gzip( 1f )
            deflate( .1f )
        }

        engine {
            val interceptor = HttpLoggingInterceptor().setLevel( HttpLoggingInterceptor.Level.BODY )
            interceptor.redactHeader( "Cookie" )

            addInterceptor( interceptor )
        }
    }
    override val cookies: String = ""
    override val dataSyncId: String? = null
    override val visitorData: String = Constants.CHROME_WINDOWS_VISITOR_DATA
}

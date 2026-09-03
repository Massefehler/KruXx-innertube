package me.knighthat.internal

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.runBlocking
import me.knighthat.innertube.Innertube
import me.knighthat.innertube.JSON
import me.knighthat.innertube.request.Localization
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class InnertubeImplAuthenticationTest {

    @Test
    fun authenticatedBrowseForwardsLoginHeaders() = runBlocking {
        val request = executeBrowse( useLogin = true )

        assertEquals( "/youtubei/v1/browse", request.url.encodedPath )
        assertLoginHeaders( request )
    }

    @Test
    fun anonymousBrowseDoesNotForwardLoginHeaders() = runBlocking {
        val request = executeBrowse( useLogin = false )

        assertEquals( "/youtubei/v1/browse", request.url.encodedPath )
        assertNull( request.headers[HttpHeaders.Cookie] )
        assertNull( request.headers[HttpHeaders.Authorization] )
        assertNull( request.headers[VISITOR_HEADER] )
    }

    @Test
    fun authenticatedNextForwardsLoginHeaders() = runBlocking {
        lateinit var capturedRequest: HttpRequestData
        val client = mockClient( "ytm/next/endpoint_response.json" ) { capturedRequest = it }
        val innertube = InnertubeImpl().apply { setProvider( TestProvider(client) ) }

        try {
            innertube.ytmNext( Localization.EN_US, useLogin = true ) {
                videoId( "test-video-id" )
            }
        } finally {
            client.close()
        }

        assertEquals( "/youtubei/v1/next", capturedRequest.url.encodedPath )
        assertLoginHeaders( capturedRequest )
    }

    private suspend fun executeBrowse( useLogin: Boolean ): HttpRequestData {
        lateinit var capturedRequest: HttpRequestData
        val client = mockClient( "ytm/browse/endpoint_playlist_response.json" ) {
            capturedRequest = it
        }
        val innertube = InnertubeImpl().apply { setProvider( TestProvider(client) ) }

        try {
            innertube.ytmBrowse( Localization.EN_US, useLogin = useLogin ) {
                browseId( "VLtest-playlist-id" )
            }
        } finally {
            client.close()
        }

        return capturedRequest
    }

    private fun mockClient(
        responseResource: String,
        captureRequest: (HttpRequestData) -> Unit
    ): HttpClient {
        val response = requireNotNull(
            javaClass.classLoader.getResourceAsStream( responseResource )
        ).bufferedReader().use { it.readText() }

        return HttpClient(
            MockEngine { request ->
                captureRequest( request )
                respond(
                    content = ByteReadChannel(response),
                    status = HttpStatusCode.OK,
                    headers = headersOf( HttpHeaders.ContentType, ContentType.Application.Json.toString() )
                )
            }
        ) {
            expectSuccess = true
            install(ContentNegotiation ) {
                json( JSON )
            }
        }
    }

    private fun assertLoginHeaders( request: HttpRequestData ) {
        assertEquals( COOKIES, request.headers[HttpHeaders.Cookie] )
        assertEquals( VISITOR_DATA, request.headers[VISITOR_HEADER] )

        val authorization = requireNotNull( request.headers[HttpHeaders.Authorization] ) {
            "Authorization header is missing"
        }
        assertTrue(
            AUTHORIZATION_PATTERN.matches(authorization),
            "Unexpected Authorization header format"
        )
    }

    private class TestProvider(
        override val client: HttpClient
    ): Innertube.KtorProvider {

        override val cookies = COOKIES
        override val dataSyncId = "test-sync-id"
        override val visitorData = VISITOR_DATA
    }

    private companion object {
        const val COOKIES = "SAPISID=test-sapisid; SID=test-session"
        const val VISITOR_DATA = "test-visitor-data"
        const val VISITOR_HEADER = "X-Goog-Visitor-Id"
        val AUTHORIZATION_PATTERN = Regex( "SAPISIDHASH \\d+_[0-9a-f]{40}" )
    }
}

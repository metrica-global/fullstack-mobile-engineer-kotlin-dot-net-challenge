package com.fieldops.app.data.remote

import com.fieldops.app.data.local.TokenProvider
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class AuthInterceptorTest {

    @Test
    fun `intercept agrega el header Authorization Bearer a las peticiones protegidas`() {
        val tokenProvider = FakeTokenProvider(
            accessToken = "valid-access-token",
            refreshToken = "valid-refresh-token",
        )
        val interceptor = AuthInterceptor(
            tokenProvider = tokenProvider,
            tokenRefresher = { null },
        )
        val chain = FakeInterceptorChain(
            initialRequest = Request.Builder()
                .url("http://10.0.2.2:5080/api/workorders")
                .build(),
            responses = listOf(200),
        )

        val response = interceptor.intercept(chain)

        assertEquals(200, response.code)
        assertEquals(1, chain.proceededRequests.size)
        assertEquals(
            "Bearer valid-access-token",
            chain.proceededRequests.first().header("Authorization"),
        )
        assertFalse(tokenProvider.sessionExpiredCalled)
    }

    @Test
    fun `intercept renueva el token una vez al recibir 401 y repite la peticion con el nuevo token`() {
        val tokenProvider = FakeTokenProvider(
            accessToken = "expired-access-token",
            refreshToken = "valid-refresh-token",
        )
        var refreshCallCount = 0
        val interceptor = AuthInterceptor(
            tokenProvider = tokenProvider,
            tokenRefresher = { refreshToken ->
                refreshCallCount++
                assertEquals("valid-refresh-token", refreshToken)
                "new-access-token" to "new-refresh-token"
            },
        )
        val chain = FakeInterceptorChain(
            initialRequest = Request.Builder()
                .url("http://10.0.2.2:5080/api/workorders")
                .build(),
            responses = listOf(401, 200),
        )

        val response = interceptor.intercept(chain)

        assertEquals(200, response.code)
        assertEquals(1, refreshCallCount)
        assertEquals(2, chain.proceededRequests.size)
        assertEquals(
            "Bearer expired-access-token",
            chain.proceededRequests[0].header("Authorization"),
        )
        assertEquals(
            "Bearer new-access-token",
            chain.proceededRequests[1].header("Authorization"),
        )
        assertEquals("new-access-token", tokenProvider.getAccessToken())
        assertEquals("new-refresh-token", tokenProvider.getRefreshToken())
        assertFalse(tokenProvider.sessionExpiredCalled)
    }

    @Test
    fun `intercept limpia la sesion y notifica expiracion cuando falla la renovacion del token`() {
        val tokenProvider = FakeTokenProvider(
            accessToken = "expired-access-token",
            refreshToken = "expired-refresh-token",
        )
        var refreshCallCount = 0
        val interceptor = AuthInterceptor(
            tokenProvider = tokenProvider,
            tokenRefresher = {
                refreshCallCount++
                null
            },
        )
        val chain = FakeInterceptorChain(
            initialRequest = Request.Builder()
                .url("http://10.0.2.2:5080/api/workorders")
                .build(),
            responses = listOf(401),
        )

        val response = interceptor.intercept(chain)

        assertEquals(401, response.code)
        assertEquals(1, refreshCallCount)
        assertEquals(1, chain.proceededRequests.size)
        assertTrue(tokenProvider.sessionExpiredCalled)
    }

    private class FakeTokenProvider(
        private var accessToken: String?,
        private var refreshToken: String?,
    ) : TokenProvider {

        var sessionExpiredCalled = false
            private set

        override fun getAccessToken(): String? = accessToken

        override fun getRefreshToken(): String? = refreshToken

        override fun updateTokens(accessToken: String, refreshToken: String) {
            this.accessToken = accessToken
            this.refreshToken = refreshToken
        }

        override fun onSessionExpired() {
            sessionExpiredCalled = true
            accessToken = null
            refreshToken = null
        }
    }

    private class FakeInterceptorChain(
        private val initialRequest: Request,
        private val responses: List<Int>,
    ) : Interceptor.Chain {

        val proceededRequests = mutableListOf<Request>()

        override fun request(): Request = initialRequest

        override fun proceed(request: Request): Response {
            val index = proceededRequests.size
            proceededRequests.add(request)
            val statusCode = responses.getOrElse(index) { 500 }
            return Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(statusCode)
                .message(if (statusCode == 200) "OK" else "Unauthorized")
                .body("{}".toResponseBody("application/json".toMediaType()))
                .build()
        }

        override fun call() = throw UnsupportedOperationException()
        override fun connectTimeoutMillis(): Int = 10_000
        override fun connection() = null
        override fun readTimeoutMillis(): Int = 10_000
        override fun withConnectTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this
        override fun withReadTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this
        override fun withWriteTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this
        override fun writeTimeoutMillis(): Int = 10_000
    }
}

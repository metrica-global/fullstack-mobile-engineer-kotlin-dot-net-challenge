package com.fieldops.app.data.remote

import com.fieldops.app.data.local.SessionManager
import com.fieldops.app.data.local.TokenProvider
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response

fun interface TokenRefresher {
    fun refreshTokens(refreshToken: String): Pair<String, String>?
}

class AuthInterceptor(
    private val tokenProvider: TokenProvider = SessionManager,
    private val tokenRefresher: TokenRefresher = TokenRefresher { refreshToken ->
        RetrofitClient.executeTokenRefresh(refreshToken)
    },
) : Interceptor {

    private val refreshLock = Any()

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        if (isPublicAuthEndpoint(originalRequest.url.encodedPath)) {
            return chain.proceed(originalRequest)
        }

        val initialToken = tokenProvider.getAccessToken()
        val authenticatedRequest = attachAuthorizationHeader(originalRequest, initialToken)
        val initialResponse = chain.proceed(authenticatedRequest)

        if (initialResponse.code != HTTP_UNAUTHORIZED) {
            return initialResponse
        }

        val refreshedAccessToken = synchronized(refreshLock) {
            val currentToken = tokenProvider.getAccessToken()
            if (!currentToken.isNullOrBlank() && currentToken != initialToken) {
                currentToken
            } else {
                val currentRefreshToken = tokenProvider.getRefreshToken()
                if (currentRefreshToken.isNullOrBlank()) {
                    null
                } else {
                    val newTokens = tokenRefresher.refreshTokens(currentRefreshToken)
                    if (newTokens != null) {
                        tokenProvider.updateTokens(
                            accessToken = newTokens.first,
                            refreshToken = newTokens.second,
                        )
                        newTokens.first
                    } else {
                        null
                    }
                }
            }
        }

        if (refreshedAccessToken.isNullOrBlank()) {
            tokenProvider.onSessionExpired()
            return initialResponse
        }

        initialResponse.close()
        val retriedRequest = attachAuthorizationHeader(originalRequest, refreshedAccessToken)
        val retriedResponse = chain.proceed(retriedRequest)

        if (retriedResponse.code == HTTP_UNAUTHORIZED) {
            tokenProvider.onSessionExpired()
        }

        return retriedResponse
    }

    private fun attachAuthorizationHeader(request: Request, token: String?): Request {
        if (token.isNullOrBlank()) {
            return request
        }
        return request.newBuilder()
            .header("Authorization", "Bearer $token")
            .build()
    }

    private fun isPublicAuthEndpoint(path: String): Boolean {
        return path.endsWith("/api/auth/login") || path.endsWith("/api/auth/refresh")
    }

    companion object {
        private const val HTTP_UNAUTHORIZED = 401
    }
}

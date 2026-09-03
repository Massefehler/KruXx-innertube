package me.knighthat.internal.util

import io.ktor.util.sha1


/**
 * Cookie names Google accepts as the seed of an authorization hash, in order of preference.
 *
 * A session created by a recent sign-in does not always carry the plain `SAPISID`; the
 * `__Secure-*` variants are then the only usable seed.
 */
private val AUTH_COOKIE_NAMES = listOf( "SAPISID", "__Secure-3PAPISID", "__Secure-1PAPISID" )

internal fun parseCookies( cookies: String ): Map<String, String> =
    cookies.split( ";" )
           .map( String::trim )
           .mapNotNull {
               val parts = it.split( "=", limit = 2 )
               if( parts.size == 2 ) parts[0] to parts[1] else null
           }
           .toMap()

/**
 * Builds Google's `SAPISIDHASH` credential: `<seconds>_<sha1("<seconds> <cookie> <origin>")>`.
 *
 * Returns an empty string when the cookie jar holds none of [AUTH_COOKIE_NAMES], so the caller
 * sends an anonymous request instead of a credential YouTube is bound to reject.
 */
internal fun getSapisidHash( cookies: String, host: String ): String {
    val cookieMap = parseCookies( cookies )
    val seed = AUTH_COOKIE_NAMES.firstNotNullOfOrNull { name ->
        cookieMap[name]?.takeIf( String::isNotBlank )
    } ?: return ""

    val currentTime = System.currentTimeMillis() / 1000
    val hash = sha1( "$currentTime $seed $host".toByteArray() )
                   .joinToString( "" ) { "%02x".format( it ) }

    return "${currentTime}_$hash"
}

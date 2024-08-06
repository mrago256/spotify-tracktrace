package mr.tracktrace.authorization;

import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import mr.tracktrace.adapter.SongTableDynamoAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.enums.AuthorizationScope;
import se.michaelthelin.spotify.model_objects.credentials.AuthorizationCodeCredentials;
import se.michaelthelin.spotify.requests.authorization.authorization_code.AuthorizationCodeRefreshRequest;
import se.michaelthelin.spotify.requests.authorization.authorization_code.AuthorizationCodeRequest;
import se.michaelthelin.spotify.requests.authorization.authorization_code.AuthorizationCodeUriRequest;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.Callable;

@Singleton
public class AuthorizationManager {
    private static final Logger log = LoggerFactory.getLogger(AuthorizationManager.class);
    private static final RetryConfig retryPolicyConfig = RetryConfig.custom()
            .maxAttempts(3)
            .intervalFunction(IntervalFunction.ofExponentialBackoff(5L, 2))
            .build();

    private final SpotifyApi spotifyApi;
    private final SongTableDynamoAdapter songTableDynamoAdapter;
    private final Retry refreshAuthorizationRetryPolicy;
    private final Retry getAuthorizationRetryPolicy;
    private final Retry getAuthorizationURIRetryPolicy;

    @Inject
    public AuthorizationManager(SongTableDynamoAdapter songTableDynamoAdapter, SpotifyApi spotifyApi) {
        this.spotifyApi = spotifyApi;
        this.songTableDynamoAdapter = songTableDynamoAdapter;
        this.refreshAuthorizationRetryPolicy = Retry.of("auth-refresh-retry", retryPolicyConfig);
        this.getAuthorizationRetryPolicy = Retry.of("get-auth-retry", retryPolicyConfig);
        this.getAuthorizationURIRetryPolicy = Retry.of("get-auth-uri-retry", retryPolicyConfig);
    }

    public void initializeAuthorization() {
        String authCode;
        log.info("Auth URL: {}", getAuthorizationURI());
        log.info("Waiting for auth response...");

        try {
            authCode = AuthServer.waitForAndRetrieveAuthCode();
        } catch (IOException | InterruptedException ex) {
            throw new RuntimeException("Web server error: " + ex);
        }

        AuthorizationCodeRequest authorizationCodeRequest = spotifyApi.authorizationCode(authCode)
                .build();

        Callable<AuthorizationCodeCredentials> getAuthCodeCallable = Retry.decorateCallable(
                getAuthorizationRetryPolicy, authorizationCodeRequest::execute);

        AuthorizationCodeCredentials authorizationCodeCredentials;
        try {
            authorizationCodeCredentials = getAuthCodeCallable.call();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

        spotifyApi.setAccessToken(authorizationCodeCredentials.getAccessToken());
        spotifyApi.setRefreshToken(authorizationCodeCredentials.getRefreshToken());
        songTableDynamoAdapter.writeAccessTokenToTable(authorizationCodeCredentials.getAccessToken());
    }

    public void refreshAuthorization() {
        AuthorizationCodeRefreshRequest authorizationCodeRefreshRequest = spotifyApi.authorizationCodeRefresh()
                .build();

        Callable<AuthorizationCodeCredentials> refreshAuthorizationCallable = Retry.decorateCallable(
                refreshAuthorizationRetryPolicy, authorizationCodeRefreshRequest::execute);

        AuthorizationCodeCredentials authorizationCodeCredentials;
        try {
            authorizationCodeCredentials = refreshAuthorizationCallable.call();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

        spotifyApi.setAccessToken(authorizationCodeCredentials.getAccessToken());
        songTableDynamoAdapter.writeAccessTokenToTable(authorizationCodeCredentials.getAccessToken());
    }

    private URI getAuthorizationURI() {
        AuthorizationCodeUriRequest request = spotifyApi.authorizationCodeUri()
                .scope(AuthorizationScope.USER_READ_PLAYBACK_STATE)
                .build();

        Callable<URI> getAuthURICallable = Retry.decorateCallable(
                getAuthorizationURIRetryPolicy, request::execute);

        URI authorizationURI;
        try {
            authorizationURI = getAuthURICallable.call();
        } catch (Exception ex) {
            throw new RuntimeException("Get URI error: " + ex);
        }

        return authorizationURI;
    }
}

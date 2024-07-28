package mr.tracktrace.authorization;

import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import mr.tracktrace.adapter.SongTableDynamoAdapter;
import org.apache.hc.core5.http.ParseException;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.enums.AuthorizationScope;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.model_objects.credentials.AuthorizationCodeCredentials;
import se.michaelthelin.spotify.requests.authorization.authorization_code.AuthorizationCodeRefreshRequest;
import se.michaelthelin.spotify.requests.authorization.authorization_code.AuthorizationCodeRequest;
import se.michaelthelin.spotify.requests.authorization.authorization_code.AuthorizationCodeUriRequest;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.Callable;

@Singleton
public class AuthorizationManager {
    private static final RetryConfig refreshAuthorizationRetryPolicyConfig = RetryConfig.custom()
            .maxAttempts(3)
            .intervalFunction(IntervalFunction.ofExponentialBackoff(5L, 2))
            .build();

    private final SpotifyApi spotifyApi;
    private final SongTableDynamoAdapter songTableDynamoAdapter;
    private final Retry refreshAuthorizationRetryPolicy;

    @Inject
    public AuthorizationManager(SongTableDynamoAdapter songTableDynamoAdapter, SpotifyApi spotifyApi) {
        this.spotifyApi = spotifyApi;
        this.songTableDynamoAdapter = songTableDynamoAdapter;
        this.refreshAuthorizationRetryPolicy = Retry.of("retry-auth-refresh", refreshAuthorizationRetryPolicyConfig);
    }

    public void initializeAuthorization() {
        String authCode;
        System.out.println("Auth URL: " + getAuthorizationURI());
        System.out.println("Waiting for auth response...");

        try {
            authCode = AuthServer.waitForAndRetrieveAuthCode();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Server error: " + e);
        }

        AuthorizationCodeRequest authorizationCodeRequest = spotifyApi.authorizationCode(authCode)
                .build();

        try {
            AuthorizationCodeCredentials authorizationCodeCredentials = authorizationCodeRequest.execute();

            spotifyApi.setAccessToken(authorizationCodeCredentials.getAccessToken());
            spotifyApi.setRefreshToken(authorizationCodeCredentials.getRefreshToken());
            songTableDynamoAdapter.writeAccessTokenToTable(authorizationCodeCredentials.getAccessToken());
        } catch (IOException | SpotifyWebApiException | ParseException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    public void refreshAuthorization() {
        AuthorizationCodeRefreshRequest authorizationCodeRefreshRequest = spotifyApi.authorizationCodeRefresh()
                .build();

        Callable<AuthorizationCodeCredentials> refreshAuthorizationCallable = Retry.decorateCallable(
                refreshAuthorizationRetryPolicy, authorizationCodeRefreshRequest::execute);

        try {
            AuthorizationCodeCredentials authorizationCodeCredentials = refreshAuthorizationCallable.call();

            spotifyApi.setAccessToken(authorizationCodeCredentials.getAccessToken());
            songTableDynamoAdapter.writeAccessTokenToTable(authorizationCodeCredentials.getAccessToken());
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private URI getAuthorizationURI() {
        AuthorizationCodeUriRequest request = spotifyApi.authorizationCodeUri()
                .scope(AuthorizationScope.USER_READ_PLAYBACK_STATE)
                .build();

        return request.execute();
    }
}

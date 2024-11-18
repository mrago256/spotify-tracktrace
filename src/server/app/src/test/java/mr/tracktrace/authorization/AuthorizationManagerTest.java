package mr.tracktrace.authorization;

import mr.tracktrace.adapter.SongTableDynamoAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.enums.AuthorizationScope;
import se.michaelthelin.spotify.model_objects.credentials.AuthorizationCodeCredentials;
import se.michaelthelin.spotify.requests.authorization.authorization_code.AuthorizationCodeRefreshRequest;
import se.michaelthelin.spotify.requests.authorization.authorization_code.AuthorizationCodeRequest;
import se.michaelthelin.spotify.requests.authorization.authorization_code.AuthorizationCodeUriRequest;

import java.io.IOException;
import java.net.URI;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

@ExtendWith(MockitoExtension.class)
public class AuthorizationManagerTest {
    AuthorizationManager subject;

    @Mock
    SongTableDynamoAdapter songTableDynamoAdapter;

    @Mock
    SpotifyApi spotifyApi;

    @BeforeEach
    public void setup() {
        openMocks(this);
        subject = new AuthorizationManager(songTableDynamoAdapter, spotifyApi);
    }

    @Test
    public void initializeAuthorization() {
        mockUriRequest();
        mockInitializeTokens();

        try(MockedStatic<AuthServer> authServer = mockStatic(AuthServer.class)) {
            authServer.when(AuthServer::waitForAndRetrieveAuthCode).thenReturn("code");
            subject.initializeAuthorization();
        }

        verify(spotifyApi).setAccessToken("someToken");
        verify(spotifyApi).setRefreshToken("someRefresh");
        verify(songTableDynamoAdapter).writeAccessTokenToTable("someToken");
    }

    @Test
    public void initializeAuthorization_throwsOnServerException() {
        mockUriRequest();

        try(MockedStatic<AuthServer> authServer = mockStatic(AuthServer.class)) {
            authServer.when(AuthServer::waitForAndRetrieveAuthCode).thenThrow(new IOException("Auth server failed"));

            assertThrows(RuntimeException.class, () -> subject.initializeAuthorization());

            verify(songTableDynamoAdapter).tryGetRefreshToken();
            verifyNoMoreInteractions(spotifyApi, songTableDynamoAdapter);
        }
    }

    @Test
    public void initializeAuthorization_throwsOnAuthApi() throws Exception {
        mockUriRequest();

        AuthorizationCodeRequest requestMock = mock(AuthorizationCodeRequest.class);
        AuthorizationCodeRequest.Builder builderMock = mock(AuthorizationCodeRequest.Builder.class);

        when(builderMock.build()).thenReturn(requestMock);
        when(spotifyApi.authorizationCode("code")).thenReturn(builderMock);
        doThrow(new IOException("Get token call failed")).when(requestMock).execute();

        try(MockedStatic<AuthServer> authServer = mockStatic(AuthServer.class)) {
            authServer.when(AuthServer::waitForAndRetrieveAuthCode).thenReturn("code");

            assertThrows(RuntimeException.class, () -> subject.initializeAuthorization());

            verify(songTableDynamoAdapter).tryGetRefreshToken();
            verifyNoMoreInteractions(spotifyApi, songTableDynamoAdapter);
        }
    }

    @Test
    public void initializeAuthorization_usesSavedToken() {
        mockRefreshRequest();

        String token = "someRefreshToken";
        when(songTableDynamoAdapter.tryGetRefreshToken()).thenReturn(Optional.of(token));

        subject.initializeAuthorization();

        verify(spotifyApi).setRefreshToken(token);
        verify(spotifyApi).setAccessToken(any(String.class));
        verify(songTableDynamoAdapter).writeAccessTokenToTable(any(String.class));

        verifyNoMoreInteractions(spotifyApi, songTableDynamoAdapter);
    }

    @Test
    public void initializeAuthorization_startsServerOnInvalidSavedToken() throws Exception {
        mockUriRequest();
        mockInitializeTokens();

        AuthorizationCodeRefreshRequest requestMock = mock(AuthorizationCodeRefreshRequest.class);
        AuthorizationCodeRefreshRequest.Builder builderMock = mock(AuthorizationCodeRefreshRequest.Builder.class);

        when(builderMock.build()).thenReturn(requestMock);
        when(spotifyApi.authorizationCodeRefresh()).thenReturn(builderMock);
        doThrow(new RuntimeException("Refresh token invalid")).when(requestMock).execute();

        when(songTableDynamoAdapter.tryGetRefreshToken()).thenReturn(Optional.of("invalidCode"));

        try(MockedStatic<AuthServer> authServer = mockStatic(AuthServer.class)) {
            authServer.when(AuthServer::waitForAndRetrieveAuthCode).thenReturn("code");
            subject.initializeAuthorization();
        }
    }

    @Test
    public void refreshAuthorization() {
        mockRefreshRequest();

        subject.refreshAuthorization();

        verify(spotifyApi).setAccessToken("someToken");
        verify(songTableDynamoAdapter).writeAccessTokenToTable("someToken");
    }

    @Test
    public void refreshAuthorizationThrows() throws Exception {
        AuthorizationCodeRefreshRequest requestMock = mock(AuthorizationCodeRefreshRequest.class);
        AuthorizationCodeRefreshRequest.Builder builderMock = mock(AuthorizationCodeRefreshRequest.Builder.class);

        when(builderMock.build()).thenReturn(requestMock);
        when(spotifyApi.authorizationCodeRefresh()).thenReturn(builderMock);
        doThrow(new IOException("Refresh call failed")).when(requestMock).execute();

        assertThrows(RuntimeException.class, () -> subject.refreshAuthorization());

        verifyNoMoreInteractions(spotifyApi, songTableDynamoAdapter);
    }

    @Test
    public void getUriThrows() {
        AuthorizationCodeUriRequest uriRequestMock = mock(AuthorizationCodeUriRequest.class);
        AuthorizationCodeUriRequest.Builder uriBuilderMock = mock(AuthorizationCodeUriRequest.Builder.class);

        when(spotifyApi.authorizationCodeUri()).thenReturn(uriBuilderMock);
        when(uriBuilderMock.scope(any(AuthorizationScope[].class))).thenReturn(uriBuilderMock);
        when(uriBuilderMock.build()).thenReturn(uriRequestMock);
        doThrow(new IOException("Get code call failed")).when(uriRequestMock).execute();

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> subject.initializeAuthorization());

        assertTrue(thrown.getMessage().contains("Get URI error"));
    }

    private void mockUriRequest() {
        AuthorizationCodeUriRequest uriRequestMock = mock(AuthorizationCodeUriRequest.class);
        AuthorizationCodeUriRequest.Builder uriBuilderMock = mock(AuthorizationCodeUriRequest.Builder.class);

        URI uriResponseMock = null;
        try {
            uriResponseMock = new URI("http://example.com");
        } catch (Exception ignored){}

        when(spotifyApi.authorizationCodeUri()).thenReturn(uriBuilderMock);
        when(uriBuilderMock.scope(any(AuthorizationScope[].class))).thenReturn(uriBuilderMock);
        when(uriBuilderMock.build()).thenReturn(uriRequestMock);
        when(uriRequestMock.execute()).thenReturn(uriResponseMock);
    }

    private void mockRefreshRequest() {
        AuthorizationCodeRefreshRequest requestMock = mock(AuthorizationCodeRefreshRequest.class);
        AuthorizationCodeRefreshRequest.Builder builderMock = mock(AuthorizationCodeRefreshRequest.Builder.class);
        AuthorizationCodeCredentials responseMock = mock(AuthorizationCodeCredentials.class);

        when(responseMock.getAccessToken()).thenReturn("someToken");

        when(builderMock.build()).thenReturn(requestMock);
        when(spotifyApi.authorizationCodeRefresh()).thenReturn(builderMock);

        try {
            when(requestMock.execute()).thenReturn(responseMock);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private void mockInitializeTokens() {
        AuthorizationCodeRequest requestMock = mock(AuthorizationCodeRequest.class);
        AuthorizationCodeRequest.Builder builderMock = mock(AuthorizationCodeRequest.Builder.class);
        AuthorizationCodeCredentials responseMock = mock(AuthorizationCodeCredentials.class);

        when(responseMock.getAccessToken()).thenReturn("someToken");
        when(responseMock.getRefreshToken()).thenReturn("someRefresh");

        when(builderMock.build()).thenReturn(requestMock);
        when(spotifyApi.authorizationCode("code")).thenReturn(builderMock);
        try {
            when(requestMock.execute()).thenReturn(responseMock);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}

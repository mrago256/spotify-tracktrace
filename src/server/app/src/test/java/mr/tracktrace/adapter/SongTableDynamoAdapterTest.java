package mr.tracktrace.adapter;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.PaginatedQueryList;
import io.github.resilience4j.retry.RetryConfig;
import mr.tracktrace.adapter.internal.AuthTokenDDBItem;
import mr.tracktrace.adapter.internal.DDBItem;
import mr.tracktrace.adapter.internal.SongItemDDBItem;
import mr.tracktrace.adapter.internal.SongTableReadDDBItem;
import mr.tracktrace.model.SongItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SongTableDynamoAdapterTest {
    private SongTableDynamoAdapter subject;

    private static final SongItem SONG_ITEM = SongItem.builder()
            .trackName("someName")
            .artistName("someArtist")
            .build();

    private static final RetryConfig SHORT_RETRY = RetryConfig.custom()
            .maxAttempts(2)
            .waitDuration(Duration.ofSeconds(0))
            .build();

    @Mock
    DynamoDBMapper mapper;

    @BeforeEach
    public void setup() {
        subject = new SongTableDynamoAdapter(mapper, SHORT_RETRY);
    }

    @Test
    public void writeSongToTable() {
        Instant now = Instant.now();
        SongItemDDBItem expectedSongItemDDBItem = SongItemDDBItem.builder()
                .trackName(SONG_ITEM.getTrackName())
                .artistName(SONG_ITEM.getArtistName())
                .timestamp(now.getEpochSecond())
                .listens(1)
                .build();

        subject.writeSongToTable(SONG_ITEM, now);

        verify(mapper).save(expectedSongItemDDBItem);
    }

    @Test
    public void writeSongToTableThrows() {
        Instant now = Instant.now();
        doThrow(new RuntimeException("Dynamo save failed")).when(mapper).save(any(DDBItem.class));

        Exception exception = assertThrows(RuntimeException.class, () -> subject.writeSongToTable(SONG_ITEM, now));
        assertTrue(exception.getMessage().contains("Dynamo save failed"));
    }

    @Test
    public void writeAccessTokenToTable() {
        Instant now = Instant.now();
        AuthTokenDDBItem expectedAuthTokenDDBItem = AuthTokenDDBItem.builder()
                .tokenName("auth-token")
                .tokenType("auth")
                .token("someToken")
                .timestamp(now.getEpochSecond())
                .build();

        subject.writeAccessTokenToTable("someToken");

        verify(mapper).save(expectedAuthTokenDDBItem);
    }

    @Test
    public void writeAccessTokenToTableThrows() {
        doThrow(new RuntimeException("Dynamo save failed")).when(mapper).save(any(DDBItem.class));

        Exception exception = assertThrows(RuntimeException.class, () -> subject.writeAccessTokenToTable("someToken"));
        assertTrue(exception.getMessage().contains("Dynamo save failed"));
    }

    @Test
    public void writeRefreshTokenToTable() {
        Instant now = Instant.now();
        AuthTokenDDBItem expectedAuthTokenDDBItem = AuthTokenDDBItem.builder()
                .tokenName("refresh-token")
                .tokenType("auth")
                .token("someToken")
                .timestamp(now.getEpochSecond())
                .build();

        subject.writeRefreshTokenToTable("someToken");
        verify(mapper).save(expectedAuthTokenDDBItem);
    }

    @Test
    public void writeRefreshTokenToTableThrows() {
        doThrow(new RuntimeException("Dynamo save failed")).when(mapper).save(any(DDBItem.class));

        Exception exception = assertThrows(RuntimeException.class, () -> subject.writeRefreshTokenToTable("someToken"));
        assertTrue(exception.getMessage().contains("Dynamo save failed"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void tryGetRefreshTokenReturnsEmpty() {
        PaginatedQueryList<AuthTokenDDBItem> mockResponse = (PaginatedQueryList<AuthTokenDDBItem>) mock(PaginatedQueryList.class);

        when(mapper.query(eq(AuthTokenDDBItem.class), any())).thenReturn(mockResponse);
        when(mockResponse.isEmpty()).thenReturn(true);

        assertTrue(subject.tryGetRefreshToken().isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void tryGetRefreshTokenReturnsToken() {
        AuthTokenDDBItem authTokenDDBItem = AuthTokenDDBItem.builder()
                .tokenName("refresh-token")
                .tokenType("auth")
                .token("someToken")
                .timestamp(Instant.now().getEpochSecond())
                .build();

        PaginatedQueryList<AuthTokenDDBItem> mockResponse = (PaginatedQueryList<AuthTokenDDBItem>) mock(PaginatedQueryList.class);

        when(mapper.query(eq(AuthTokenDDBItem.class), any())).thenReturn(mockResponse);
        when(mockResponse.isEmpty()).thenReturn(false);
        when(mockResponse.getFirst()).thenReturn(authTokenDDBItem);

        assertEquals(subject.tryGetRefreshToken().get(), "someToken");
    }

    @Test
    public void tryGetRefreshTokenThrows() {
        doThrow(new RuntimeException("Dynamo read failed")).when(mapper).query(any(), any());

        Exception exception = assertThrows(RuntimeException.class, () -> subject.tryGetRefreshToken());
        assertTrue(exception.getMessage().contains("Dynamo read failed"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void songInTable() {
        PaginatedQueryList<SongTableReadDDBItem> mockResponse = (PaginatedQueryList<SongTableReadDDBItem>) mock(PaginatedQueryList.class);
        SongTableReadDDBItem songTableReadDDBItem = SongTableReadDDBItem.builder()
                .trackName("someName")
                .artistName("someArtist")
                .timestamp(0L)
                .listens(0)
                .build();

        when(mapper.query(eq(SongTableReadDDBItem.class), any())).thenReturn(mockResponse);
        when(mockResponse.getFirst()).thenReturn(songTableReadDDBItem);

        assertTrue(subject.songInTable(SONG_ITEM));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void songNotInTable() {
        PaginatedQueryList<SongTableReadDDBItem> mockResponse = (PaginatedQueryList<SongTableReadDDBItem>) mock(PaginatedQueryList.class);

        when(mapper.query(eq(SongTableReadDDBItem.class), any())).thenReturn(mockResponse);
        when(mockResponse.getFirst()).thenThrow(new NoSuchElementException());

        assertFalse(subject.songInTable(SONG_ITEM));
    }

    @Test
    public void songInTableThrows() {
        when(mapper.query(any(), any())).thenThrow(new RuntimeException("Dynamo query failed"));

        Exception exception = assertThrows(RuntimeException.class, () -> subject.songInTable(SONG_ITEM));
        assertTrue(exception.getMessage().contains("Dynamo query failed"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void incrementSongListenCount() {
        SongItemDDBItem expectedUpdateItem = SongItemDDBItem.builder()
                .trackName("someName")
                .artistName("someArtist")
                .timestamp(99999999L)
                .listens(2)
                .build();

        PaginatedQueryList<SongTableReadDDBItem> mockResponse = (PaginatedQueryList<SongTableReadDDBItem>) mock(PaginatedQueryList.class);
        when(mapper.query(eq(SongTableReadDDBItem.class), any())).thenReturn(mockResponse);
        when(mockResponse.getFirst()).thenReturn(SongTableReadDDBItem.builder()
                .trackName("someName")
                .artistName("someArtist")
                .timestamp(99999999L)
                .listens(1)
                .build());

        subject.incrementSongListenCount(SONG_ITEM);
        verify(mapper).save(expectedUpdateItem);
    }
}

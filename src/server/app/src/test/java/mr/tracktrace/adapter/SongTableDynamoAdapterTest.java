package mr.tracktrace.adapter;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.PaginatedQueryList;
import mr.tracktrace.adapter.internal.AuthTokenDDBItem;
import mr.tracktrace.adapter.internal.DDBItem;
import mr.tracktrace.adapter.internal.SongItemDDBItem;
import mr.tracktrace.model.SongItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

@ExtendWith(MockitoExtension.class)
public class SongTableDynamoAdapterTest {
    private SongTableDynamoAdapter subject;

    private static final SongItem songItem = SongItem.builder()
            .trackName("someName")
            .artistName("someArtist")
            .build();

    @Mock
    DynamoDBMapper mapper;

    @BeforeEach
    public void setup() {
        openMocks(this);
        subject = new SongTableDynamoAdapter(mapper);
    }

    @Test
    public void writeSongToTable() {
        Instant now = Instant.now();
        SongItemDDBItem expectedSongItemDDBItem = SongItemDDBItem.builder()
                .trackName(songItem.getTrackName())
                .artistName(songItem.getArtistName())
                .timestamp(now.getEpochSecond())
                .listens(1)
                .build();

        subject.writeSongToTable(songItem, now);

        verify(mapper).save(expectedSongItemDDBItem);
    }

    @Test
    public void writeSongToTableThrows() {
        Instant now = Instant.now();
        doThrow(new RuntimeException("Dynamo save failed")).when(mapper).save(any(DDBItem.class));

        Exception exception = assertThrows(RuntimeException.class, () -> subject.writeSongToTable(songItem, now));
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
    @SuppressWarnings("unchecked")
    public void songInTable() {
        PaginatedQueryList<SongItemDDBItem> mockResponse = (PaginatedQueryList<SongItemDDBItem>) mock(PaginatedQueryList.class);

        when(mapper.query(eq(SongItemDDBItem.class), any())).thenReturn(mockResponse);

        assertTrue(subject.songInTable(songItem));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void songNotInTable() {
        PaginatedQueryList<SongItemDDBItem> mockResponse = (PaginatedQueryList<SongItemDDBItem>) mock(PaginatedQueryList.class);

        when(mapper.query(eq(SongItemDDBItem.class), any())).thenReturn(mockResponse);
        when(mockResponse.getFirst()).thenThrow(new NoSuchElementException());

        assertFalse(subject.songInTable(songItem));
    }

    @Test
    public void songInTableThrows() {
        when(mapper.query(any(), any())).thenThrow(new RuntimeException("Dynamo query failed"));

        Exception exception = assertThrows(RuntimeException.class, () -> subject.songInTable(songItem));
        assertTrue(exception.getMessage().contains("Dynamo query failed"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void incrementSongListenCount() {
        SongItemDDBItem expectedUpdateItem = SongItemDDBItem.builder()
                .trackName("someName")
                .artistName("someArtist")
                .listens(2)
                .build();

        PaginatedQueryList<SongItemDDBItem> mockResponse = (PaginatedQueryList<SongItemDDBItem>) mock(PaginatedQueryList.class);
        when(mapper.query(eq(SongItemDDBItem.class), any())).thenReturn(mockResponse);
        when(mockResponse.getFirst()).thenReturn(SongItemDDBItem.builder().trackName("someName").artistName("someArtist").timestamp(0L).listens(1).build());

        subject.incrementSongListenCount(songItem);
        verify(mapper).save(expectedUpdateItem);
    }
}

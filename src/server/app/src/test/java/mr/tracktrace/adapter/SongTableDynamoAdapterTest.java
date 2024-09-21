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
import java.util.Optional;

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
import static org.mockito.MockitoAnnotations.openMocks;

@ExtendWith(MockitoExtension.class)
public class SongTableDynamoAdapterTest {
    private SongTableDynamoAdapter subject;

    private static final SongItem songItem = SongItem.builder()
            .trackURI("someURI")
            .trackName("songName")
            .artistName("someArtist")
            .build();
    private static final SongItemDDBItem songItemDDBItem = SongItemDDBItem.builder()
            .trackURI("someURI")
            .trackName("songName")
            .artistName("someArtist")
            .timestamp(Instant.now().getEpochSecond())
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
                .trackURI(songItem.getTrackURI())
                .trackName(songItem.getTrackName())
                .artistName(songItem.getArtistName())
                .timestamp(now.getEpochSecond())
                .build();

        subject.writeSongToTable(songItem, now);

        verify(mapper).save(expectedSongItemDDBItem);
    }

    @Test
    public void writeSongToTableThrows() {
        Instant now = Instant.now();
        doThrow(new RuntimeException("Dynamo save failed")).when(mapper).save(any(DDBItem.class));

        assertThrows(RuntimeException.class, () -> subject.writeSongToTable(songItem, now));
    }

    @Test
    public void writeAccessTokenToTable() {
        Instant now = Instant.now();
        AuthTokenDDBItem expectedAuthTokenDDBItem = AuthTokenDDBItem.builder()
                .tokenName("auth-token")
                .authToken("someToken")
                .timestamp(now.getEpochSecond())
                .build();

        subject.writeAccessTokenToTable("someToken");

        verify(mapper).save(expectedAuthTokenDDBItem);
    }

    @Test
    public void writeAccessTokenToTableThrows() {
        doThrow(new RuntimeException("Dynamo save failed")).when(mapper).save(any(DDBItem.class));

        assertThrows(RuntimeException.class, () -> subject.writeAccessTokenToTable("someToken"));
    }

    @Test
    public void songInTable() {
        SongItem songItem = SongItem.fromSongItemDDBItem(songItemDDBItem);

        when(mapper.load(SongItemDDBItem.class, "someURI"))
                .thenReturn(songItemDDBItem);

        assertTrue(subject.songInTable(songItem));
    }

    @Test
    public void songNotInTable() {
        when(mapper.load(SongItemDDBItem.class, "someURI")).thenReturn(null);

        assertFalse(subject.songInTable(songItem));
    }

    @Test
    public void songInTableThrows() {
        when(mapper.load(SongItemDDBItem.class, "songURI")).thenThrow(new RuntimeException("Dynamo read failed"));

        assertThrows(RuntimeException.class, () -> subject.songInTable(songItem));
    }

    @Test
    public void tryGetExistingTimestamp() {
        PaginatedQueryList<SongItemDDBItem> mockResponse = (PaginatedQueryList<SongItemDDBItem>) mock(PaginatedQueryList.class);

        when(mapper.query(eq(SongItemDDBItem.class), any())).thenReturn(mockResponse);
        when(mockResponse.getFirst()).thenReturn(songItemDDBItem);

        subject.tryGetExistingTimestamp(songItem);
    }

    @Test
    public void tryGetExistingTimestampReturnsEmpty() {
        PaginatedQueryList<SongItemDDBItem> mockResponse = (PaginatedQueryList<SongItemDDBItem>) mock(PaginatedQueryList.class);

        when(mapper.query(eq(SongItemDDBItem.class), any())).thenReturn(mockResponse);
        when(mockResponse.getFirst()).thenThrow(new NoSuchElementException());

        assertEquals(subject.tryGetExistingTimestamp(songItem), Optional.empty());
    }

    @Test
    public void tryGetExistingThrows() {
        when(mapper.query(any(), any())).thenThrow(new RuntimeException("Dynamo query failed"));

        assertThrows(RuntimeException.class, () -> subject.tryGetExistingTimestamp(songItem));
    }
}

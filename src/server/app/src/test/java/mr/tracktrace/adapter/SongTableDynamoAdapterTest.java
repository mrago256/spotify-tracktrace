package mr.tracktrace.adapter;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

@ExtendWith(MockitoExtension.class)
public class SongTableDynamoAdapterTest {
    private SongTableDynamoAdapter subject;

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
        SongItem songItem = SongItem.builder()
                .trackId("someId")
                .trackName("songName")
                .build();

        SongItemDDBItem expectedSongItemDDBItem = SongItemDDBItem.builder()
                .trackId("someId")
                .trackName("songName")
                .timestamp(now.getEpochSecond())
                .build();

        subject.writeSongToTable(songItem, now);

        verify(mapper).save(expectedSongItemDDBItem);
    }

    @Test
    public void writeSongToTableThrows() {
        Instant now = Instant.now();
        SongItem songItem = SongItem.builder()
                .trackId("someId")
                .trackName("songName")
                .build();

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
        SongItemDDBItem songItemDDBItem = SongItemDDBItem.builder()
                .trackId("songId")
                .trackName("the best song")
                .timestamp(0L)
                .build();

        SongItem songItem = songItemDDBItem.toSongItem();

        when(mapper.load(SongItemDDBItem.class, "songId")).thenReturn(songItemDDBItem);

        assertTrue(subject.songInTable(songItem));
    }

    @Test
    public void songNotInTable() {
        SongItem songItem = SongItem.builder()
                .trackId("songId")
                .trackName("the best song")
                .build();

        when(mapper.load(SongItemDDBItem.class, "songId")).thenReturn(null);

        assertFalse(subject.songInTable(songItem));
    }

    @Test
    public void songInTableThrows() {
        SongItem songItem = SongItem.builder()
                .trackId("songId")
                .trackName("the best song")
                .build();

        when(mapper.load(SongItemDDBItem.class, "songId")).thenThrow(new RuntimeException("Dynamo read failed"));

        assertThrows(RuntimeException.class, () -> subject.songInTable(songItem));
    }
}

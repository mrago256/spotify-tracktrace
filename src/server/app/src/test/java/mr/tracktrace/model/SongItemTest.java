package mr.tracktrace.model;

import mr.tracktrace.adapter.internal.SongItemDDBItem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import se.michaelthelin.spotify.model_objects.IPlaylistItem;
import se.michaelthelin.spotify.model_objects.miscellaneous.CurrentlyPlaying;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SongItemTest {
    @Test
    public void songItemFromDDBItem() {
        SongItemDDBItem songItemDDBItem = SongItemDDBItem.builder()
                .trackName("someName")
                .artistName("someArtist")
                .timestamp(0L)
                .listens(0)
                .build();

        SongItem expectedSongItem = SongItem.builder()
                .trackName("someName")
                .artistName("someArtist")
                .build();

        assertEquals(expectedSongItem, SongItem.fromSongItemDDBItem(songItemDDBItem));
    }

    @Test
    public void songItemFromCurrentlyPlaying() {
        CurrentlyPlaying currentlyPlayingMock = mock(CurrentlyPlaying.class);
        IPlaylistItem itemMock = mock(IPlaylistItem.class);

        when(currentlyPlayingMock.getItem()).thenReturn(itemMock);
        when(itemMock.getName()).thenReturn("someName");

        SongItem expectedSongItem = SongItem.builder()
                .trackName("someName")
                .build();

        assertEquals(expectedSongItem, SongItem.fromCurrentlyPlaying(currentlyPlayingMock));
    }
}

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
                .trackURI("someURI")
                .trackName("someName")
                .artistName("someArtist")
                .timestamp(0L)
                .build();

        SongItem expectedSongItem = SongItem.builder()
                .trackURI("someURI")
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
        when(itemMock.getUri()).thenReturn("someURI");
        when(itemMock.getName()).thenReturn("someName");

        SongItem expectedSongItem = SongItem.builder()
                .trackURI("someURI")
                .trackName("someName")
                .build();

        assertEquals(expectedSongItem, SongItem.fromCurrentlyPlaying(currentlyPlayingMock));
    }

    @Test
    public void getTrackId() {
        SongItem songItem = SongItem.builder()
                .trackURI("spotify:track:someId")
                .build();
        String expectedId = "someId";

        assertEquals(expectedId, SongItem.getTrackId(songItem));
    }
}
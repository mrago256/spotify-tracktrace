package mr.tracktrace.adapter;

import mr.tracktrace.model.SongItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.model_objects.IPlaylistItem;
import se.michaelthelin.spotify.model_objects.miscellaneous.CurrentlyPlaying;
import se.michaelthelin.spotify.requests.data.player.GetUsersCurrentlyPlayingTrackRequest;

import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

@ExtendWith(MockitoExtension.class)
public class SpotifyAdapterTest {
    private SpotifyAdapter subject;

    @Mock
    SpotifyApi spotifyApi;

    @BeforeEach
    public void setup() {
        openMocks(this);
        subject = new SpotifyAdapter(spotifyApi);
    }

    @Test
    public void getCurrentlyPlaying() throws Exception {
        SongItem expectedCurrentlyPlaying = SongItem.builder()
                .trackId("someId")
                .trackName("someName")
                .build();

        GetUsersCurrentlyPlayingTrackRequest requestMock = mock(GetUsersCurrentlyPlayingTrackRequest.class);
        GetUsersCurrentlyPlayingTrackRequest.Builder builderMock = mock(GetUsersCurrentlyPlayingTrackRequest.Builder.class);
        CurrentlyPlaying currentlyPlayingMock = mock(CurrentlyPlaying.class);
        IPlaylistItem itemMock = mock(IPlaylistItem.class);

        when(currentlyPlayingMock.getItem()).thenReturn(itemMock);
        when(itemMock.getUri()).thenReturn("someId");
        when(itemMock.getName()).thenReturn("someName");

        when(builderMock.build()).thenReturn(requestMock);
        when(spotifyApi.getUsersCurrentlyPlayingTrack()).thenReturn(builderMock);
        when(requestMock.execute()).thenReturn(currentlyPlayingMock);

        Optional<SongItem> songItemResponse = subject.getCurrentlyPlaying();

        assertTrue(songItemResponse.isPresent());
        assertEquals(expectedCurrentlyPlaying, songItemResponse.get());
    }

    @Test
    public void getCurrentlyPlaying_noCurrentSong() throws Exception {
        GetUsersCurrentlyPlayingTrackRequest requestMock = mock(GetUsersCurrentlyPlayingTrackRequest.class);
        GetUsersCurrentlyPlayingTrackRequest.Builder builderMock = mock(GetUsersCurrentlyPlayingTrackRequest.Builder.class);

        when(builderMock.build()).thenReturn(requestMock);
        when(spotifyApi.getUsersCurrentlyPlayingTrack()).thenReturn(builderMock);
        when(requestMock.execute()).thenReturn(null);

        Optional<SongItem> songItemResponse = subject.getCurrentlyPlaying();

        assertTrue(songItemResponse.isEmpty());
    }

    @Test
    public void getCurrentlyPlayingThrows() throws Exception {
        GetUsersCurrentlyPlayingTrackRequest requestMock = mock(GetUsersCurrentlyPlayingTrackRequest.class);
        GetUsersCurrentlyPlayingTrackRequest.Builder builderMock = mock(GetUsersCurrentlyPlayingTrackRequest.Builder.class);

        when(builderMock.build()).thenReturn(requestMock);
        when(spotifyApi.getUsersCurrentlyPlayingTrack()).thenReturn(builderMock);
        doThrow(new IOException("Spotify call failed")).when(requestMock).execute();

        assertThrows(RuntimeException.class, () -> subject.getCurrentlyPlaying());
    }
}

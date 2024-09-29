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
import se.michaelthelin.spotify.model_objects.specification.ArtistSimplified;
import se.michaelthelin.spotify.model_objects.specification.Track;
import se.michaelthelin.spotify.requests.data.player.GetUsersCurrentlyPlayingTrackRequest;
import se.michaelthelin.spotify.requests.data.tracks.GetTrackRequest;

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
                .trackName("someName")
                .artistName("someArtist")
                .build();

        ArtistSimplified artistMock = mock(ArtistSimplified.class);
        when(artistMock.getName()).thenReturn("someArtist");

        Track trackMock = mock(Track.class);
        when(trackMock.getArtists()).thenReturn(new ArtistSimplified[] {artistMock});

        GetTrackRequest.Builder trackBuilderMock = mock(GetTrackRequest.Builder.class);
        when(trackBuilderMock.build()).thenReturn(mock(GetTrackRequest.class));
        when(spotifyApi.getTrack("someId")).thenReturn(trackBuilderMock);
        when(trackBuilderMock.build().execute()).thenReturn(trackMock);

        IPlaylistItem itemMock = mock(IPlaylistItem.class);
        when(itemMock.getId()).thenReturn("someId");
        when(itemMock.getName()).thenReturn("someName");

        CurrentlyPlaying currentlyPlayingMock = mock(CurrentlyPlaying.class);
        when(currentlyPlayingMock.getItem()).thenReturn(itemMock);

        GetUsersCurrentlyPlayingTrackRequest.Builder requestBuilderMock = mock(GetUsersCurrentlyPlayingTrackRequest.Builder.class);
        when(requestBuilderMock.build()).thenReturn(mock(GetUsersCurrentlyPlayingTrackRequest.class));
        when(spotifyApi.getUsersCurrentlyPlayingTrack()).thenReturn(requestBuilderMock);
        when(requestBuilderMock.build().execute()).thenReturn(currentlyPlayingMock);

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

        Exception exception = assertThrows(RuntimeException.class, () -> subject.getCurrentlyPlaying());
        assertTrue(exception.getMessage().contains("Spotify call failed"));
    }

    @Test
    public void getTrackThrows() throws Exception {
        GetUsersCurrentlyPlayingTrackRequest requestMock = mock(GetUsersCurrentlyPlayingTrackRequest.class);
        GetUsersCurrentlyPlayingTrackRequest.Builder builderMock = mock(GetUsersCurrentlyPlayingTrackRequest.Builder.class);
        CurrentlyPlaying currentlyPlayingMock = mock(CurrentlyPlaying.class);
        IPlaylistItem itemMock = mock(IPlaylistItem.class);

        when(currentlyPlayingMock.getItem()).thenReturn(itemMock);
        when(itemMock.getId()).thenReturn("someId");
        when(itemMock.getName()).thenReturn("someName");

        when(builderMock.build()).thenReturn(requestMock);
        when(spotifyApi.getUsersCurrentlyPlayingTrack()).thenReturn(builderMock);
        when(requestMock.execute()).thenReturn(currentlyPlayingMock);

        GetTrackRequest trackRequestMock = mock(GetTrackRequest.class);
        GetTrackRequest.Builder trackBuilderMock = mock(GetTrackRequest.Builder.class);

        when(trackBuilderMock.build()).thenReturn(trackRequestMock);
        when(spotifyApi.getTrack("someId")).thenReturn(trackBuilderMock);
        doThrow(new RuntimeException("Get track failed")).when(trackRequestMock).execute();

        Exception exception = assertThrows(RuntimeException.class, () -> subject.getCurrentlyPlaying());
        assertTrue(exception.getMessage().contains("Get track failed"));
    }
}

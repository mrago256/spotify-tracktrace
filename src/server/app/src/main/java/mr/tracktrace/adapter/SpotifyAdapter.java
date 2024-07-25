package mr.tracktrace.adapter;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import mr.tracktrace.model.SongItem;
import org.apache.hc.core5.http.ParseException;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.model_objects.miscellaneous.CurrentlyPlaying;
import se.michaelthelin.spotify.requests.data.player.GetUsersCurrentlyPlayingTrackRequest;

import java.io.IOException;

@Singleton
public class SpotifyAdapter {

    private final SpotifyApi spotifyApi;

    @Inject
    public SpotifyAdapter(SpotifyApi spotifyApi) {
        this.spotifyApi = spotifyApi;
    }

    public SongItem getCurrentlyPlaying() {
        GetUsersCurrentlyPlayingTrackRequest getUsersCurrentlyPlayingTrackRequest = spotifyApi
                .getUsersCurrentlyPlayingTrack()
                .build();

        CurrentlyPlaying currentlyPlaying = null;
        try {
            currentlyPlaying = getUsersCurrentlyPlayingTrackRequest.execute();
        } catch (IOException | SpotifyWebApiException | ParseException e) {
            System.out.println("Error: " + e);
        }

        if (currentlyPlaying == null) {
            return null;
        }

        return SongItem.builder()
                .trackID(currentlyPlaying.getItem().getUri())
                .trackName(currentlyPlaying.getItem().getName())
                .build();
    }
}

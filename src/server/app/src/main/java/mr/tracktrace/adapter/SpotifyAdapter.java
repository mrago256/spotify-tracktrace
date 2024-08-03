package mr.tracktrace.adapter;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import mr.tracktrace.model.SongItem;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.model_objects.miscellaneous.CurrentlyPlaying;
import se.michaelthelin.spotify.requests.data.player.GetUsersCurrentlyPlayingTrackRequest;

import java.util.Optional;
import java.util.concurrent.Callable;

@Singleton
public class SpotifyAdapter {
    private static final RetryConfig retryPolicyConfig = RetryConfig.custom()
            .maxAttempts(3)
            .intervalFunction(IntervalFunction.ofExponentialBackoff(5L, 2))
            .build();

    private final SpotifyApi spotifyApi;
    private final Retry getCurrentSongRetryPolicy;

    @Inject
    public SpotifyAdapter(SpotifyApi spotifyApi) {
        this.spotifyApi = spotifyApi;
        this.getCurrentSongRetryPolicy = Retry.of("get-current-song-retry", retryPolicyConfig);
    }

    public Optional<SongItem> getCurrentlyPlaying() {
        GetUsersCurrentlyPlayingTrackRequest getUsersCurrentlyPlayingTrackRequest = spotifyApi
                .getUsersCurrentlyPlayingTrack()
                .build();

        Callable<CurrentlyPlaying> getCurrentlyPlayingCallable = Retry.decorateCallable(
                getCurrentSongRetryPolicy, getUsersCurrentlyPlayingTrackRequest::execute);

        CurrentlyPlaying currentlyPlaying;
        try {
            currentlyPlaying = getCurrentlyPlayingCallable.call();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

        if (currentlyPlaying == null) {
            return Optional.empty();
        }

        return Optional.of(SongItem.fromCurrentlyPlaying(currentlyPlaying));
    }
}

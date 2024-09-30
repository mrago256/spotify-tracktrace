package mr.tracktrace;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import mr.tracktrace.adapter.SongTableDynamoAdapter;
import mr.tracktrace.adapter.SpotifyAdapter;
import mr.tracktrace.authorization.AuthorizationManager;
import mr.tracktrace.model.SongItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.michaelthelin.spotify.exceptions.detailed.UnauthorizedException;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Singleton
public class Service {
    private static final Logger log = LoggerFactory.getLogger(Service.class);
    private static final int SONG_CYCLE_DELAY_IN_SECONDS = 10;
    private static final int AUTH_REFRESH_DELAY_IN_MINUTES = 25;
    private static final int FULL_LISTEN_CYCLES = 5;

    private static SongItem lastKnownSong = SongItem.builder().build();
    private static Instant firstListened = Instant.now();
    private static int songCycles = 0;
    private static boolean itemUpdated = false;

    private final AuthorizationManager authorizationManager;
    private final ScheduledExecutorService scheduledExecutorService;
    private final SongTableDynamoAdapter songTableDynamoAdapter;
    private final SpotifyAdapter spotifyAdapter;

    @Inject
    public Service(
            AuthorizationManager authorizationManager,
            ScheduledExecutorService scheduledExecutorService,
            SongTableDynamoAdapter songTableDynamoAdapter,
            SpotifyAdapter spotifyAdapter) {

        this.authorizationManager = authorizationManager;
        this.scheduledExecutorService = scheduledExecutorService;
        this.songTableDynamoAdapter = songTableDynamoAdapter;
        this.spotifyAdapter = spotifyAdapter;
    }

    public void start() {
        log.info("Starting...");

        authorizationManager.initializeAuthorization();

        scheduledExecutorService.scheduleWithFixedDelay(mainTask(), 0, SONG_CYCLE_DELAY_IN_SECONDS, TimeUnit.SECONDS);
        scheduledExecutorService.scheduleWithFixedDelay(refreshToken(), AUTH_REFRESH_DELAY_IN_MINUTES, AUTH_REFRESH_DELAY_IN_MINUTES, TimeUnit.MINUTES);
    }

    Runnable mainTask() {
        return () -> {
            try {
                Optional<SongItem> currentSong = spotifyAdapter.getCurrentlyPlaying();
                if (currentSong.isEmpty()) {
                    log.info("No song playing");
                    return;
                }

                if (!lastKnownSong.equals(currentSong.get())) {
                    log.info("Current song different than last song");
                    resetState(currentSong.get());
                    return;
                }

                if (songTableDynamoAdapter.songInTable(currentSong.get())) {
                    log.info("Song in table");
                    songCycles++;

                    if (songCycles >= FULL_LISTEN_CYCLES && !itemUpdated) {
                        log.info("Incrementing listen count");
                        songTableDynamoAdapter.incrementSongListenCount(currentSong.get());
                        itemUpdated = true;
                    }
                    return;
                }

                log.info("Increment");
                songCycles++;

                if (songCycles >= FULL_LISTEN_CYCLES) {
                    log.info("Adding song: {}", currentSong.get().getTrackName());
                    songTableDynamoAdapter.writeSongToTable(currentSong.get(), firstListened);
                    itemUpdated = true;
                }
            } catch (Exception ex) {
                log.warn("Main task error", ex);

                if (ex.getCause() instanceof UnauthorizedException) {
                    log.info("Attempting authorization refresh");
                    refreshToken().run();
                }
            }
        };
    }

    Runnable refreshToken() {
        return () -> {
            try {
                log.info("Refreshing auth...");
                authorizationManager.refreshAuthorization();
            } catch (Exception ex) {
                log.error("Refresh token error", ex);
            }
        };
    }

    private void resetState(SongItem songItem) {
        songCycles = 0;
        itemUpdated = false;
        lastKnownSong = songItem;
        firstListened = Instant.now();
    }
}

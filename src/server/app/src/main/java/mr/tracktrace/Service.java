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
    private static final int CYCLES_TO_SAVE_SONG = 5;

    private static SongItem lastKnownSong = SongItem.builder().build();
    private static Instant firstListened;
    private static int songCycles;


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
                    return; // for now return early if no song playing
                }

                if (songTableDynamoAdapter.songInTable(currentSong.get())) {
                    log.info("Song in table");
                    return; // song has already been listened to before
                }

                if (!lastKnownSong.equals(currentSong.get())) {
                    log.info("Current song different than last song");
                    songCycles = 0;
                    lastKnownSong = currentSong.get();
                    firstListened = Instant.now();
                    return;
                }

                log.info("Increment");
                songCycles++;

                if (songCycles >= CYCLES_TO_SAVE_SONG) {
                    log.info("Adding song: {}", currentSong.get().getTrackName());
                    Optional<Long> existingTimestamp = songTableDynamoAdapter.tryGetExistingTimestamp(currentSong.get());
                    Instant timestampToWrite = existingTimestamp.isPresent() ? Instant.ofEpochSecond(existingTimestamp.get()) : firstListened;

                    songTableDynamoAdapter.writeSongToTable(currentSong.get(), timestampToWrite);
                }
            } catch (Exception ex) {
                log.warn("Main task error", ex);

                if (ex.getCause() instanceof UnauthorizedException) {
                    log.info("Attempting authorization refresh");
                    try {
                        authorizationManager.refreshAuthorization();
                    } catch (Exception exception) {
                        log.warn("Authorization refresh attempt failed", exception);
                    }
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
}

package mr.tracktrace;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import mr.tracktrace.adapter.SongTableDynamoAdapter;
import mr.tracktrace.adapter.SpotifyAdapter;
import mr.tracktrace.authorization.AuthorizationManager;
import mr.tracktrace.model.SongItem;

import java.time.Instant;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Singleton
public class Service {
    private static final int SONG_CYCLE_DELAY_IN_SECONDS = 10;
    private static final int AUTH_REFRESH_DELAY_IN_MINUTES = 50;
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
        System.out.println("Starting...");

        authorizationManager.initializeAuthorization();

        scheduledExecutorService.scheduleWithFixedDelay(mainTask(), 0, SONG_CYCLE_DELAY_IN_SECONDS, TimeUnit.SECONDS);
        scheduledExecutorService.scheduleWithFixedDelay(refreshToken(), AUTH_REFRESH_DELAY_IN_MINUTES, AUTH_REFRESH_DELAY_IN_MINUTES, TimeUnit.MINUTES);
    }

    private Runnable mainTask() {
        return () -> {
            try {
                SongItem currentSong = spotifyAdapter.getCurrentlyPlaying();
                if (currentSong == null) {
                    System.out.println(Instant.now().toString() + ": No song playing");
                    return; // for now return early if no song playing
                }

                if (songTableDynamoAdapter.songInTable(currentSong)) {
                    System.out.println(Instant.now().toString() + ": Song in table");
                    return; // song has already been listened to before
                }

                if (!lastKnownSong.equals(currentSong)) {
                    System.out.println(Instant.now().toString() + ": Different than last song");
                    songCycles = 0;
                    lastKnownSong = currentSong;
                    firstListened = Instant.now();
                    return;
                }

                System.out.println(Instant.now().toString() + ": Increment");
                songCycles++;

                if (songCycles >= CYCLES_TO_SAVE_SONG) {
                    System.out.println(Instant.now().toString() + ": Adding song: " + currentSong.getTrackName());
                    songTableDynamoAdapter.writeSongToTable(currentSong, firstListened);
                }
            } catch (Exception ex) {
                System.out.println(Instant.now().toString() + ": Main task error: " + ex);
            }
        };
    }

    private Runnable refreshToken() {
        return () -> {
            try {
                System.out.println(Instant.now().toString() + ": Refreshing auth...");
                authorizationManager.refreshAuthorization();
            } catch (Exception ex) {
                System.out.println(Instant.now().toString() + ": Refresh token error: " + ex);
                songTableDynamoAdapter.writeErrorToTable("Failed to refresh token " + ex.getMessage());
                System.exit(1);
            }
        };
    }
}

package mr.tracktrace;

import mr.tracktrace.adapter.SongTableDynamoAdapter;
import mr.tracktrace.adapter.SpotifyAdapter;
import mr.tracktrace.authorization.AuthorizationManager;
import mr.tracktrace.model.SongItem;
import mr.tracktrace.util.ExitHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

@ExtendWith(MockitoExtension.class)
public class ServiceTest {
    private static final SongItem songItem = SongItem.builder()
            .trackId("someId")
            .trackName("someName")
            .build();
    private static final SongItem otherSongItem = SongItem.builder()
            .trackId("anotherId")
            .trackName("anotherName")
            .build();

    private Service subject;

    @Mock
    AuthorizationManager authorizationManager;

    @Mock
    ScheduledExecutorService scheduledExecutorService;

    @Mock
    SongTableDynamoAdapter songTableDynamoAdapter;

    @Mock
    SpotifyAdapter spotifyAdapter;

    @BeforeEach
    public void setup() {
        openMocks(this);
        subject = new Service(authorizationManager, scheduledExecutorService, songTableDynamoAdapter, spotifyAdapter);
    }

    @Test
    public void start() {
        subject.start();

        verify(authorizationManager).initializeAuthorization();
        verify(scheduledExecutorService).scheduleWithFixedDelay(any(Runnable.class), eq(0L), eq(10L), eq(TimeUnit.SECONDS));
        verify(scheduledExecutorService).scheduleWithFixedDelay(any(Runnable.class), eq(25L), eq(25L), eq(TimeUnit.MINUTES));
    }

    @Test
    public void mainTaskDoesNotThrow() {
        doThrow(new RuntimeException("Spotify api fail")).when(spotifyAdapter).getCurrentlyPlaying();

        assertDoesNotThrow(() -> subject.mainTask().run());
    }

    @Test
    public void mainTask_noSongPlaying() {
        when(spotifyAdapter.getCurrentlyPlaying()).thenReturn(Optional.empty());

        subject.mainTask().run();

        verify(spotifyAdapter).getCurrentlyPlaying();
        verifyNoMoreInteractions(songTableDynamoAdapter, spotifyAdapter);
    }

    @Test
    public void mainTask_songInTable() {
        when(spotifyAdapter.getCurrentlyPlaying()).thenReturn(Optional.of(songItem));
        when(songTableDynamoAdapter.songInTable(songItem)).thenReturn(true);

        subject.mainTask().run();

        verify(songTableDynamoAdapter).songInTable(songItem);
        verifyNoMoreInteractions(songTableDynamoAdapter, spotifyAdapter);
    }

    @Test
    public void mainTask_incrementSong() {
        when(spotifyAdapter.getCurrentlyPlaying()).thenReturn(Optional.of(songItem));
        when(songTableDynamoAdapter.songInTable(songItem)).thenReturn(false);

        subject.mainTask().run();
        subject.mainTask().run();

        verifyNoMoreInteractions(songTableDynamoAdapter, spotifyAdapter);
    }

    @Test
    public void mainTask_writeNewSongToTable() {
        when(spotifyAdapter.getCurrentlyPlaying()).thenReturn(Optional.of(otherSongItem));
        when(songTableDynamoAdapter.songInTable(otherSongItem)).thenReturn(false);

        for (int i = 0; i <= 5; i++) {
            subject.mainTask().run();
        }

        verify(songTableDynamoAdapter).writeSongToTable(eq(otherSongItem), any(Instant.class));
    }

    @Test
    public void refreshAuth() {
        subject.refreshToken().run();

        verify(authorizationManager).refreshAuthorization();
    }

    @Test
    public void refreshAuthDoesNotThrow() {
        doThrow(new RuntimeException("Refresh auth failed")).when(authorizationManager).refreshAuthorization();

        assertDoesNotThrow(() -> subject.refreshToken().run());
    }
}

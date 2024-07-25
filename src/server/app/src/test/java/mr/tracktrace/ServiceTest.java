package mr.tracktrace;

import mr.tracktrace.adapter.SongTableDynamoAdapter;
import mr.tracktrace.adapter.SpotifyAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class ServiceTest {
    private Service subject;

    @Mock
    SongTableDynamoAdapter songTableDynamoAdapter;

    @Mock
    SpotifyAdapter spotifyAdapter;

//    @BeforeEach
//    public void setup() {
//        subject = new Service(songTableDynamoAdapter, spotifyAdapter);
//    }
//
//    @Test
//    public void serverTest() {
//        subject.start();
//
//        verify(songTableDynamoAdapter).test();
//    }
}

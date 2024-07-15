package mr.tracktrace;

import mr.tracktrace.adapter.SongTableDynamoAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class ServerTest {
    private Server subject;

    @Mock
    SongTableDynamoAdapter songTableDynamoAdapter;

    @BeforeEach
    public void setup() {
        subject = new Server(songTableDynamoAdapter);
    }

    @Test
    public void serverTest() {
        subject.run();

        verify(songTableDynamoAdapter).test();
    }
}

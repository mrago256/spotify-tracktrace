package mr.tracktrace.adapter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.MockitoAnnotations.openMocks;

@ExtendWith(MockitoExtension.class)
public class SongTableDynamoAdapterTest {
    @BeforeEach
    public void setup() {
        openMocks(this);
    }
}

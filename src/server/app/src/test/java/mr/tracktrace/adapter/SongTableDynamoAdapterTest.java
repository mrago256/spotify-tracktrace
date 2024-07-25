package mr.tracktrace.adapter;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.MockitoAnnotations.openMocks;

@ExtendWith(MockitoExtension.class)
public class SongTableDynamoAdapterTest {
    private SongTableDynamoAdapter subject;

    @Mock
    DynamoDBMapper mapper;

    @BeforeEach
    public void setup() {
        openMocks(this);
        subject = new SongTableDynamoAdapter(mapper);
    }
}

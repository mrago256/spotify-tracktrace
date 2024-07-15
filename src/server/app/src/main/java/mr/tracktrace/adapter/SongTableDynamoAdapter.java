package mr.tracktrace.adapter;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import mr.tracktrace.adapter.internal.SongItemDDBItem;

public class SongTableDynamoAdapter {
    private final DynamoDBMapper dynamoDBMapper;

    public SongTableDynamoAdapter(DynamoDBMapper dynamoDBMapper) {
        this.dynamoDBMapper = dynamoDBMapper;
    }

    public String test() {
        SongItemDDBItem songItemDDBItem = dynamoDBMapper.load(SongItemDDBItem.class, "spotify:track:7qdgz117gc5StS0u2ViinE", 1609014807L);
        return songItemDDBItem.getSongName();
    }
}

package mr.tracktrace.adapter.internal;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import mr.tracktrace.model.SongItem;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@DynamoDBTable(tableName = SongItemDDBItem.TABLE_NAME)
public class SongItemDDBItem {
    public static final String TABLE_NAME = "testTrackTrace";

    public static final String TRACK_ID_KEY = "trackId";
    public static final String TIMESTAMP_KEY = "timestamp";
    public static final String TRACK_NAME_KEY = "trackName";

    @NonNull
    @DynamoDBHashKey(attributeName = TRACK_ID_KEY)
    private String trackId;

    @NonNull
    @DynamoDBAttribute(attributeName = TIMESTAMP_KEY)
    private Long timestamp;

    @NonNull
    @DynamoDBAttribute(attributeName = TRACK_NAME_KEY)
    private String trackName;

    public SongItem toSongItem() {
        return SongItem.builder()
                .trackID(trackId)
                .trackName(trackName)
                .build();
    }
}

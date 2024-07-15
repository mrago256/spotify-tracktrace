package mr.tracktrace.adapter.internal;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBRangeKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@DynamoDBTable(tableName = SongItemDDBItem.TABLE_NAME)
public class SongItemDDBItem {
    public static final String TABLE_NAME = "tracktrace";

    public static final String TRACK_ID_KEY = "trackID";
    public static final String TIMESTAMP_KEY = "timestamp";
    public static final String SONG_NAME_KEY = "songName";

    @NonNull
    @DynamoDBHashKey(attributeName = TRACK_ID_KEY)
    private String trackId;

    @NonNull
    @DynamoDBRangeKey(attributeName = TIMESTAMP_KEY)
    private Long timestamp;

    @NonNull
    @DynamoDBAttribute(attributeName = SONG_NAME_KEY)
    private String songName;
}
